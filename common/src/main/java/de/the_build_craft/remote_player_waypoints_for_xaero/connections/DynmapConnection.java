/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel
        (some parts of this file are originally from "RemotePlayers" by ewpratten)

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
import de.the_build_craft.remote_player_waypoints_for_xaero.mapUpdates.DynmapUpdate;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;

/**
 * Represents a connection to a dynmap server
 */
public class DynmapConnection extends MapConnection {
    public DynmapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
        super(serverEntry, updateTask);
        try {
            generateLink(serverEntry, false);
        }
        catch (Exception ignored){
            try {
                generateLink(serverEntry, true);
            }
            catch (Exception e){
                if (!updateTask.linkBrokenErrorWasShown){
                    updateTask.linkBrokenErrorWasShown = true;
                    mc.inGameHud.getChatHud().addMessage(Text.literal("[" + RemotePlayerWaypointsForXaero.MOD_NAME + "]: Error: Your Dynmap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    private void generateLink(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        var baseURL = getBaseURL(serverEntry, useHttps);

        try{
            // Get the first world name. I know it seems random. Just trust me...
            var firstWorldName = ((DynmapConfiguration) HTTP.makeJSONHTTPRequest(
                    URI.create(baseURL + "/up/configuration").toURL(), DynmapConfiguration.class)).worlds[0].name;

            // Build the url
            queryURL = URI.create(baseURL + "/up/world/" + firstWorldName + "/").toURL();
            // Test the url
            var a = this.getPlayerPositions();
        }
        catch (Exception ignored){
            // Get the first world name. I know it seems random. Just trust me...
            var firstWorldName = ((DynmapConfiguration) HTTP.makeJSONHTTPRequest(
                    URI.create(baseURL + "/standalone/dynmap_config.json?").toURL(), DynmapConfiguration.class)).worlds[0].name;

            // Build the url
            queryURL = URI.create(baseURL + "/standalone/world/" + firstWorldName + ".json?").toURL();
            // Test the url
            var a = this.getPlayerPositions();
        }

        RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
        if (CommonModConfig.Instance.debugMode()){
            mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + queryURL));
        }
    }

    /**
     * Ask the server for a list of all player positions
     *
     * @return Player positions
     * @throws IOException
     */
    @Override
    public PlayerPosition[] getPlayerPositions() throws IOException {
        // Make request for all players
        DynmapUpdate update = HTTP.makeJSONHTTPRequest(queryURL, DynmapUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            DynmapUpdate.Player player = update.players[i];
            positions[i] = new PlayerPosition(player.account, Math.round(player.x), Math.round(player.y), Math.round(player.z), player.world);
        }

        return HandlePlayerPositions(positions);
    }

}