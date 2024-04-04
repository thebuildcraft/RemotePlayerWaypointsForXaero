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
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.Objects;
import java.util.TimerTask;

/**
 * Threaded task that is run once every few seconds to fetch data from dynmap
 * and update the local maps
 */
public class UpdateTask extends TimerTask {
    private final MinecraftClient mc;

    public UpdateTask() {
        this.mc = MinecraftClient.getInstance();
    }
    private static final String DEFAULT_PLAYER_SET_NAME = "RemotePlayerWaypointsForXaero_Temp";

    @Override
    public void run() {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        var cw = WaypointsManager.getCustomWaypoints("RemotePlayerWaypointsForXaero");
        cw.clear();

        RemotePlayerWaypointsForXaero.enabled = config.general.enabled;

        // Skip if disabled
        if (!config.general.enabled) {
            RemotePlayerWaypointsForXaero.setConnection(null);
            return;
        }

        // Skip if not in game
        if (mc.isInSingleplayer() || mc.getNetworkHandler() == null
                || !mc.getNetworkHandler().getConnection().isOpen()) {
            RemotePlayerWaypointsForXaero.setConnection(null);
            return;
        }

        // Get the IP of this server
        String serverIP = mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);

        if (RemotePlayerWaypointsForXaero.getConnection() == null){
            try {
                ModConfig.ServerEntry serverEntry = null;
                for (var server : config.general.serverEntries){
                    //RemotePlayerWaypointsForXaero.LOGGER.info(server.ip);
                    if (Objects.equals(serverIP, server.ip.toLowerCase(Locale.ROOT))){
                        serverEntry = server;
                    }
                }
                if (Objects.equals(serverEntry, null)){
                    RemotePlayerWaypointsForXaero.connected = false;
                    return;
                }
                RemotePlayerWaypointsForXaero.setConnection(new DynmapConnection(serverEntry));
            } catch (IOException e) {
                e.printStackTrace();
                RemotePlayerWaypointsForXaero.connected = false;
                return;
            }
        }
        // Access the current waypoint world
        WaypointWorld currentWorld = XaeroMinimapSession.getCurrentSession().getWaypointsManager().getCurrentWorld();

        // Skip if the world is null
        if (currentWorld == null) {
            RemotePlayerWaypointsForXaero.LOGGER.info("Player left world, disconnecting from dynmap");
            RemotePlayerWaypointsForXaero.setConnection(null);
            return;
        }

        if (config.general.debugMode) mc.inGameHud.getChatHud().addMessage(Text.literal("=========="));
        // Get a list of all player's positions
        PlayerPosition[] positions;
        try {
            positions = RemotePlayerWaypointsForXaero.getConnection().getAllPlayerPositions();
        } catch (IOException e) {
            RemotePlayerWaypointsForXaero.LOGGER.warn("Failed to make remote request");
            e.printStackTrace();
            RemotePlayerWaypointsForXaero.setConnection(null);
            return;
        }

        RemotePlayerWaypointsForXaero.connected = true;

        if (!currentWorld.getSets().containsKey(DEFAULT_PLAYER_SET_NAME)){
            currentWorld.addSet(DEFAULT_PLAYER_SET_NAME);
        }
        var waypointList = currentWorld.getSets().get(DEFAULT_PLAYER_SET_NAME).getList();
        var clientplayer = MinecraftClient.getInstance().player;
        if (config.general.debugMode) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("before adding waypoints loop"));
        }
        try {
            synchronized (waypointList) {
                waypointList.clear();

                // Add each player to the map
                for (PlayerPosition playerPosition : positions) {
                    if (config.general.debugMode) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal("before player null check"));
                    }
                    if (playerPosition == null) {
                        continue;
                    }

                    if (config.general.debugMode) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal("after player null check"));
                    }

                    var d = clientplayer.getPos().distanceTo(new Vec3d(playerPosition.x, playerPosition.y, playerPosition.z));
                    if (d < config.general.minDistance || d > config.general.maxDistance) continue;

                    if (config.general.debugMode) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal("player after other checks"));
                    }
                    // Add waypoint for the player
                    try {
                        waypointList.add(new PlayerWaypoint(playerPosition));
                    } catch (NullPointerException e) {
                        RemotePlayerWaypointsForXaero.LOGGER.warn("cant add waypoint");
                        e.printStackTrace();
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            RemotePlayerWaypointsForXaero.LOGGER.warn("waypoint error");
            e.printStackTrace();
        }

        RemotePlayerWaypointsForXaero.AfkColor = config.general.AfkColor;
        RemotePlayerWaypointsForXaero.unknownAfkStateColor = config.general.unknownAfkStateColor;
        RemotePlayerWaypointsForXaero.showAfkTimeInTabList = config.general.showAfkTimeInTabList;

        if (config.general.updateDelay != RemotePlayerWaypointsForXaero.TimerDelay){
            RemotePlayerWaypointsForXaero.setUpdateDelay(config.general.updateDelay);
        }
    }
}