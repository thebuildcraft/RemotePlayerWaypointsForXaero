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
import de.the_build_craft.maplink.common.configurations.Pl3xMapConfiguration;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.mapUpdates.*;
import de.the_build_craft.maplink.common.waypoints.*;
import de.the_build_craft.maplink.common.wrappers.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 06.08.2026
 */
public class Pl3xMapConnection extends MapConnection{
    private String markerLayerStringTemplate = "";
    private String markerStringTemplate = "";
    private String markerIconLinkTemplate = "";
    int version;
    public Pl3xMapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry);
        try {
            generateLink(serverEntry, true);
        }
        catch (Exception a) {
            try {
                generateLink(serverEntry, false);
            }
            catch (Exception b) {
                b.addSuppressed(a);
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: Error: Your Pl3xMap link is broken!");
                }
                throw b;
            }
        }
    }

    public Pl3xMapConnection(ModConfig.ServerEntry serverEntry, String baseURL, String link, boolean partOfLifeAtlas) throws IOException {
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
        onlineMapConfigLink = baseURL + "/tiles/settings.json";
        markerLayerStringTemplate = baseURL + "/tiles/{world}/markers.json";
        markerIconLinkTemplate = baseURL + "/images/icon/registered/{icon}.png";

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
        if (config.general.debugMode){
            Utils.sendToClientChat("new link: " + queryURL);
        }

        setUpdateDelay();
    }

    private void setUpdateDelay() {
        try {
            Type apiResponseType = new TypeToken<Pl3xMapMarkerLayerConfig[]>() {}.getType();
            Pl3xMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, Pl3xMapPlayerUpdate.class);
            float updateDelay = 1;
            for (Pl3xMapPlayerUpdate.WorldSetting ws : update.worldSettings) {
                Pl3xMapMarkerLayerConfig[] mls = HTTP.makeJSONHTTPRequest(URI.create(markerLayerStringTemplate
                        .replace("{world}", ws.name.replaceAll(":", "-"))).toURL(), apiResponseType);
                for (Pl3xMapMarkerLayerConfig ml : mls) {
                    if (Objects.equals(ml.key, "pl3xmap_players")) {
                        updateDelay = Math.max(updateDelay, ml.updateInterval);
                    }
                }
            }
            UpdateTask.nextUpdateDelay = Math.max(UpdateTask.nextUpdateDelay, (int) Math.ceil(updateDelay * 1000));
        } catch (Exception e) {
            AbstractModInitializer.LOGGER.error("Error getting update Delay! Using the Default of 1000 ms for Pl3xMap.", e);
            UpdateTask.nextUpdateDelay = Math.max(UpdateTask.nextUpdateDelay, 1000);
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
            PlayerPosition playerPosition = null;
            if (version == 0) playerPosition = new PlayerPosition(player.name, player.position.x, config.general.defaultY, player.position.z, player.world);
            else if (version == 1) playerPosition = new PlayerPosition(player.name, player.x, config.general.defaultY, player.z, player.world);
            if (playerPosition == null) continue;
            ClientMapHandler.registerPlayerPosition(playerPosition, "https://mc-heads.net/avatar/" + player.uuid + "/32");
            positions[i] = playerPosition;
        }

        return HandlePlayerPositions(positions);
    }

    private String lastMarkerDimension = "";
    int lastMarkerHash;
    int lastAreaMarkerHash;
    List<Position> positions = new ArrayList<>();
    List<AreaMarker> areaMarkers = new ArrayList<>();
    private final Map<String, Function<Pl3xMapMarkerUpdate, Int3[][]>> areaTypes = new HashMap<>();
    {
        areaTypes.put("circ", m -> new Int3[][]{convertCircleToPolygon(m.data.center, m.data.radius)});
        areaTypes.put("multipoly", m -> Arrays.stream(m.data.polygons).flatMap(p -> Arrays.stream(p.polylines)).map(pl -> pl.points).toArray(Int3[][]::new));
        areaTypes.put("poly", m -> Arrays.stream(m.data.polylines).map(pl -> pl.points).toArray(Int3[][]::new));
        areaTypes.put("line", m -> new Int3[][]{m.data.points});
        areaTypes.put("rect", m -> new Int3[][]{{m.data.point1, new Int3(m.data.point1.x, 0, m.data.point2.z), m.data.point2, new Int3(m.data.point2.x, 0, m.data.point1.z)}});
    }

    @Override
    public void getWaypointPositions(boolean forceRefresh) throws IOException {
        if (markerLayerStringTemplate.isEmpty() || currentDimension.isEmpty()) {
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

        if (markerStringTemplate.isEmpty() && version == 0) {
            ClientMapHandler.getInstance().removeAllMarkerWaypoints();
            ClientMapHandler.getInstance().removeAllAreaMarkers(true);
            lastMarkerDimension = currentDimension;
            return;
        }
        int newMarkerHash = serverEntry.getMarkerVisibilityHash();
        int newAreaMarkerHash = serverEntry.getAreaMarkerVisibilityHash();
        if (lastMarkerDimension.equals(currentDimension)
                && newAreaMarkerHash == lastAreaMarkerHash
                && newMarkerHash == lastMarkerHash
                && !forceRefresh) {
            ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
            return;
        }
        lastMarkerDimension = currentDimension;
        lastMarkerHash = newMarkerHash;
        lastAreaMarkerHash = newAreaMarkerHash;

        positions.clear();
        areaMarkers.clear();

        if (version == 0) {
            for (MarkerLayer layer : getMarkerLayers(false)){
                Type apiResponseType = new TypeToken<Pl3xMapMarkerUpdate[]>() {}.getType();
                URL reqUrl = URI.create(markerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))
                        .replace("{layerName}", layer.id)).toURL();
                Pl3xMapMarkerUpdate[] markers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                for (Pl3xMapMarkerUpdate marker : markers){
                    if (Objects.equals(marker.type, "icon") && serverEntry.includeMarkerLayer(layer.id) && serverEntry.includeMarker(marker.options.tooltip.content)) {
                        Position position = new Position(marker.options.tooltip.content, marker.data.point.x, config.general.defaultY, marker.data.point.z, currentDimension + layer.id + marker.data.key, layer);
                        ClientMapHandler.registerPosition(position,
                                (!config.general.showDefaultMarkerIcons && marker.data.image.equals("marker-icon")) ? null : markerIconLinkTemplate.replace("{icon}", marker.data.image));
                        positions.add(position);
                    }
                    if (!(serverEntry.includeAreaMarkerLayer(layer.id) && areaTypes.containsKey(marker.type) && serverEntry.includeAreaMarker(marker.options.tooltip.content))) continue;
                    Int3[][] polygons = areaTypes.get(marker.type).apply(marker);
                    areaMarkers.add(new AreaMarker(marker.options.tooltip.content,
                            0,
                            0,
                            0,
                            polygons,
                            (marker.options.stroke != null && marker.options.stroke.enabled) ? new Color(marker.options.stroke.color) : new Color(),
                            (marker.options.fill != null && marker.options.fill.enabled) ? new Color(marker.options.fill.color) : new Color(),
                            currentDimension + layer.id + marker.data.key,
                            layer));
                }
            }
        } else if (version == 1) {
            Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();
            URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
            SquareMapMarkerUpdate[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

            for (SquareMapMarkerUpdate layer : markerLayers){
                if (!Objects.equals(layer.id, "pl3xmap_players")) {
                    for (SquareMapMarkerUpdate.Marker marker : layer.markers) {
                        if (Objects.equals(marker.type, "icon") && serverEntry.includeMarkerLayer(layer.id) && serverEntry.includeMarker(marker.tooltip)) {
                            Position position = new Position(marker.tooltip, marker.point.x, config.general.defaultY, marker.point.z, currentDimension + layer.id + marker.tooltip + marker.point.x + marker.point.z, new MarkerLayer(layer.id, layer.name));
                            ClientMapHandler.registerPosition(position, marker.icon.equals("marker-icon") ? null : markerIconLinkTemplate.replace("{icon}", marker.icon));
                            positions.add(position);
                        }
                        else if (Objects.equals(marker.type, "polygon") && serverEntry.includeAreaMarkerLayer(layer.id) && serverEntry.includeAreaMarker(marker.tooltip)) {
                            areaMarkers.add(new AreaMarker(marker.tooltip, 0, 0, 0, Arrays.stream(marker.points).flatMap(Arrays::stream).toArray(Int3[][]::new),
                                    new Color(marker.color, 1f), new Color(marker.fillColor, marker.opacity), currentDimension + layer.id + marker.tooltip + Arrays.deepHashCode(marker.points), new MarkerLayer(layer.id, layer.name)));
                        }
                    }
                }
            }
        }
        ClientMapHandler.getInstance().handleMarkerWaypoints(positions);
        ClientMapHandler.getInstance().handleAreaMarkers(areaMarkers);
    }

    Int3[] convertCircleToPolygon(Int3 center, float radius) {
        int N = 40;
        Int3[] points = new Int3[N];
        for (int i = 0; i < N; i++) {
            double a = (Math.PI * 2 / N) * i;
            points[i] = new Double3(center.x + radius * Math.sin(a), center.y, center.z + radius * Math.cos(a)).roundToInt3();
        }
        return points;
    }

    @Override
    public Set<String> getMarkerLayers() {
        return getMarkerLayers(true).stream().map(m -> m.id).collect(Collectors.toSet());
    }

    private Set<MarkerLayer> getMarkerLayers(boolean all) {
        try {
            if (version == 0) {
                Type apiResponseType = new TypeToken<Pl3xMapMarkerLayerConfig[]>() {}.getType();

                if (all) {
                    Pl3xMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(queryURL, Pl3xMapPlayerUpdate.class);
                    Set<MarkerLayer> layerSet = new HashSet<>();
                    for (Pl3xMapPlayerUpdate.WorldSetting ws : update.worldSettings) {
                        Pl3xMapMarkerLayerConfig[] mls = HTTP.makeJSONHTTPRequest(URI.create(markerLayerStringTemplate
                                .replace("{world}", ws.name.replaceAll(":", "-"))).toURL(), apiResponseType);
                        for (Pl3xMapMarkerLayerConfig ml : mls) {
                            if (!Objects.equals(ml.key, "pl3xmap_players")) {
                                layerSet.add(new MarkerLayer(ml.key, ml.label));
                            }
                        }
                    }
                    return layerSet;
                }

                URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
                Pl3xMapMarkerLayerConfig[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                Set<MarkerLayer> layers = new HashSet<>();

                for (Pl3xMapMarkerLayerConfig layer : markerLayers){
                    if (!Objects.equals(layer.key, "pl3xmap_players")) {
                        layers.add(new MarkerLayer(layer.key, layer.label));
                    }
                }

                return layers;
            }
            if (version == 1) {
                Type apiResponseType = new TypeToken<SquareMapMarkerUpdate[]>() {}.getType();

                if (all) {
                    Pl3xMapConfiguration configuration = HTTP.makeJSONHTTPRequest(URI.create(onlineMapConfigLink).toURL(), Pl3xMapConfiguration.class);
                    Set<MarkerLayer> layerSet = new HashSet<>();
                    for (Pl3xMapConfiguration.World ws : configuration.worlds) {
                        SquareMapMarkerUpdate[] mls = HTTP.makeJSONHTTPRequest(URI.create(markerLayerStringTemplate.replace("{world}", ws.name.replaceAll(":", "-"))).toURL(), apiResponseType);
                        for (SquareMapMarkerUpdate ml : mls) {
                            if (!Objects.equals(ml.id, "pl3xmap_players")) {
                                layerSet.add(new MarkerLayer(ml.id, ml.name));
                            }
                        }
                    }
                    return layerSet;
                }

                URL reqUrl = URI.create(markerLayerStringTemplate.replace("{world}", currentDimension.replaceAll(":", "-"))).toURL();
                SquareMapMarkerUpdate[] markerLayers = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

                Set<MarkerLayer> layers = new HashSet<>();

                for (SquareMapMarkerUpdate layer : markerLayers){
                    if (!Objects.equals(layer.id, "pl3xmap_players")) {
                        layers.add(new MarkerLayer(layer.id, layer.name));
                    }
                }

                return layers;
            }
            throw new IllegalStateException();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
