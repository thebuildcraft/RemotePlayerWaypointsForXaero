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


package tbc.remote_player_waypoints_for_xaero.connections;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tbc.remote_player_waypoints_for_xaero.*;
import tbc.remote_player_waypoints_for_xaero.MapUpdates.BlueMapUpdate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlueMapConnection extends MapConnection {
    public int lastWorldIndex;
    public List<URL> urls;

    public BlueMapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        urls = new ArrayList<>();
        try {
            generateLinks(serverEntry, config, false);
        }
        catch (Exception ignored){
            try {
                generateLinks(serverEntry, config, true);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    mc.inGameHud.getChatHud().addMessage(Text.literal("Error: Your Bluemap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    private void generateLinks(ModConfig.ServerEntry serverEntry, ModConfig config, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);
        RemotePlayerWaypointsForXaero.LOGGER.info("baseURL " + baseURL);
        // Get config and build the urls
        for (var w : ((BlueMapConfiguration) HTTP.makeJSONHTTPRequest(
                URI.create(baseURL + "/settings.json?").toURL(), BlueMapConfiguration.class)).maps){
            urls.add(URI.create(baseURL + "/maps/" + w + "/live/players.json?").toURL());
        }

        // Test the urls
        var a = this.getPlayerPositions(config);

        for (var url : urls){
            RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + url);
            if (config.general.debugMode){
                mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + url));
            }
        }
    }

    @Override
    public PlayerPosition[] getPlayerPositions(ModConfig config) throws IOException {
        BlueMapUpdate update = HTTP.makeJSONHTTPRequest(urls.get(lastWorldIndex), BlueMapUpdate.class);
        String clientName = mc.player.getName().getLiteralString();
        boolean correctWorld = false;

        for (var p : update.players){
            if (Objects.equals(p.name, clientName)){
                correctWorld = !p.foreign;
                break;
            }
        }
        if (!correctWorld){
            for (int i = 0; i < urls.size(); i++) {
                var u = urls.get(i);
                update = HTTP.makeJSONHTTPRequest(urls.get(i), BlueMapUpdate.class);
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
                BlueMapUpdate.Player player = update.players[i];
                positions[i] = new PlayerPosition(player.name, Math.round(player.position.x), Math.round(player.position.y), Math.round(player.position.z), player.foreign ? "foreign" : "thisWorld");
            }
        }
        else {
            for (int i = 0; i < update.players.length; i++) {
                BlueMapUpdate.Player player = update.players[i];
                positions[i] = new PlayerPosition(player.name, Math.round(player.position.x), Math.round(player.position.y), Math.round(player.position.z), "unknown");
            }
        }
        return HandlePlayerPositions(positions, config);
    }
}
