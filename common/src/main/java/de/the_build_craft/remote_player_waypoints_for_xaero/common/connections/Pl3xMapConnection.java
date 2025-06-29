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
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.Pl3xMapAltMarkerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.Pl3xMapMarkerLayerConfig;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.Pl3xMapMarkerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.Pl3xMapPlayerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 28.06.2025
 */
public class Pl3xMapConnection extends MapConnection{
    private String markerLayerStringTemplate = "";
    private String markerStringTemplate = "";
    int version;
    public Pl3xMapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
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
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Pl3xMap link is broken!");
                }
                throw e;
            }
        }
    }

    public Pl3xMapConnection(String baseURL, String link) throws IOException {
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

    private void generateLink(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        setup(getBaseURL(serverEntry, useHttps));
    }

    private void setup(String baseURL) throws IOException {
        onlineMapConfigLink = baseURL + "/tiles/settings.json";
        markerLayerStringTemplate = baseURL + "/tiles/{world}/markers.json";

        // test which version is needed
        if (HTTP.makeTextHttpRequest(URI.create(onlineMapConfigLink).toURL()).contains("\"players\":[")) {
            version = 0;
            queryURL = URI.create(onlineMapConfigLink).toURL();
            markerStringTemplate = baseURL + "/tiles/{world}/markers/{layerName}.json";
        } else {
            version = 1;
            queryURL = URI.create(baseURL + "/tiles/players.json").toURL();
        }

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
        Pl3xMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, Pl3xMapPlayerUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            Pl3xMapPlayerUpdate.Player player = update.players[i];
            if (version == 0) positions[i] = new PlayerPosition(player.name, player.position.x, CommonModConfig.Instance.defaultY(), player.position.z, player.world);
            else if (version == 1) positions[i] = new PlayerPosition(player.name, player.x, CommonModConfig.Instance.defaultY(), player.z, player.world);
        }

        return HandlePlayerPositions(positions);
    }

    private String lastMarkerDimension = "";
    private HashMap<String, WaypointPosition> lastResult = new HashMap<>();

    @Override
    public HashMap<String, WaypointPosition> getWaypointPositions() throws IOException {
        if (markerLayerStringTemplate.isEmpty() || currentDimension.isEmpty()) {
            return new HashMap<>();
        }
        CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
        if (serverEntry.markerVisibilityMode == CommonModConfig.ServerEntry.MarkerVisibilityMode.Auto) {
            CommonModConfig.Instance.setMarkerLayers(serverEntry.ip, new ArrayList<>(getMarkerLayers()));
        }
        if (markerStringTemplate.isEmpty() && version == 0) {
            return new HashMap<>();
        }
        if (lastMarkerDimension.equals(currentDimension)) {
            return lastResult;
        }
        lastMarkerDimension = currentDimension;

        HashMap<String, WaypointPosition> positions = new HashMap<>();

        if (version == 0) {
            for (String layer : getMarkerLayers(false)){
                Type apiResponseType = new TypeToken<Pl3xMapMarkerUpdate[]>() {}.getType();
                URL reqUrl = URI.create(markerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))
                        .replace("{layerName}", layer)).toURL();
                Pl3xMapMarkerUpdate[] markers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                for (Pl3xMapMarkerUpdate marker : markers){
                    if (!Objects.equals(marker.type, "icon")) continue;
                    WaypointPosition newWaypointPosition = new WaypointPosition(marker.options.tooltip.content, marker.data.point.x, CommonModConfig.Instance.defaultY(), marker.data.point.z);
                    positions.put(newWaypointPosition.name, newWaypointPosition);
                }
            }
        } else if (version == 1) {
            Type apiResponseType = new TypeToken<Pl3xMapAltMarkerUpdate[]>() {}.getType();
            URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
            Pl3xMapAltMarkerUpdate[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

            for (Pl3xMapAltMarkerUpdate layer : markerLayers){
                if (!Objects.equals(layer.id, "pl3xmap_players") && serverEntry.includeMarkerLayer(layer.id)) {
                    for (Pl3xMapAltMarkerUpdate.Marker marker : layer.markers) {
                        if (!Objects.equals(marker.type, "icon")) continue;
                        WaypointPosition newWaypointPosition = new WaypointPosition(marker.tooltip, marker.point.x, CommonModConfig.Instance.defaultY(), marker.point.z);
                        positions.put(newWaypointPosition.name, newWaypointPosition);
                    }
                }
            }
        }
        lastResult = positions;
        return positions;
    }

    @Override
    public HashSet<String> getMarkerLayers() {
        return getMarkerLayers(true);
    }

    private HashSet<String> getMarkerLayers(boolean all) {
        try {
            if (version == 0) {
                Type apiResponseType = new TypeToken<Pl3xMapMarkerLayerConfig[]>() {}.getType();

                if (all) {
                    Pl3xMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, Pl3xMapPlayerUpdate.class);
                    HashSet<String> layerSet = new HashSet<>();
                    for (Pl3xMapPlayerUpdate.WorldSetting ws : update.worldSettings) {
                        Pl3xMapMarkerLayerConfig[] mls = HTTP.makeJSONHTTPRequest(URI.create(markerLayerStringTemplate
                                .replace("{world}", ws.name.replaceAll(":", "-"))).toURL(), apiResponseType);
                        for (Pl3xMapMarkerLayerConfig ml : mls) {
                            if (!Objects.equals(ml.key, "pl3xmap_players")) {
                                layerSet.add(ml.key);
                            }
                        }
                    }
                    return layerSet;
                }

                URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
                Pl3xMapMarkerLayerConfig[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                HashSet<String> layers = new HashSet<>();

                CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
                for (Pl3xMapMarkerLayerConfig layer : markerLayers){
                    if (!Objects.equals(layer.key, "pl3xmap_players") && serverEntry.includeMarkerLayer(layer.key)) {
                        layers.add(layer.key);
                    }
                }

                return layers;
            }
            if (version == 1) {
                Type apiResponseType = new TypeToken<Pl3xMapAltMarkerUpdate[]>() {}.getType();

                if (all) {
                    Pl3xMapConfiguration configuration = HTTP.makeJSONHTTPRequest(URI.create(onlineMapConfigLink).toURL(), Pl3xMapConfiguration.class);
                    HashSet<String> layerSet = new HashSet<>();
                    for (Pl3xMapConfiguration.World ws : configuration.worlds) {
                        Pl3xMapAltMarkerUpdate[] mls = HTTP.makeJSONHTTPRequest(URI.create(markerLayerStringTemplate.replace("{world}", ws.name.replaceAll(":", "-"))).toURL(), apiResponseType);
                        for (Pl3xMapAltMarkerUpdate ml : mls) {
                            if (!Objects.equals(ml.id, "pl3xmap_players")) {
                                layerSet.add(ml.id);
                            }
                        }
                    }
                    return layerSet;
                }

                URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
                Pl3xMapAltMarkerUpdate[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                HashSet<String> layers = new HashSet<>();

                CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
                for (Pl3xMapAltMarkerUpdate layer : markerLayers){
                    if (!Objects.equals(layer.id, "pl3xmap_players") && serverEntry.includeMarkerLayer(layer.id)) {
                        layers.add(layer.id);
                    }
                }

                return layers;
            }
            throw new IllegalStateException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
