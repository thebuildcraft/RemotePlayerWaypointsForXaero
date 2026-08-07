/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2025 - 2026  Leander Knüttel and contributors
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

import de.the_build_craft.maplink.common.*;
import de.the_build_craft.maplink.common.clientMapHandlers.ClientMapHandler;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.waypoints.PlayerPosition;
import de.the_build_craft.maplink.common.wrappers.Utils;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class LiveAtlasConnection extends MapConnection {
    public static final Pattern dynmapRegexPattern = Pattern.compile("dynmap: *\\{\\R*((?!\\s+//\\s*).*\\R*)*?[^}\"']*}");
    public static final Pattern Pl3xMapRegexPattern = Pattern.compile("pl3xmap: *[\"'](.+)[\"']");
    public static final Pattern SquareMapRegexPattern = Pattern.compile("squaremap: *[\"'](.+)[\"']");
    List<MapConnection> mapConnections = new ArrayList<>();
    int mapIndex;

    @Override
    public void setCurrentDimension(String currentDimension) {
        for (MapConnection connection : mapConnections) {
            connection.setCurrentDimension(currentDimension);
        }
    }

    public LiveAtlasConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry);
        try {
            setupConnections(serverEntry, true);
        } catch (Exception a) {
            try {
                setupConnections(serverEntry, false);
            } catch (Exception b) {
                b.addSuppressed(a);
                if (!updateTask.linkBrokenErrorWasShown) {
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your LiveAtlas link is broken!");
                }
                throw b;
            }
        }
    }

    private void setupConnections(ModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);
        String liveAtlasHTML = HTTP.makeTextHttpRequest(URI.create(baseURL).toURL(), true);
        Matcher matcher = dynmapRegexPattern.matcher(liveAtlasHTML);
        while (matcher.find()) {
            String g = matcher.group();
            try {
                mapConnections.add(new DynmapConnection(serverEntry, baseURL, g, true));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Dynmap connection for LiveAtlas", e);
            }
        }
        matcher = Pl3xMapRegexPattern.matcher(liveAtlasHTML);
        while (matcher.find()) {
            String g = matcher.group(1);
            try {
                mapConnections.add(new Pl3xMapConnection(serverEntry, baseURL, g, true));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Pl3xMap connection for LiveAtlas", e);
            }
        }
        matcher = SquareMapRegexPattern.matcher(liveAtlasHTML);
        while (matcher.find()) {
            String g = matcher.group(1);
            try {
                mapConnections.add(new SquareMapConnection(serverEntry, baseURL, g, true));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Squaremap connection for LiveAtlas", e);
            }
        }
        // use default /standalone/config.js
        if (mapConnections.isEmpty()) {
            try {
                mapConnections.add(new DynmapConnection(serverEntry, baseURL,
                        HTTP.makeTextHttpRequest(URI.create(baseURL + "/standalone/config.js").toURL()), true));
            } catch (Exception e) {
                AbstractModInitializer.LOGGER.error("error creating Dynmap connection for LiveAtlas", e);
            }
        }
    }

    @Override
    public HashMap<String, PlayerPosition> getPlayerPositions() throws IOException {
        if (mapConnections.isEmpty()) return new HashMap<>();
        if (config.general.debugMode) {
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

    private int lastMapIndex;

    @Override
    public void getWaypointPositions(boolean forceRefresh) throws IOException {
        if (mapConnections.isEmpty()) {
            if (ClientMapHandler.getInstance() != null) {
                ClientMapHandler.getInstance().removeAllMarkerWaypoints();
                ClientMapHandler.getInstance().removeAllAreaMarkers(true);
            }
            return;
        }

        if (serverEntry.needsMarkerLayerUpdate()) {
            serverEntry.setMarkerLayers(new ArrayList<>(getMarkerLayers()));
        }

        mapConnections.get(mapIndex).getWaypointPositions(forceRefresh || mapIndex != lastMapIndex);
        lastMapIndex = mapIndex;
    }

    @Override
    public Set<String> getMarkerLayers() {
        HashSet<String> layers = new HashSet<>();
        for (MapConnection connection : mapConnections) {
            layers.addAll(connection.getMarkerLayers());
        }
        return layers;
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
