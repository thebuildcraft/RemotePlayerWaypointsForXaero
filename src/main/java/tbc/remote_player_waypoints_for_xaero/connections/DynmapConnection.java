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
import tbc.remote_player_waypoints_for_xaero.MapUpdates.DynmapUpdate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * Represents a connection to a dynmap server
 */
public class DynmapConnection extends MapConnection {
    public DynmapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        try {
            var baseURL = serverEntry.link.toLowerCase(Locale.ROOT);
            if (!baseURL.startsWith("http://")){
                baseURL = "http://" + baseURL;
            }

            int i = baseURL.indexOf("?");
            if (i != -1){
                baseURL = baseURL.substring(0, i - 1);
            }

            i = baseURL.indexOf("#");
            if (i != -1){
                baseURL = baseURL.substring(0, i - 1);
            }

            if (baseURL.endsWith("/")){
                baseURL = baseURL.substring(0, baseURL.length() - 1);
            }

            // Get the first world name. I know it seems random. Just trust me...
            var firstWorldName = ((DynmapConfiguration) HTTP.makeJSONHTTPRequest(
                    URI.create(baseURL + "/up/configuration").toURL(), DynmapConfiguration.class)).worlds[0].name;

            // Build the url
            queryURL = URI.create(baseURL + "/up/world/" + firstWorldName + "/").toURL();
            // Test the url
            var a = this.getPlayerPositions(config);

            RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
            if (config.general.debugMode){
                mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + queryURL));
            }
        }
        catch (Exception ignored){
            try {
                var baseURL = serverEntry.link.toLowerCase(Locale.ROOT);
                if (!baseURL.startsWith("https://")){
                    baseURL = "https://" + baseURL;
                }

                int i = baseURL.indexOf("?");
                if (i != -1){
                    baseURL = baseURL.substring(0, i - 1);
                }

                i = baseURL.indexOf("#");
                if (i != -1){
                    baseURL = baseURL.substring(0, i - 1);
                }

                if (baseURL.endsWith("/")){
                    baseURL = baseURL.substring(0, baseURL.length() - 1);
                }

                // Get the first world name. I know it seems random. Just trust me...
                var firstWorldName = ((DynmapConfiguration) HTTP.makeJSONHTTPRequest(
                        URI.create(baseURL + "/up/configuration").toURL(), DynmapConfiguration.class)).worlds[0].name;

                // Build the url
                queryURL = URI.create(baseURL + "/up/world/" + firstWorldName + "/").toURL();
                // Test the url
                var a = this.getPlayerPositions(config);

                RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
                if (config.general.debugMode){
                    mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + queryURL));
                }
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    mc.inGameHud.getChatHud().addMessage(Text.literal("Error: Your Dynmap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    /**
     * Ask the server for a list of all player positions
     *
     * @return Player positions
     * @throws IOException
     */
    @Override
    public PlayerPosition[] getPlayerPositions(ModConfig config) throws IOException {
        // Make request for all players
        DynmapUpdate update = HTTP.makeJSONHTTPRequest(queryURL, DynmapUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            DynmapUpdate.Player player = update.players[i];
            positions[i] = new PlayerPosition(player.account, Math.round(player.x), Math.round(player.y), Math.round(player.z), player.world);
        }

        return HandlePlayerPositions(positions, config);
    }

}