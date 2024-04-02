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

package tbc.remote_player_waypoints_for_xaero;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a connection to a dynmap server
 */
public class DynmapConnection {
    private URL queryURL;
    private final MinecraftClient mc;

    public DynmapConnection(ModConfig.ServerEntry serverEntry) throws IOException {
        mc = MinecraftClient.getInstance();
        try {
            var baseURL = serverEntry.link.toLowerCase(Locale.ROOT);
            if (!baseURL.startsWith("http://")){
                baseURL = "http://" + baseURL;
            }
            int i = baseURL.indexOf("/", 8);
            if (i != -1){
                baseURL = baseURL.substring(0, i);
            }

            // Get the first world name
            var firstWorldName = ((DynmapConfiguration) HTTP.makeJSONHTTPRequest(
                    new URL(baseURL + "/up/configuration"), DynmapConfiguration.class)).worlds[0].name;

            // Build the url
            queryURL = new URL(baseURL + "/up/world/" + firstWorldName + "/");
            RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
        }
        catch (Exception ignored){
            mc.inGameHud.getChatHud().addMessage(Text.literal("Error: Your Dynmap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
        }
    }

    /**
     * Ask the server for a list of all player positions
     *
     * @return Player positions
     * @throws IOException
     */
    public PlayerPosition[] getAllPlayerPositions() throws IOException {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        // Make request for all players
        DynmapUpdate update = HTTP.makeJSONHTTPRequest(queryURL, DynmapUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        String clientName = mc.player.getName().getLiteralString();
        String currentDimension = "";
        for (var p : update.players){
            if (Objects.equals(p.account, clientName)) {
                currentDimension = p.world;
            }
        }

        for (int i = 0; i < update.players.length; i++) {
            var playerPosition = new PlayerPosition(update.players[i].account, update.players[i].x, update.players[i].y, update.players[i].z);
            if (RemotePlayerWaypointsForXaero.lastPlayerDataDic.containsKey(playerPosition.player)) {
                if (RemotePlayerWaypointsForXaero.lastPlayerDataDic.get(playerPosition.player).CompareCords(playerPosition)) {
                    if (RemotePlayerWaypointsForXaero.AfkTimeDic.containsKey(playerPosition.player)) {
                        RemotePlayerWaypointsForXaero.AfkTimeDic.put(playerPosition.player, RemotePlayerWaypointsForXaero.AfkTimeDic.get(playerPosition.player) + (config.general.updateDelay / 1000));
                    } else {
                        RemotePlayerWaypointsForXaero.AfkTimeDic.put(playerPosition.player, (config.general.updateDelay / 1000));
                    }
                    if (config.general.debugMode) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal(playerPosition.player + "  afk_time: " + RemotePlayerWaypointsForXaero.AfkTimeDic.get(playerPosition.player)));
                    }
                    if (RemotePlayerWaypointsForXaero.AfkTimeDic.get(playerPosition.player) >= config.general.timeUntilAfk) {
                        RemotePlayerWaypointsForXaero.AfkDic.put(playerPosition.player, true);
                    }
                } else {
                    RemotePlayerWaypointsForXaero.AfkTimeDic.put(playerPosition.player, 0);
                    RemotePlayerWaypointsForXaero.AfkDic.put(playerPosition.player, false);
                }
            }
            RemotePlayerWaypointsForXaero.lastPlayerDataDic.put(playerPosition.player, playerPosition);

            if (!config.general.debugMode && (!Objects.equals(update.players[i].world, currentDimension) || Objects.equals(update.players[i].account, clientName))) {
                continue;
            }
            positions[i] = playerPosition;
        }

        return positions;
    }

}