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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import tbc.remote_player_waypoints_for_xaero.connections.DynmapConnection;
import tbc.remote_player_waypoints_for_xaero.connections.SquareMapConnection;
import xaero.common.HudMod;
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

    private boolean connectionErrorWasShown = false;
    private boolean cantFindServerErrorWasShown = false;
    private boolean cantGetPlayerPositionsErrorWasShown = false;
    public boolean linkBrokenErrorWasShown = false;

    @Override
    public void run() {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        var cw = WaypointsManager.getCustomWaypoints("RemotePlayerWaypointsForXaero");
        cw.clear();

        RemotePlayerWaypointsForXaero.enabled = config.general.enabled;

        // Skip if disabled
        if (!config.general.enabled) {
            Reset();
            return;
        }

        // Skip if not in game
        if (mc.isInSingleplayer() || mc.getNetworkHandler() == null
                || !mc.getNetworkHandler().getConnection().isOpen()) {
            Reset();
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
                    if (!(config.general.ignoredServers.contains(serverIP) || cantFindServerErrorWasShown)) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal("Could not find an online map link for this server. Make sure to add it to the config. ").setStyle(Style.EMPTY.withColor(Formatting.GOLD)).append(Text.literal("[ignore this server]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ignore_server")))));
                        cantFindServerErrorWasShown = true;
                    }
                    RemotePlayerWaypointsForXaero.connected = false;
                    return;
                }
                switch (serverEntry.maptype){
                    case Dynmap -> RemotePlayerWaypointsForXaero.setConnection(new DynmapConnection(serverEntry, this));
                    case Squaremap -> RemotePlayerWaypointsForXaero.setConnection(new SquareMapConnection(serverEntry, this));
                    default -> throw new IllegalStateException("Unexpected value: " + serverEntry.maptype);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                if (!connectionErrorWasShown){
                    connectionErrorWasShown = true;
                    mc.inGameHud.getChatHud().addMessage(Text.literal("Error while connecting to the online map. Please check you config or report a bug.").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                }
                RemotePlayerWaypointsForXaero.connected = false;
                return;
            }
        }
        // Access the current waypoint world
        WaypointWorld currentWorld = XaeroMinimapSession.getCurrentSession().getWaypointsManager().getCurrentWorld();

        // Skip if the world is null
        if (currentWorld == null) {
            RemotePlayerWaypointsForXaero.LOGGER.info("Player left world, disconnecting from dynmap");
            Reset();
            return;
        }

        if (config.general.debugMode) mc.inGameHud.getChatHud().addMessage(Text.literal("=========="));
        // Get a list of all player's positions
        PlayerPosition[] positions;
        try {
            positions = RemotePlayerWaypointsForXaero.getConnection().getPlayerPositions(config);
        } catch (IOException e) {
            if (!cantGetPlayerPositionsErrorWasShown){
                cantGetPlayerPositionsErrorWasShown = true;
                mc.inGameHud.getChatHud().addMessage(Text.literal("Failed to make online map request. Please check your config (probably your link...) or report a bug.").setStyle(Style.EMPTY.withColor(Formatting.RED)));
            }
            e.printStackTrace();
            RemotePlayerWaypointsForXaero.setConnection(null);
            return;
        }

        RemotePlayerWaypointsForXaero.connected = true;
        HudMod.INSTANCE.getSettings().renderAllSets = true;

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

    private void Reset() {
        RemotePlayerWaypointsForXaero.setConnection(null);
        connectionErrorWasShown = false;
        cantFindServerErrorWasShown = false;
        cantGetPlayerPositionsErrorWasShown = false;
        linkBrokenErrorWasShown = false;
    }
}