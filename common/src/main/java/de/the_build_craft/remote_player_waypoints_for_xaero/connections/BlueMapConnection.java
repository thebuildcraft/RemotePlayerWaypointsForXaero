/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <https://www.gnu.org/licenses/>.*/


package de.the_build_craft.remote_player_waypoints_for_xaero.connections;

import de.the_build_craft.remote_player_waypoints_for_xaero.*;
import de.the_build_craft.remote_player_waypoints_for_xaero.mapUpdates.BlueMapMarkerSet;
import de.the_build_craft.remote_player_waypoints_for_xaero.mapUpdates.BlueMapPlayerUpdate;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;

public class BlueMapConnection extends MapConnection {
    public int lastWorldIndex;
    public List<URL> playerUrls;
    public List<URL> markerUrls;

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
                    mc.inGameHud.getChatHud().addMessage(Text.literal("[" + RemotePlayerWaypointsForXaero.MOD_NAME + "]: Error: Your Bluemap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    private void generateLinks(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);
        RemotePlayerWaypointsForXaero.LOGGER.info("baseURL " + baseURL);
        // Get config and build the urls
        for (var w : ((BlueMapConfiguration) HTTP.makeJSONHTTPRequest(
                URI.create(baseURL + "/settings.json?").toURL(), BlueMapConfiguration.class)).maps){
            playerUrls.add(URI.create(baseURL + "/maps/" + w + "/live/players.json?").toURL());
            markerUrls.add(URI.create(baseURL + "/maps/" + w + "/live/markers.json?").toURL());
        }

        // Test the urls
        var a = this.getPlayerPositions();

        for (var url : playerUrls){
            RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + url);
            if (CommonModConfig.Instance.debugMode()){
                mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + url));
            }
        }

        for (var url : markerUrls){
            RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + url);
            if (CommonModConfig.Instance.debugMode()){
                mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + url));
            }
        }
    }

    @Override
    public WaypointPosition[] getWaypointPositions() throws IOException {
        Type apiResponseType = new TypeToken<Map<String, BlueMapMarkerSet>>() {}.getType();
        URL reqUrl = markerUrls.get(lastWorldIndex);
        Map<String, BlueMapMarkerSet> markerSets = HTTP.makeJSONHTTPRequest(reqUrl, apiResponseType);

        ArrayList<WaypointPosition> positions = new ArrayList<>();

        for (var m : markerSets.entrySet()){
            if (CommonModConfig.Instance.debugMode()){
                mc.inGameHud.getChatHud().addMessage(Text.literal("===================================="));
                mc.inGameHud.getChatHud().addMessage(Text.literal("markerSet: " + m.getKey()));
            }

            for(BlueMapMarkerSet.Marker marker : m.getValue().markers.values()){
                if (Objects.equals(marker.type, "poi") || Objects.equals(marker.type, "html")){
                    BlueMapMarkerSet.Position pos = marker.position;
                    positions.add(new WaypointPosition(marker.label, Math.round(pos.x), Math.round(pos.y), Math.round(pos.z)));
                }
            }
        }

        return positions.toArray(new WaypointPosition[0]);
    }

    @Override
    public PlayerPosition[] getPlayerPositions() throws IOException {
        URL reqUrl = playerUrls.get(lastWorldIndex);
        BlueMapPlayerUpdate update = HTTP.makeJSONHTTPRequest(reqUrl, BlueMapPlayerUpdate.class);
        String clientName = mc.player.getName().getString();
        boolean correctWorld = false;

        for (var p : update.players){
            if (Objects.equals(p.name, clientName)){
                correctWorld = !p.foreign;
                break;
            }
        }
        if (!correctWorld){
            for (int i = 0; i < playerUrls.size(); i++) {
                var u = playerUrls.get(i);
                update = HTTP.makeJSONHTTPRequest(playerUrls.get(i), BlueMapPlayerUpdate.class);
                for (var p : update.players){
                    if (Objects.equals(p.name, clientName)){
                        correctWorld = !p.foreign;
                        break;
                    }
                }
                if (correctWorld) {
                    lastWorldIndex = i;
                    break;
                }
            }
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
