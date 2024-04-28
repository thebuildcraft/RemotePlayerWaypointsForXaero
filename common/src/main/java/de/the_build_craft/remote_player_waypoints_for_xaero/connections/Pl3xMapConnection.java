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
import de.the_build_craft.remote_player_waypoints_for_xaero.mapUpdates.Pl3xMapUpdate;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.URI;

public class Pl3xMapConnection extends MapConnection{
    public Pl3xMapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) throws IOException {
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
                    mc.inGameHud.getChatHud().addMessage(Text.literal("[" + RemotePlayerWaypointsForXaero.MOD_NAME + "]: Error: Your Pl3xMap link is broken!").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                throw e;
            }
        }
    }

    private void generateLink(CommonModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = getBaseURL(serverEntry, useHttps);

        // Build the url
        queryURL = URI.create(baseURL + "/tiles/settings.json").toURL();
        // Test the url
        var a = this.getPlayerPositions();

        RemotePlayerWaypointsForXaero.LOGGER.info("new link: " + queryURL);
        if (CommonModConfig.Instance.debugMode()){
            mc.inGameHud.getChatHud().addMessage(Text.literal("new link: " + queryURL));
        }
    }

    @Override
    public PlayerPosition[] getPlayerPositions() throws IOException {
        // Make request for all players
        Pl3xMapUpdate update = HTTP.makeJSONHTTPRequest(queryURL, Pl3xMapUpdate.class);

        // Build a list of positions
        PlayerPosition[] positions = new PlayerPosition[update.players.length];
        for (int i = 0; i < update.players.length; i++){
            Pl3xMapUpdate.Player player = update.players[i];
            positions[i] = new PlayerPosition(player.name, player.position.x, CommonModConfig.Instance.defaultY(), player.position.z, player.world);
        }

        return HandlePlayerPositions(positions);
    }
}
