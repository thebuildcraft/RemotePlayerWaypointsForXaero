/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2024  Leander Knüttel
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

package de.the_build_craft.remote_player_waypoints_for_xaero.common.connections;

import com.google.common.reflect.TypeToken;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.*;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.SquareMapMarkerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.SquareMapPlayerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 21.04.2025
 */
public class SquareMapConnection extends MapConnection {
    private String markerStringTemplate = "";
    public SquareMapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
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

    private void generateLink(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);

        // Build the url
        queryURL = URI.create(baseURL + "/tiles/players.json").toURL();
        markerStringTemplate = baseURL + "/tiles/{world}/markers.json";

        onlineMapConfigLink = baseURL + "/tiles/settings.json";

        // Test the url
        this.getPlayerPositions();

        AbstractModInitializer.LOGGER.info("new link: " + queryURL);
        if (CommonModConfig.Instance.debugMode()){
            Utils.sendToClientChat("new link: " + queryURL);
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
            positions[i] = new PlayerPosition(player.name, player.x,
                    player.y == Integer.MIN_VALUE ? CommonModConfig.Instance.defaultY() : player.y, player.z, player.world);
        }

        return HandlePlayerPositions(positions);
    }

    @Override
    public HashSet<String> getMarkerLayers() {
        try {
            Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();

            HashSet<String> layers = new HashSet<>();
            SquareMapConfiguration squareMapConfiguration = HTTP.makeJSONHTTPRequest(URI.create(onlineMapConfigLink).toURL(), SquareMapConfiguration.class);
            for (SquareMapConfiguration.World world : squareMapConfiguration.worlds) {
                SquareMapMarkerUpdate[] ml = HTTP.makeJSONHTTPRequest(URI.create(markerStringTemplate.replace("{world}", world.name)).toURL(), apiResponseType);
                for (SquareMapMarkerUpdate markerLayer : ml) {
                    layers.add(markerLayer.name);
                }
            }
            return layers;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String lastMarkerDimension = "";
    HashMap<String, WaypointPosition> lastResult = new HashMap<>();

    @Override
    public HashMap<String, WaypointPosition> getWaypointPositions() throws IOException {
        if (markerStringTemplate.isEmpty() || currentDimension.isEmpty()) {
            return new HashMap<>();
        }
        if (lastMarkerDimension.equals(currentDimension)) {
            return lastResult;
        }
        lastMarkerDimension = currentDimension;

        Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();

        CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
        if (serverEntry.markerVisibilityMode == CommonModConfig.ServerEntry.MarkerVisibilityMode.Auto) {
            CommonModConfig.Instance.setMarkerLayers(serverEntry.ip, new ArrayList<>(getMarkerLayers()));
        }

        URL reqUrl = URI.create(markerStringTemplate.replace("{world}", currentDimension)).toURL();
        SquareMapMarkerUpdate[] markersLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

        HashMap<String, WaypointPosition> positions = new HashMap<>();

        for (SquareMapMarkerUpdate markerLayer : markersLayers){
            if (!serverEntry.includeMarkerLayer(markerLayer.name)) continue;

            for (SquareMapMarkerUpdate.Marker marker : markerLayer.markers){
                if (Objects.equals(marker.type, "icon")) {
                    WaypointPosition newWaypointPosition = new WaypointPosition(marker.tooltip, marker.point.x, CommonModConfig.Instance.defaultY(), marker.point.z);
                    positions.put(newWaypointPosition.name, newWaypointPosition);
                }
            }
        }
        lastResult = positions;
        return positions;
    }
}
