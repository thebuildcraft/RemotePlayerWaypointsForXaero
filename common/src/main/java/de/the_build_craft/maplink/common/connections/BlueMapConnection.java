/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2024 - 2026  Leander Knüttel and contributors
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.the_build_craft.maplink.common.connections;

import com.mojang.blaze3d.platform.NativeImage;
import de.the_build_craft.maplink.common.*;
import de.the_build_craft.maplink.common.clientMapHandlers.ClientMapHandler;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.configurations.BlueMapConfiguration;
import de.the_build_craft.maplink.common.configurations.BlueMapMapSettings;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.level.ProgressCounter;
import de.the_build_craft.maplink.common.mapUpdates.BlueMapMarkerSet;
import de.the_build_craft.maplink.common.mapUpdates.BlueMapPlayerUpdate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import com.google.common.reflect.TypeToken;
import de.the_build_craft.maplink.common.waypoints.*;
import de.the_build_craft.maplink.common.wrappers.Utils;

import java.lang.reflect.Type;
import java.util.stream.Collectors;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 06.08.2026
 */
public class BlueMapConnection extends MapConnection {
    List<Integer> lastWorldIndices = new ArrayList<>();
    List<URL> playerUrls = new ArrayList<>();
    List<URL> markerUrls = new ArrayList<>();
    List<String> worlds = new ArrayList<>();
    List<String> playerHeadIconUrlTemplates = new ArrayList<>();
    private String markerIconLinkTemplate = "";
    Map<String, BlueMapMapSettings> maps = new HashMap<>();
    private String tilesUrlTemplate;

