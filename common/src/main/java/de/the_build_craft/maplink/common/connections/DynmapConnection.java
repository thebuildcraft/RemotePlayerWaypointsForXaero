/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by ewpratten)
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
import de.the_build_craft.maplink.common.configurations.DynmapConfiguration;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.mapUpdates.DynmapMarkerUpdate;
import de.the_build_craft.maplink.common.mapUpdates.DynmapPlayerUpdate;
import de.the_build_craft.maplink.common.waypoints.*;
import de.the_build_craft.maplink.common.wrappers.Utils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * Represents a connection to a dynmap server
 *
 * @author ewpratten
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 06.08.2026
 */
public class DynmapConnection extends MapConnection {
    private String markerStringTemplate = "";
    private String tilesStringTemplate;
    public String firstWorldName = "";
    public String[] worldNames = new String[0];
    public DynmapConfiguration.World[] worlds = new DynmapConfiguration.World[0];

    public DynmapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry);
        try {
            generateLink(serverEntry, true);
        }
        catch (Exception a){
            try {
                generateLink(serverEntry, false);
            }
            catch (Exception b){
                b.addSuppressed(a);
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Dynmap link is broken!");
                }
                throw b;
            }
        }
    }

    public DynmapConnection(ModConfig.ServerEntry serverEntry, String baseURL, String config, boolean partOfLifeAtlas) throws IOException {
        super(serverEntry);
        generateLinkWithConfig(baseURL, config);
        this.partOfLiveAtlas = partOfLifeAtlas;
    }

    private void generateLink(ModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);

        try{
            // test if the link is already the correct get-request
            queryURL = URI.create(serverEntry.link.replace(" ", "%20")).toURL();
            // Test the url
            this.getPlayerPositions();

            if (config.general.debugMode){
                Utils.sendToClientChat(("got link with method 1 | overwrite mode active!"));
            }
        }
        catch (Exception a){
            try{
                // get config.js
                String mapConfig = HTTP.makeTextHttpRequest(URI.create(baseURL + "/standalone/config.js").toURL());

                generateLinkWithConfig(baseURL, mapConfig);

                if (config.general.debugMode){
                    Utils.sendToClientChat("got link with method 2 | that is good!");
                }
            }
            catch (Exception b){
                try{
                    onlineMapConfigLink = baseURL + "/up/configuration";

                    setWorldNames();

                    // Build the url
                    queryURL = URI.create(baseURL + "/up/world/" + firstWorldName + "/").toURL();
                    markerStringTemplate = baseURL + "/tiles/_markers_/marker_{world}.json";

                    // Test the url
                    this.getPlayerPositions();

                    if (config.general.debugMode){
                        Utils.sendErrorToClientChat("got link with method 3 instead of 2 | please report this on github!");
                    }
                }
                catch (Exception ignored){
                    onlineMapConfigLink = baseURL + "/standalone/dynmap_config.json?";

                    setWorldNames();

                    // Build the url
                    queryURL = URI.create(baseURL + "/standalone/world/" + firstWorldName + ".json?").toURL();
                    markerStringTemplate = baseURL + "/tiles/_markers_/marker_{world}.json";

                    // Test the url
                    this.getPlayerPositions();

                    if (config.general.debugMode){
                        Utils.sendErrorToClientChat("got link with method 4 instead of 2 | please report this on github!");
                    }
                }
            }
        }

        AbstractModInitializer.LOGGER.info("new link: " + queryURL);
        if (config.general.debugMode){
            Utils.sendToClientChat("new link: " + queryURL);
        }
    }

    public void generateLinkWithConfig(String baseURL, String mapConfig) throws IOException {
        int i = mapConfig.indexOf("configuration: ");
        int j = mapConfig.indexOf(",", i);

        AbstractModInitializer.LOGGER.info("mapConfig: " + mapConfig);
        String substring = mapConfig.substring(i + 16, j - 1);
        if (substring.contains("?")){
            int k  = substring.indexOf("?");
            substring = substring.substring(0, k);
        }
        if (substring.contains("//")) {
            onlineMapConfigLink = substring.replace(" ", "%20");
        } else {
            if (!substring.startsWith("/")){
                substring = "/" + substring;
            }
            onlineMapConfigLink = (baseURL + substring).replace(" ", "%20");
        }
        AbstractModInitializer.LOGGER.info("configuration link: " + onlineMapConfigLink);

        setWorldNames();

        AbstractModInitializer.LOGGER.info("firstWorldName: " + firstWorldName);

        i = mapConfig.indexOf("update: ");
        j = mapConfig.indexOf(",", i);
        String updateStringTemplate = mapConfig.substring(i + 9, j - 1).replace("{timestamp}", "1");
        AbstractModInitializer.LOGGER.info("updateStringTemplate: " + updateStringTemplate);

        if (updateStringTemplate.contains("//")) {
            queryURL = URI.create(updateStringTemplate.replace("{world}", firstWorldName)).toURL();
        } else {
            if (!updateStringTemplate.startsWith("/")){
                updateStringTemplate = "/" + updateStringTemplate;
            }
            queryURL = URI.create(baseURL + updateStringTemplate.replace("{world}", firstWorldName)).toURL();
        }

        AbstractModInitializer.LOGGER.info("url: " + queryURL);

        i = mapConfig.indexOf("markers: ");
        int l = "markers: ".length() + 1;
        j = mapConfig.indexOf("'", i + l + 1);
        String markerSubstring = mapConfig.substring(i + l, j);
        if (markerSubstring.contains("//")) {
            markerStringTemplate = markerSubstring + "_markers_/marker_{world}.json";
        } else {
            if(!markerSubstring.startsWith("/")) {
                markerSubstring = "/" + markerSubstring;
            }
            markerStringTemplate = baseURL + markerSubstring + "_markers_/marker_{world}.json";
        }
        AbstractModInitializer.LOGGER.info("markerStringTemplate: " + markerStringTemplate);

        Matcher matcher = Pattern.compile("tiles: *['\"]([^'\"]*)['\"]").matcher(mapConfig);
        if (matcher.find()) {
            tilesStringTemplate = matcher.group(1);
            if (!tilesStringTemplate.contains("//")) {
                if (!tilesStringTemplate.startsWith("/")) tilesStringTemplate = "/" + tilesStringTemplate;
                tilesStringTemplate = baseURL + tilesStringTemplate;
            }
            tilesStringTemplate += "{world}/{prefix}/{regionX}_{regionZ}/{z}_{chunkX}_{chunkZ}";
        }

        // Test the url
        this.getPlayerPositions();
    }

    private void setWorldNames() throws IOException {
        DynmapConfiguration dynmapConfiguration = HTTP.makeJSONHTTPRequest(
                URI.create(onlineMapConfigLink).toURL(), DynmapConfiguration.class);
        worlds = dynmapConfiguration.worlds;
        worldNames = new String[worlds.length];
        for (int k = 0, worldsLength = worlds.length; k < worldsLength; k++) {
            worldNames[k] = worlds[k].name.replace(" ", "%20");
        }

        // Get the first world name. I know it seems random. Just trust me...
        firstWorldName = worldNames[0];

        UpdateTask.nextUpdateDelay = Math.max(UpdateTask.nextUpdateDelay, (int) Math.ceil(dynmapConfiguration.updaterate));
    }

    /**
     * Ask the server for a list of all player positions
     *
     * @return Player positions
     */
    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        // Make request for all players
        DynmapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, DynmapPlayerUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            DynmapPlayerUpdate.Player player = update.players[i];
            PlayerPosition playerPosition = new PlayerPosition(player.account, (int) Math.round(player.x), (int) Math.round(player.y), (int) Math.round(player.z), player.world);
            positions[i] = playerPosition;
            ClientMapHandler.registerPlayerPosition(playerPosition, markerStringTemplate.replace("_markers_/marker_{world}.json", "faces/32x32/" + player.account + ".png"));
        }

        return HandlePlayerPositions(positions);
    }

    @Override
    public HashSet<String> getMarkerLayers() {
        HashSet<String> layers = new HashSet<>();
        for (String world : worldNames) {
            DynmapMarkerUpdate u;
            try {
                u = HTTP.makeJSONHTTPRequest(URI.create(markerStringTemplate.replace("{world}", world).replace(" ", "%20")).toURL(), DynmapMarkerUpdate.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            layers.addAll(u.sets.keySet());
        }
        return layers;
    }

    String lastMarkerDimension = "";
    int lastMarkerHash;
    int lastAreaMarkerHash;
    List<Position> positions = new ArrayList<>();
    List<AreaMarker> areaMarkers = new ArrayList<>();

    @Override
    public void getWaypointPositions(boolean forceRefresh) throws IOException {
        if (serverEntry.needsMarkerLayerUpdate() && !partOfLiveAtlas) {
            serverEntry.setMarkerLayers(new ArrayList<>(getMarkerLayers()));
        }

        if (ClientMapHandler.getInstance() == null) return;

        String dimension;
        if (config.general.debugMode){
            dimension = firstWorldName;
        }
        else {
            dimension = currentDimension;
        }
        if (AbstractModInitializer.overwriteCurrentDimension && !Objects.equals(currentDimension, "")){
            dimension = currentDimension;
        }
        if (markerStringTemplate.isEmpty() || dimension.isEmpty()) {
            ClientMapHandler.getInstance().removeAllMarkerWaypoints();
            ClientMapHandler.getInstance().removeAllAreaMarkers(true);
            lastMarkerDimension = dimension;
            return;
        }
        int newMarkerHash = serverEntry.getMarkerVisibilityHash();
        int newAreaMarkerHash = serverEntry.getAreaMarkerVisibilityHash();
        if (lastMarkerDimension.equals(dimension)
                && newMarkerHash == lastMarkerHash
                && newAreaMarkerHash == lastAreaMarkerHash
                && !forceRefresh
        ) {
            ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
            return;
        }
        lastMarkerDimension = dimension;
        lastMarkerHash = newMarkerHash;
        lastAreaMarkerHash = newAreaMarkerHash;

        DynmapMarkerUpdate update = HTTP.makeJSONHTTPRequest(URI.create(markerStringTemplate.replace("{world}", dimension).replace(" ", "%20")).toURL(), DynmapMarkerUpdate.class);
        positions.clear();
        areaMarkers.clear();

        for (Map.Entry<String, DynmapMarkerUpdate.Set> set : update.sets.entrySet()){
            if (serverEntry.includeMarkerLayer(set.getKey())) {
                for (Map.Entry<String, DynmapMarkerUpdate.Set.Marker> markerEntry : set.getValue().markers.entrySet()) {
                    DynmapMarkerUpdate.Set.Marker m = markerEntry.getValue();
                    if (!serverEntry.includeMarker(m.label)) continue;
                    Position position = new Position(m.label, m.x, m.y, m.z, dimension + set.getKey() + markerEntry.getKey(), new MarkerLayer(set.getKey(), set.getValue().label));
                    positions.add(position);
                    ClientMapHandler.registerPosition(position,
                            (!config.general.showDefaultMarkerIcons && m.icon.equals("default")) ? null : markerStringTemplate.replace("marker_{world}.json", m.icon + ".png"));
                }
            }
            if (serverEntry.includeAreaMarkerLayer(set.getKey())) {
                for (Map.Entry<String, DynmapMarkerUpdate.Set.Area> areaEntry : set.getValue().areas.entrySet()) {
                    DynmapMarkerUpdate.Set.Area a = areaEntry.getValue();
                    if (!serverEntry.includeAreaMarker(a.label) || a.x.length < 2 || a.z.length < 2 || a.x.length != a.z.length) continue;
                    Double3[] points;
                    if (a.x.length > 2) {
                        points = new Double3[a.x.length];
                        for (int i = 0; i < a.x.length; i++) {
                            points[i] = new Double3(a.x[i], 0, a.z[i]);
                        }
                    } else {
                        points = new Double3[]{
                                new Double3(a.x[0], 0, a.z[0]),
                                new Double3(a.x[0], 0, a.z[1]),
                                new Double3(a.x[1], 0, a.z[1]),
                                new Double3(a.x[1], 0, a.z[0]),
                        };
                    }
                    areaMarkers.add(new AreaMarker(a.label, 0f, 0f, 0f, points,
                            new Color(a.color, a.opacity), new Color(a.fillcolor, a.fillopacity), dimension + set.getKey() + areaEntry.getKey(), new MarkerLayer(set.getKey(), set.getValue().label)));
                }
                for (Map.Entry<String, DynmapMarkerUpdate.Set.Circle> circleEntry : set.getValue().circles.entrySet()) {
                    DynmapMarkerUpdate.Set.Circle c = circleEntry.getValue();
                    if (!serverEntry.includeAreaMarker(c.label)) continue;
                    areaMarkers.add(new AreaMarker(c.label, c.x, c.y, c.z, convertEllipseToPolygon(c),
                            new Color(c.color, c.opacity), new Color(c.fillcolor, c.fillopacity), dimension + set.getKey() + circleEntry.getKey(), new MarkerLayer(set.getKey(), set.getValue().label)));
                }
            }
        }
        ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
        ClientMapHandler.getInstance().handleAreaMarkers(areaMarkers);
    }

    Double3[] convertEllipseToPolygon(DynmapMarkerUpdate.Set.Circle circle) {
        int N = 40;
        Double3[] points = new Double3[N];
        for (int i = 0; i < N; i++) {
            double a = (Math.PI * 2 / N) * i;
            points[i] = new Double3(circle.x + circle.xr * Math.sin(a), circle.y, circle.z + circle.zr * Math.cos(a));
        }
        return points;
    }

    private static void convertImage(NativeImage image, int chunkX, int chunkZ, int zOffset) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int currChunkX = 0; currChunkX < width / 16; currChunkX++) {
            int cx = (chunkX + currChunkX) * 16;
            for (int currChunkZ = 0; currChunkZ < height / 16; currChunkZ++) {
                int cz = (-chunkZ + currChunkZ + zOffset) * 16;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        //TODO
                    }
                }
            }
        }
    }

    @Override
    public List<String[]> getPossibleTileMaps() {
        return new ArrayList<>();
    }

    @Override
    public boolean downloadTiles(String map, AreaSelection areaSelection) {
        String template = null;
        for (DynmapConfiguration.World world : worlds) {
            if (world.name.equals(currentDimension)) {
                //TODO
                break;
            }
        }
        if (template == null) return false;
        //TODO
        return false;
    }
}