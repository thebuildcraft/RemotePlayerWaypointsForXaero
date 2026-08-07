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

import com.google.common.reflect.TypeToken;
import de.the_build_craft.maplink.common.*;
import de.the_build_craft.maplink.common.clientMapHandlers.ClientMapHandler;
import de.the_build_craft.maplink.common.configurations.SquareMapConfiguration;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.mapUpdates.SquareMapMarkerUpdate;
import de.the_build_craft.maplink.common.mapUpdates.SquareMapPlayerUpdate;
import de.the_build_craft.maplink.common.mapUpdates.SquareMapWorldSettings;
import de.the_build_craft.maplink.common.waypoints.*;
import de.the_build_craft.maplink.common.wrappers.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 06.08.2026
 */
public class SquareMapConnection extends MapConnection {
    private String markerStringTemplate = "";
    private String markerIconLinkTemplate = "";
    private String worldSettingsLinkTemplate = "";
    public SquareMapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry);
        try {
            generateLink(serverEntry, true);
        }
        catch (Exception ignored){
            try {
                generateLink(serverEntry, false);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Squaremap link is broken!");
                }
                throw e;
            }
        }
    }

    public SquareMapConnection(ModConfig.ServerEntry serverEntry, String baseURL, String link, boolean partOfLifeAtlas) throws IOException {
        super(serverEntry);
        this.partOfLiveAtlas = partOfLifeAtlas;
        Matcher matcher = Pattern.compile(".*?//\\w*(\\.\\w+)+(:\\w+)?").matcher(baseURL);
        if (!matcher.find()) throw new RuntimeException("wrong url pattern");
        baseURL = matcher.group();
        if (link.contains("//")) {
            baseURL = link;
        } else {
            if (!link.startsWith("/")) link = "/" + link;
            baseURL += link;
        }
        if (baseURL.endsWith("/")) baseURL = baseURL.substring(0, baseURL.length() - 1);
        setup(baseURL);
    }

    private void generateLink(ModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        setup(getBaseURL(serverEntry, useHttps));
    }

    private void setup(String baseURL) throws IOException {
        // Build the url
        queryURL = URI.create(baseURL + "/tiles/players.json").toURL();
        markerStringTemplate = baseURL + "/tiles/{world}/markers.json";
        markerIconLinkTemplate = baseURL + "/images/icon/registered/{icon}.png";

        onlineMapConfigLink = baseURL + "/tiles/settings.json";
        worldSettingsLinkTemplate = baseURL + "/tiles/{world}/settings.json";

        // Test the url
        this.getPlayerPositions();

        AbstractModInitializer.LOGGER.info("new link: " + queryURL);
        if (config.general.debugMode){
            Utils.sendToClientChat("new link: " + queryURL);
        }

        setUpdateDelay();
    }

    private void setUpdateDelay() {
        try {
            SquareMapConfiguration squareMapConfiguration = HTTP.makeJSONHTTPRequest(URI.create(onlineMapConfigLink).toURL(), SquareMapConfiguration.class);
            float updateDelay = 1;
            for (SquareMapConfiguration.World world : squareMapConfiguration.worlds) {
                SquareMapWorldSettings squareMapWorldSettings = HTTP.makeJSONHTTPRequest(URI.create(worldSettingsLinkTemplate.replace("{world}", world.name)).toURL(), SquareMapWorldSettings.class);
                updateDelay = Math.max(updateDelay, squareMapWorldSettings.player_tracker.update_interval);
            }
            UpdateTask.nextUpdateDelay = Math.max(UpdateTask.nextUpdateDelay, (int) Math.ceil(updateDelay * 1000));
        } catch (Exception e) {
            AbstractModInitializer.LOGGER.error("Error getting update Delay! Using the Default of 1000 ms for SquareMap.", e);
            UpdateTask.nextUpdateDelay = Math.max(UpdateTask.nextUpdateDelay, 1000);
        }
    }

    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        // Make request for all players
        SquareMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, SquareMapPlayerUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            SquareMapPlayerUpdate.Player player = update.players[i];
            PlayerPosition playerPosition = new PlayerPosition(player.name, player.x,
                    player.y == Integer.MIN_VALUE ? config.general.defaultY : player.y, player.z, player.world);
            ClientMapHandler.registerPlayerPosition(playerPosition, "https://mc-heads.net/avatar/" + player.uuid + "/32");
            positions[i] = playerPosition;
        }

        return HandlePlayerPositions(positions);
    }

    @Override
    public Set<String> getMarkerLayers() {
        try {
            Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();

            HashSet<String> layers = new HashSet<>();
            SquareMapConfiguration squareMapConfiguration = HTTP.makeJSONHTTPRequest(URI.create(onlineMapConfigLink).toURL(), SquareMapConfiguration.class);
            for (SquareMapConfiguration.World world : squareMapConfiguration.worlds) {
                SquareMapMarkerUpdate[] ml = HTTP.makeJSONHTTPRequest(URI.create(markerStringTemplate.replace("{world}", world.name)).toURL(), apiResponseType);
                for (SquareMapMarkerUpdate markerLayer : ml) {
                    layers.add(markerLayer.id);
                }
            }
            return layers;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String lastMarkerDimension = "";
    int lastMarkerHash;
    int lastAreaMarkerHash;
    List<Position> positions = new ArrayList<>();
    List<AreaMarker> areaMarkers = new ArrayList<>();

    @Override
    public void getWaypointPositions(boolean forceRefresh) throws IOException {
        if (markerStringTemplate.isEmpty() || currentDimension.isEmpty()) {
            lastMarkerDimension = currentDimension;
            if (ClientMapHandler.getInstance() != null) {
                ClientMapHandler.getInstance().removeAllMarkerWaypoints();
                ClientMapHandler.getInstance().removeAllAreaMarkers(true);
            }
            return;
        }

        if (serverEntry.needsMarkerLayerUpdate() && !partOfLiveAtlas) {
            serverEntry.setMarkerLayers(new ArrayList<>(getMarkerLayers()));
        }

        if (ClientMapHandler.getInstance() == null) return;

        int newMarkerHash = serverEntry.getMarkerVisibilityHash();
        int newAreaMarkerHash = serverEntry.getAreaMarkerVisibilityHash();
        if (lastMarkerDimension.equals(currentDimension)
                && newMarkerHash == lastMarkerHash
                && newAreaMarkerHash == lastAreaMarkerHash
                && !forceRefresh
        ) {
            ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
            return;
        }
        lastMarkerDimension = currentDimension;
        lastMarkerHash = newMarkerHash;
        lastAreaMarkerHash = newAreaMarkerHash;

        Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();

        URL reqUrl = URI.create(markerStringTemplate.replace("{world}", currentDimension)).toURL();
        SquareMapMarkerUpdate[] markersLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

        positions.clear();
        areaMarkers.clear();

        for (SquareMapMarkerUpdate markerLayer : markersLayers){
            for (SquareMapMarkerUpdate.Marker marker : markerLayer.markers){
                if (Objects.equals(marker.type, "icon") && serverEntry.includeMarkerLayer(markerLayer.id) && serverEntry.includeMarker(marker.tooltip)) {
                    Position position = new Position(marker.tooltip, marker.point.x, config.general.defaultY, marker.point.z, currentDimension + markerLayer.id + marker.tooltip + marker.point.x + marker.point.z, new MarkerLayer(markerLayer.id, markerLayer.name));
                    positions.add(position);
                    ClientMapHandler.registerPosition(position, markerIconLinkTemplate.replace("{icon}", marker.icon));
                }
                else if (Objects.equals(marker.type, "polygon") && serverEntry.includeAreaMarkerLayer(markerLayer.id) && serverEntry.includeAreaMarker(marker.tooltip)) {
                    areaMarkers.add(new AreaMarker(marker.tooltip, 0, 0, 0, Arrays.stream(marker.points).flatMap(Arrays::stream).toArray(Int3[][]::new),
                            new Color(marker.color, 1f), new Color(marker.fillColor, marker.opacity), currentDimension + markerLayer.id + marker.tooltip + Arrays.deepHashCode(marker.points), new MarkerLayer(markerLayer.id, markerLayer.name)));
                }
            }
        }
        ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
        ClientMapHandler.getInstance().handleAreaMarkers(areaMarkers);
    }

    @Override
    public List<String[]> getPossibleTileMaps() {
        return new ArrayList<>();
    }

    @Override
    public boolean downloadTiles(String map, AreaSelection areaSelection) {
        return false;
    }
}
