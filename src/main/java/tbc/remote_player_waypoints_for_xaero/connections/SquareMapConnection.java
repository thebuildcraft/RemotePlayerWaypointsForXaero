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
import tbc.remote_player_waypoints_for_xaero.MapUpdates.SquareMapUpdate;

import java.io.IOException;
import java.net.URI;

public class SquareMapConnection extends MapConnection {
    public SquareMapConnection(ModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        try {
            generateLink(serverEntry, config, false);
        }
        catch (Exception ignored){
            try {
                generateLink(serverEntry, config, true);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    mc.inGameHud.getChatHud().addMessage(Text.literal("Error: Your Squaremap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    private void generateLink(ModConfig.ServerEntry serverEntry, ModConfig config, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);

        // Build the url
        queryURL = URI.create(baseURL + "/tiles/players.json").toURL();
        // Test the url
        var a = this.getPlayerPositions(config);

        RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
        if (config.general.debugMode){
            mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + queryURL));
        }
    }

    @Override
    public PlayerPosition[] getPlayerPositions(ModConfig config) throws IOException {
        // Make request for all players
        SquareMapUpdate update = HTTP.makeJSONHTTPRequest(queryURL, SquareMapUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            SquareMapUpdate.Player player = update.players[i];
            positions[i] = new PlayerPosition(player.name, player.x, config.general.defaultY, player.z, player.world);
        }

        return HandlePlayerPositions(positions, config);
    }
}