    public BlueMapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry);
        autoUpdateDimensionMappings = false;
        try {
            generateLinks(serverEntry, true);
        }
        catch (Exception suppressed){
            try {
                generateLinks(serverEntry, false);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Bluemap link is broken!");
                }
                e.addSuppressed(suppressed);
                throw e;
            }
        }
    }

    private void generateLinks(ModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        playerUrls.clear();
        markerUrls.clear();
        worlds.clear();
        playerHeadIconUrlTemplates.clear();

        String baseURL = getBaseURL(serverEntry, useHttps);
        // Get config and build the urls
        for (String w : HTTP.makeJSONHTTPRequest(
                URI.create(baseURL + "/settings.json?").toURL(), BlueMapConfiguration.class).maps){
            playerUrls.add(URI.create((baseURL + "/maps/" + w + "/live/players.json?").replace(" ", "%20")).toURL());
            markerUrls.add(URI.create((baseURL + "/maps/" + w + "/live/markers.json?").replace(" ", "%20")).toURL());
            worlds.add(w);
            playerHeadIconUrlTemplates.add(baseURL + "/maps/" + w + "/assets/playerheads/{uuid}.png");
            maps.put(w, HTTP.makeJSONHTTPRequest(URI.create(baseURL + "/maps/" + w + "/settings.json").toURL(), BlueMapMapSettings.class));
        }

        onlineMapConfigLink = baseURL + "/settings.json?";
        markerIconLinkTemplate = baseURL + "/{icon}";
        tilesUrlTemplate = baseURL + "/maps/{world}/tiles/";

        // Test the urls
        this.getPlayerPositions();

        for (URL url : playerUrls){
            AbstractModInitializer.LOGGER.info("new player link: " + url);
            if (config.general.debugMode){
                Utils.sendToClientChat("new link: " + url);
            }
        }

        for (URL url : markerUrls){
            AbstractModInitializer.LOGGER.info("new marker link: " + url);
            if (config.general.debugMode){
                Utils.sendToClientChat("new link: " + url);
            }
        }

        //Bluemap updates players every second
        UpdateTask.nextUpdateDelay = 1000;
    }

    @Override
    public Set<String> getMarkerLayers() {
        Type apiResponseType = new TypeToken<Map<String, BlueMapMarkerSet>>() {}.getType();
        HashSet<String> layers = new HashSet<>();
        for (URL url : markerUrls) {
            Map<String, BlueMapMarkerSet> sets;
            try {
                sets = HTTP.makeJSONHTTPRequest(url, apiResponseType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            layers.addAll(sets.keySet());
        }
        return layers;
    }

    int lastWorldIndicesHash;
    int lastMarkerHash;
    int lastAreaMarkerHash;
    boolean lastExcludeOPAC;
    List<Position> positions = new ArrayList<>();
    List<AreaMarker> areaMarkers = new ArrayList<>();

    @Override
    public void getWaypointPositions(boolean forceRefresh) throws IOException {
        Type apiResponseType = new TypeToken<Map<String, BlueMapMarkerSet>>() {}.getType();

        if (serverEntry.needsMarkerLayerUpdate()) {
            serverEntry.setMarkerLayers(new ArrayList<>(getMarkerLayers()));
        }

        if (ClientMapHandler.getInstance() == null) return;

        int newWorldIndicesHash = lastWorldIndices.hashCode();
        int newMarkerHash = serverEntry.getMarkerVisibilityHash();
        int newAreaMarkerHash = serverEntry.getAreaMarkerVisibilityHash();
        boolean newExcludeOPAC = config.general.excludeOPAC;
        if (newWorldIndicesHash == lastWorldIndicesHash
                && newMarkerHash == lastMarkerHash
                && newAreaMarkerHash == lastAreaMarkerHash
                && newExcludeOPAC == lastExcludeOPAC
                && !forceRefresh
        ) {
            ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
            return;
        }
        lastWorldIndicesHash = newWorldIndicesHash;
        lastMarkerHash = newMarkerHash;
        lastAreaMarkerHash = newAreaMarkerHash;
        lastExcludeOPAC = newExcludeOPAC;
        positions.clear();
        areaMarkers.clear();

        for (int i : lastWorldIndices) {
            Map<String, BlueMapMarkerSet> markerSets = HTTP.makeJSONHTTPRequest(markerUrls.get(i), apiResponseType);
            for (Map.Entry<String, BlueMapMarkerSet> markerSetEntry : markerSets.entrySet()) {
                if (config.general.debugMode && config.general.chatLogInDebugMode) {
                    Utils.sendToClientChat("====================================");
                    Utils.sendToClientChat("markerSet: " + markerSetEntry.getKey());
                }

                if (config.general.excludeOPAC && Objects.equals(markerSetEntry.getKey(), "opac-bluemap-integration")) continue;

                for (Map.Entry<String, BlueMapMarkerSet.Marker> markerEntry : markerSetEntry.getValue().markers.entrySet()) {
                    BlueMapMarkerSet.Marker marker = markerEntry.getValue();
                    Double3 pos = marker.position;
                    if (Objects.equals(marker.type, "poi") || Objects.equals(marker.type, "html")) {
                        if (!serverEntry.includeMarkerLayer(markerSetEntry.getKey()) || !serverEntry.includeMarker(marker.label)) continue;
                        Position position = new Position(marker.label, pos.x, pos.y, pos.z, i + markerSetEntry.getKey() + markerEntry.getKey(), new MarkerLayer(markerSetEntry.getKey(), markerSetEntry.getValue().label));
                        positions.add(position);
                        ClientMapHandler.registerPosition(position, marker.icon.startsWith("http") ? marker.icon : (marker.icon.equals("assets/poi.svg") ? null : markerIconLinkTemplate.replace("{icon}", marker.icon)));
                    } else if (Objects.equals(marker.type, "shape") || Objects.equals(marker.type, "extrude")) {
                        if (!serverEntry.includeAreaMarkerLayer(markerSetEntry.getKey()) || !serverEntry.includeAreaMarker(marker.label)) continue;
                        List<Double3[]> polygons = new ArrayList<>();
                        polygons.add(marker.shape);
                        polygons.addAll(Arrays.asList(marker.holes));
                        areaMarkers.add(new AreaMarker(marker.label, pos.x, pos.y, pos.z, polygons.stream().toArray(Double3[][]::new), marker.lineColor, marker.fillColor, i + markerSetEntry.getKey() + markerEntry.getKey(), new MarkerLayer(markerSetEntry.getKey(), markerSetEntry.getValue().label)));
                    }
                }
            }
        }
        ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
        ClientMapHandler.getInstance().handleAreaMarkers(areaMarkers);
    }

    private boolean correctWorld = false;
    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        assert mc.player != null;
        String clientName = mc.player.getName().getString();
        correctWorld = false;
        BlueMapPlayerUpdate update = null;

        if (AbstractModInitializer.overwriteCurrentDimension && !Objects.equals(currentDimension, "")){
            lastWorldIndices.clear();
            lastWorldIndices.add(worlds.indexOf(currentDimension));
            update = HTTP.makeJSONHTTPRequest(playerUrls.get(lastWorldIndices.get(0)), BlueMapPlayerUpdate.class);
        }
        else{
            if (!lastWorldIndices.isEmpty()) update = getBlueMapPlayerUpdate(clientName, update, lastWorldIndices.get(0));
            if (!correctWorld){
                lastWorldIndices.clear();
                for (int i = 0; i < playerUrls.size(); i++) {
                    correctWorld = false;
                    update = getBlueMapPlayerUpdate(clientName, update, i);
                    if (correctWorld) lastWorldIndices.add(i);
                }

                #if MC_VER >= MC_1_21_11
                String clientDimension = mc.level.dimension().identifier().toString();
                #else
                String clientDimension = mc.level.dimension().location().toString();
                #endif
                if (lastWorldIndices.isEmpty()) {
                    String[] mappedDimensions = serverEntry.dimensionMapping.get(clientDimension);
                    if (config.general.invisibilityRecovery && mappedDimensions != null && mappedDimensions.length > 0) {
                        lastWorldIndices = new ArrayList<>(Arrays.stream(mappedDimensions).map(world -> worlds.indexOf(world)).collect(Collectors.toList()));
                    }
                } else {
                    serverEntry.addDimensionMapping(clientDimension,
                            lastWorldIndices.stream().map(i -> worlds.get(i)).toArray(String[]::new));
                }
            }
        }
        correctWorld = !lastWorldIndices.isEmpty();

        if ((update == null) || playerUrls.isEmpty()){
            throw new IllegalStateException("Can't get player positions. All Bluemap links are broken!");
        }
        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++) {
            BlueMapPlayerUpdate.Player player = update.players[i];
            PlayerPosition playerPosition = new PlayerPosition(player.name, player.position.x, player.position.y, player.position.z, correctWorld ? (player.foreign ? "foreign" : "thisWorld") : "unknown");
            positions[i] = playerPosition;
            if (!correctWorld) continue;
            ClientMapHandler.registerPlayerPosition(playerPosition, playerHeadIconUrlTemplates.get(lastWorldIndices.get(0)).replace("{uuid}", player.uuid));
        }
        return HandlePlayerPositions(positions, "thisWorld");
    }

    private BlueMapPlayerUpdate getBlueMapPlayerUpdate(String clientName, BlueMapPlayerUpdate update, int worldIndex) {
        try{
            BlueMapPlayerUpdate newUpdate = HTTP.makeJSONHTTPRequest(playerUrls.get(worldIndex), BlueMapPlayerUpdate.class);
            for (BlueMapPlayerUpdate.Player p : newUpdate.players){
                if (Objects.equals(p.name, clientName)){
                    correctWorld = !p.foreign;
                    break;
                }
            }
            if (update == null || correctWorld) update = newUpdate;
        }
        catch (Exception ignored){
            if (config.general.debugMode){
                Utils.sendToClientChat("removed broken link: " + playerUrls.get(worldIndex));
            }
            playerUrls.remove(worldIndex);
            markerUrls.remove(worldIndex);
            maps.remove(worlds.get(worldIndex));
            worlds.remove(worldIndex);
        }
        return update;
    }

    @Override
    public List<String[]> getPossibleTileMaps() {
        if (lastWorldIndices.isEmpty()) return worlds.stream().map(w -> new String[]{w, maps.get(w).name}).collect(Collectors.toList());
        return lastWorldIndices.stream().map(i -> {
            String world = worlds.get(i);
            return new String[]{world, maps.get(world).name};
        }).collect(Collectors.toList());
    }

    @Override
    public boolean downloadTiles(String map, AreaSelection areaSelection) {
        BlueMapMapSettings settings = maps.get(map);
        if (settings == null) return false;
        int tileSizeX = settings.lowres.tileSize[0];
        int tileSizeZ = settings.lowres.tileSize[1];
        String basePath = tilesUrlTemplate.replace("{world}", map);

        XaeroClientMapHandler.xaeroWorldMapSupport.init(areaSelection);

        for (int tileX = Math.floorDiv(areaSelection.minX, tileSizeX); tileX <= Math.floorDiv(areaSelection.maxX, tileSizeX); tileX++) {
            for (int tileZ = Math.floorDiv(areaSelection.minZ, tileSizeZ); tileZ <= Math.floorDiv(areaSelection.maxZ, tileSizeZ); tileZ++) {
                int finalTileX = tileX;
                int finalTileZ = tileZ;

                if (!ProgressCounter.converting.get()) return false;

                ProgressCounter.addTask(() -> {
                    try (NativeImage tile = HTTP.makeImageHttpRequest(URI.create(pathFromCoords(basePath, finalTileX, finalTileZ)).toURL())) {
                        for (int x = 0; x < tileSizeX; x++) {
                            for (int z = 0; z < tileSizeZ; z++) {
                                #if MC_VER > MC_1_21_1
                                int pixelRgb = tile.getPixel(x, z);
                                int metaRgb = tile.getPixel(x, tileSizeZ + 1 + z);
                                #else
                                int pixelRgb = Color.ABGRtoARGB(tile.getPixelRGBA(x, z));
                                int metaRgb = Color.ABGRtoARGB(tile.getPixelRGBA(x, tileSizeZ + 1 + z));
                                #endif
                                XaeroClientMapHandler.xaeroWorldMapSupport.writeBlock(
                                        finalTileX * tileSizeX + x,
                                        finalTileZ * tileSizeZ + z,
                                        metaToLight(metaRgb),
                                        metaToHeight(metaRgb),
                                        pixelRgb);
                            }
                        }
                    }
                });
            }
        }

        return ProgressCounter.finishAllTasks();
    }

    //adapted from https://github.com/BlueMap-Minecraft/BlueMap licensed under the MIT License
    private int metaToHeight(int metaRgb) {
        int heightUnsigned = Color.argbToG(metaRgb) * 256 + Color.argbToB(metaRgb);
        if (heightUnsigned >= 32768) {
            return -(65535 - heightUnsigned);
        } else {
            return heightUnsigned;
        }
    }

    private int metaToLight(int metaRgb) {
        return Color.argbToR(metaRgb);
    }

    //adapted from https://github.com/BlueMap-Minecraft/BlueMap licensed under the MIT License
    private String pathFromCoords(String basePath, int tileX, int tileZ) {
        String path = basePath + "1/x";
        path += splitNumberToPath(tileX);

        path += "z";
        path += splitNumberToPath(tileZ);

        path = path.substring(0, path.length() - 1);

        return path + ".png";
    }

    //adapted from https://github.com/BlueMap-Minecraft/BlueMap licensed under the MIT License
    private String splitNumberToPath(int num) {
        StringBuilder path = new StringBuilder();

        if (num < 0) {
            num = -num;
            path.append("-");
        }

        String s = num + "";

        for (int i = 0; i < s.length(); i++) {
            path.append(s.charAt(i)).append("/");
        }

        return path.toString();
    }
}
