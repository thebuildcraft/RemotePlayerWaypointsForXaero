/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2025  Leander Knüttel
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
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Leander Knüttel
 * @version 26.06.2025
 */
public class LiveAtlasConnection extends MapConnection {
    public static final Pattern dynmapRegexPattern = Pattern.compile("\\n +dynmap: \\{\\n(.*\\n)*?.*}\\n");
    public static final Pattern Pl3xMapRegexPattern = Pattern.compile("\n +\t+ +pl3xmap: \"(.+?)\"\n");
    List<MapConnection> mapConnections = new ArrayList<>();
    int mapIndex;

    @Override
    public void setCurrentDimension(String currentDimension) {
        for (MapConnection connection : mapConnections) {
            connection.setCurrentDimension(currentDimension);
        }
    }

    public LiveAtlasConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        try {
            setupConnections(serverEntry, true);
        } catch (Exception ignored) {
            try {
                setupConnections(serverEntry, false);
            } catch (Exception e) {
                if (!updateTask.linkBrokenErrorWasShown) {
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your LiveAtlas link is broken!");
                }
                throw e;
            }
        }
    }

    private void setupConnections(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);
        String liveAtlasHTML = HTTP.makeTextHttpRequest(URI.create(baseURL).toURL(), true);
        Matcher matcher = dynmapRegexPattern.matcher(liveAtlasHTML);
        while (matcher.find()) {
            String g = matcher.group();
            try {
                mapConnections.add(new DynmapConnection(baseURL, g));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Dynmap connection for LiveAtlas");
            }
        }
        matcher = Pl3xMapRegexPattern.matcher(liveAtlasHTML);
        while (matcher.find()) {
            String g = matcher.group(1);
            try {
                mapConnections.add(new Pl3xMapConnection(baseURL, g));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Pl3xMap connection for LiveAtlas");
                e.printStackTrace();
            }
        }
    }

    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        if (mapConnections.isEmpty()) return new HashMap<>();
        if (CommonModConfig.Instance.debugMode()) {
            HashMap<String, PlayerPosition> debug = new HashMap<>();
            for (MapConnection mapConnection : mapConnections) {
                debug.putAll(mapConnection.getPlayerPositions());
            }
            return debug;
        }

        HashMap<String, PlayerPosition> map = mapConnections.get(mapIndex).getPlayerPositions();

        if (mapConnections.get(mapIndex).foundPlayer) {
            return map;
        } else {
            int i = 0;
            for (MapConnection mapConnection : mapConnections) {
                if (i == mapIndex) {
                    i++;
                    continue;
                }
                HashMap<String, PlayerPosition> map2 = mapConnection.getPlayerPositions();
                if (mapConnection.foundPlayer) {
                    mapIndex = i;
                    return map2;
                }
                i++;
            }
        }
        return map;
    }

    @Override
    public HashMap<String, WaypointPosition> getWaypointPositions() throws IOException {
        if (mapConnections.isEmpty()) return new HashMap<>();
        CommonModConfig.ServerEntry serverEntry = CommonModConfig.Instance.getCurrentServerEntry();
        if (serverEntry.markerVisibilityMode == CommonModConfig.ServerEntry.MarkerVisibilityMode.Auto) {
            CommonModConfig.Instance.setMarkerLayers(serverEntry.ip, new ArrayList<>(getMarkerLayers()));
        }

        return mapConnections.get(mapIndex).getWaypointPositions();
    }

    @Override
    public HashSet<String> getMarkerLayers() {
        HashSet<String> layers = new HashSet<>();
        for (MapConnection connection : mapConnections) {
            layers.addAll(connection.getMarkerLayers());
        }
        return layers;
    }
}
