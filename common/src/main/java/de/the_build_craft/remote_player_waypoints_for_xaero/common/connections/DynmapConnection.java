/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by ewpratten)
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
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.DynmapMarkerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.mapUpdates.DynmapPlayerUpdate;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a connection to a dynmap server
 *
 * @author ewpratten
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 28.06.2025
 */
public class DynmapConnection extends MapConnection {
    private String markerStringTemplate = "";
    public String firstWorldName = "";
    public String[] worldNames = new String[0];
    public DynmapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
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
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Dynmap link is broken!");
                }
                throw e;
            }
        }
    }

    public DynmapConnection(String baseURL, String config) throws IOException {
        generateLinkWithConfig(baseURL, config);
    }

    private void generateLink(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);

        try{
            // test if the link is already the correct get-request
            queryURL = URI.create(serverEntry.link.replace(" ", "%20")).toURL();
            // Test the url
            this.getPlayerPositions();

            if (CommonModConfig.Instance.debugMode()){
                Utils.sendToClientChat(("got link with method 1 | overwrite mode active!"));
            }
        }
        catch (Exception a){
            try{
                // get config.js
                String mapConfig = HTTP.makeTextHttpRequest(URI.create(baseURL + "/standalone/config.js").toURL());

                generateLinkWithConfig(baseURL, mapConfig);

                if (CommonModConfig.Instance.debugMode()){
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

                    if (CommonModConfig.Instance.debugMode()){
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

                    if (CommonModConfig.Instance.debugMode()){
                        Utils.sendErrorToClientChat("got link with method 4 instead of 2 | please report this on github!");
                    }
                }
            }
        }

        AbstractModInitializer.LOGGER.info("new link: " + queryURL);
        if (CommonModConfig.Instance.debugMode()){
            Utils.sendToClientChat("new link: " + queryURL);
        }
    }

    public void generateLinkWithConfig(String baseURL, String mapConfig) throws IOException {
        Matcher matcher = Pattern.compile(".*?//\\w*(\\.\\w+)+(:\\w+)?").matcher(baseURL);
        if (!matcher.find()) throw new RuntimeException("wrong url pattern");
        baseURL = matcher.group();

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

        // Test the url
        this.getPlayerPositions();
    }

    private void setWorldNames() throws IOException {
        DynmapConfiguration.World[] worlds = HTTP.makeJSONHTTPRequest(
                URI.create(onlineMapConfigLink).toURL(), DynmapConfiguration.class).worlds;
        worldNames = new String[worlds.length];
        for (int k = 0, worldsLength = worlds.length; k < worldsLength; k++) {
            worldNames[k] = worlds[k].name.replace(" ", "%20");
        }

        // Get the first world name. I know it seems random. Just trust me...
        firstWorldName = worldNames[0];
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
            positions[i] = new PlayerPosition(player.account, Math.round(player.x), Math.round(player.y), Math.round(player.z), player.world);
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
            for (DynmapMarkerUpdate.Set set : u.sets.values()) {
                layers.add(set.label);
            }
        }
        return layers;
    }

    String lastMarkerDimension = "";
    HashMap<String, WaypointPosition> lastResult = new HashMap<>();

    @Override
    public HashMap<String, WaypointPosition> getWaypointPositions() throws IOException {
        CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
        if (serverEntry.markerVisibilityMode == CommonModConfig.ServerEntry.MarkerVisibilityMode.Auto) {
            CommonModConfig.Instance.setMarkerLayers(serverEntry.ip, new ArrayList<>(getMarkerLayers()));
        }

        String dimension;
        if (CommonModConfig.Instance.debugMode()){
            dimension = firstWorldName;
        }
        else {
            dimension = currentDimension;
        }
        if (AbstractModInitializer.overwriteCurrentDimension && !Objects.equals(currentDimension, "")){
            dimension = currentDimension;
        }
        if (markerStringTemplate.isEmpty() || dimension.isEmpty()) {
            return new HashMap<>();
        }
        if (lastMarkerDimension.equals(dimension)) {
            return lastResult;
        }
        lastMarkerDimension = dimension;

        DynmapMarkerUpdate update = HTTP.makeJSONHTTPRequest(URI.create(markerStringTemplate.replace("{world}", dimension).replace(" ", "%20")).toURL(), DynmapMarkerUpdate.class);
        HashMap<String, WaypointPosition> positions = new HashMap<>();

        for (DynmapMarkerUpdate.Set set : update.sets.values()){
            if (!serverEntry.includeMarkerLayer(set.label)) continue;

            for (DynmapMarkerUpdate.Set.Marker m : set.markers.values()){
                WaypointPosition newWaypointPosition = new WaypointPosition(m.label, Math.round(m.x), Math.round(m.y), Math.round(m.z));
                positions.put(newWaypointPosition.name, newWaypointPosition);
            }
        }
        lastResult = positions;
        return positions;
    }
}