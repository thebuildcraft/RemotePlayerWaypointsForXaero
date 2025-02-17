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

import de.the_build_craft.remote_player_waypoints_for_xaero.common.*;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.BlueMapMarkerSet;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.BlueMapPlayerUpdate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import com.google.common.reflect.TypeToken;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;

import java.lang.reflect.Type;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 17.02.2025
 */
public class BlueMapConnection extends MapConnection {
    public int lastWorldIndex;
    public List<URL> playerUrls;
    public List<URL> markerUrls;
    public List<String> worlds = new ArrayList<>();

    public BlueMapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        playerUrls = new ArrayList<>();
        markerUrls = new ArrayList<>();
        try {
            generateLinks(serverEntry, false);
        }
        catch (Exception ignored){
            try {
                generateLinks(serverEntry, true);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Bluemap link is broken!");
                }
                throw e;
            }
        }
    }

    private void generateLinks(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);
        AbstractModInitializer.LOGGER.info("baseURL " + baseURL);
        // Get config and build the urls
        for (String w : ((BlueMapConfiguration) HTTP.makeJSONHTTPRequest(
                URI.create(baseURL + "/settings.json?").toURL(), BlueMapConfiguration.class)).maps){
            playerUrls.add(URI.create((baseURL + "/maps/" + w + "/live/players.json?").replace(" ", "%20")).toURL());
            markerUrls.add(URI.create((baseURL + "/maps/" + w + "/live/markers.json?").replace(" ", "%20")).toURL());
            worlds.add(w);
        }

        onlineMapConfigLink = baseURL + "/settings.json?";

        // Test the urls
        this.getPlayerPositions();

        for (URL url : playerUrls){
            AbstractModInitializer.LOGGER.info("new link: " + url);
            if (CommonModConfig.Instance.debugMode()){
                Utils.sendToClientChat("new link: " + url);
            }
        }

        for (URL url : markerUrls){
            AbstractModInitializer.LOGGER.info("new link: " + url);
            if (CommonModConfig.Instance.debugMode()){
                Utils.sendToClientChat("new link: " + url);
            }
        }
    }

    @Override
    public HashMap<String, WaypointPosition> getWaypointPositions() throws IOException {
        Type apiResponseType = new TypeToken<Map<String, BlueMapMarkerSet>>() {}.getType();
        URL reqUrl;
        if (AbstractModInitializer.overwriteCurrentDimension && !Objects.equals(currentDimension, "")){
            reqUrl = markerUrls.get(worlds.indexOf(currentDimension));
        }
        else{
            reqUrl = markerUrls.get(lastWorldIndex);
        }
        Map<String, BlueMapMarkerSet> markerSets = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

        HashMap<String, WaypointPosition> positions = new HashMap<>();

        CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
        if (serverEntry.markerVisibilityMode == CommonModConfig.ServerEntry.MarkerVisibilityMode.Auto) {
            List<String> layers = new ArrayList<>();
            for (Map.Entry<String, BlueMapMarkerSet> m : markerSets.entrySet()) {
                layers.add(m.getKey());
            }
            CommonModConfig.Instance.setMarkerLayers(serverEntry.ip, layers);
        }

        for (Map.Entry<String, BlueMapMarkerSet> m : markerSets.entrySet()){
            if (CommonModConfig.Instance.debugMode() && CommonModConfig.Instance.chatLogInDebugMode()){
                Utils.sendToClientChat("====================================");
                Utils.sendToClientChat("markerSet: " + m.getKey());
            }

            if (!serverEntry.includeMarkerLayer(m.getKey())) continue;

            for(BlueMapMarkerSet.Marker marker : m.getValue().markers.values()){
                if (Objects.equals(marker.type, "poi") || Objects.equals(marker.type, "html")){
                    BlueMapMarkerSet.Position pos = marker.position;
                    WaypointPosition newWaypointPosition = new WaypointPosition(marker.label, Math.round(pos.x), Math.round(pos.y), Math.round(pos.z));
                    positions.put(newWaypointPosition.name, newWaypointPosition);
                }
            }
        }

        return positions;
    }

    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        String clientName = mc.player.getName().getString();
        boolean correctWorld = false;
        BlueMapPlayerUpdate update = null;

        if (AbstractModInitializer.overwriteCurrentDimension && !Objects.equals(currentDimension, "")){
            update = HTTP.makeJSONHTTPRequest(playerUrls.get(worlds.indexOf(currentDimension)), BlueMapPlayerUpdate.class);
        }
        else{
            try{
                update = HTTP.makeJSONHTTPRequest(playerUrls.get(lastWorldIndex), BlueMapPlayerUpdate.class);
                for (BlueMapPlayerUpdate.Player p : update.players){
                    if (Objects.equals(p.name, clientName)){
                        correctWorld = !p.foreign;
                        break;
                    }
                }
            }
            catch (Exception ignored){
                if (CommonModConfig.Instance.debugMode()){
                    Utils.sendToClientChat("removed broken link: " + playerUrls.get(lastWorldIndex));
                }
                playerUrls.remove(lastWorldIndex);
                markerUrls.remove(lastWorldIndex);
                worlds.remove(lastWorldIndex);
            }
            if (!correctWorld){
                for (int i = 0; i < playerUrls.size(); i++) {
                    try{
                        update = HTTP.makeJSONHTTPRequest(playerUrls.get(i), BlueMapPlayerUpdate.class);
                        for (BlueMapPlayerUpdate.Player p : update.players){
                            if (Objects.equals(p.name, clientName)){
                                correctWorld = !p.foreign;
                                break;
                            }
                        }
                    }
                    catch (Exception ignored){
                        if (CommonModConfig.Instance.debugMode()){
                            Utils.sendToClientChat("removed broken link: " + playerUrls.get(i));
                        }
                        playerUrls.remove(i);
                        markerUrls.remove(i);
                        worlds.remove(i);
                    }

                    if (correctWorld) {
                        lastWorldIndex = i;
                        break;
                    }
                }
            }
        }

        if ((update == null) || playerUrls.isEmpty()){
            throw new IllegalStateException("Can't get player positions. All Bluemap links are broken!");
        }
        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        if (correctWorld){
            for (int i = 0; i < update.players.length; i++) {
                BlueMapPlayerUpdate.Player player = update.players[i];
                positions[i] = new PlayerPosition(player.name, Math.round(player.position.x), Math.round(player.position.y), Math.round(player.position.z), player.foreign ? "foreign" : "thisWorld");
            }
        }
        else {
            for (int i = 0; i < update.players.length; i++) {
                BlueMapPlayerUpdate.Player player = update.players[i];
                positions[i] = new PlayerPosition(player.name, Math.round(player.position.x), Math.round(player.position.y), Math.round(player.position.z), "unknown");
            }
        }
        return HandlePlayerPositions(positions);
    }
}
