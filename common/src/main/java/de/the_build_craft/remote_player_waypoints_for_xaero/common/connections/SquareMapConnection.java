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
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 15.06.2024
 */
public class SquareMapConnection extends MapConnection {
    private String markerStringTemplate = "";
    public SquareMapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        try {
            generateLink(serverEntry, false);
        }
        catch (Exception ignored){
            try {
                generateLink(serverEntry, true);
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
        // Test the url
        PlayerPosition[] a = this.getPlayerPositions();

        AbstractModInitializer.LOGGER.info("new link: " + queryURL);
        if (CommonModConfig.Instance.debugMode()){
            Utils.sendToClientChat("new link: " + queryURL);
        }
    }

    @Override
    public PlayerPosition[] getPlayerPositions() throws IOException {
        // Make request for all players
        SquareMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, SquareMapPlayerUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            SquareMapPlayerUpdate.Player player = update.players[i];
            positions[i] = new PlayerPosition(player.name, player.x, CommonModConfig.Instance.defaultY(), player.z, player.world);
        }

        return HandlePlayerPositions(positions);
    }

    @Override
    public WaypointPosition[] getWaypointPositions() throws IOException {
        if (markerStringTemplate.isEmpty() || currentDimension.isEmpty()) {
            return new WaypointPosition[0];
        }

        Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();
        URL reqUrl = URI.create(markerStringTemplate.replace("{world}", currentDimension)).toURL();
        SquareMapMarkerUpdate[] markersLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

        ArrayList<WaypointPosition> positions = new ArrayList<>();

        for (SquareMapMarkerUpdate markerLayer : markersLayers){
            for (SquareMapMarkerUpdate.Marker marker : markerLayer.markers){
                if (Objects.equals(marker.type, "icon")) {
                    positions.add(new WaypointPosition(marker.tooltip, marker.point.x, CommonModConfig.Instance.defaultY(), marker.point.z));
                }
            }
        }

        return positions.toArray(new WaypointPosition[0]);
    }
}
