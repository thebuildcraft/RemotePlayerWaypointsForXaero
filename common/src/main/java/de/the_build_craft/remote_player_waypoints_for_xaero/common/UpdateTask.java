/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by ewpratten)
 *
 *    Copyright (C) 2024  Leander Knüttel
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

package de.the_build_craft.remote_player_waypoints_for_xaero.common;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.connections.BlueMapConnection;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.connections.DynmapConnection;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.connections.Pl3xMapConnection;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.connections.SquareMapConnection;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Text;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import xaero.common.HudMod;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;

import java.io.IOException;
import java.util.*;

/**
 * Threaded task that is run once every few seconds to fetch data from the online map
 * and update the local maps
 *
 * @author ewpratten
 * @author eatmyvenom
 * @author Leander Knüttel
 * @version 15.06.2024
 */
public class UpdateTask extends TimerTask {
    private final Minecraft mc;

    public UpdateTask() {
        this.mc = Minecraft.getInstance();
    }
    private static final String DEFAULT_PLAYER_SET_NAME = "AbstractModInitializer_Temp";

    private boolean connectionErrorWasShown = false;
    private boolean cantFindServerErrorWasShown = false;
    private boolean cantGetPlayerPositionsErrorWasShown = false;
    public boolean linkBrokenErrorWasShown = false;

    private String currentServerIP = "";

    @Override
    public void run() {
        if (AbstractModInitializer.mapModInstalled){
            Hashtable<Integer, Waypoint> cw = WaypointsManager.getCustomWaypoints("AbstractModInitializer");
            cw.clear();
        }

        AbstractModInitializer.enabled = CommonModConfig.Instance.enabled();

        // Skip if disabled
        if (!CommonModConfig.Instance.enabled()) {
            Reset();
            return;
        }

        // Skip if not in game
        if (mc.isSingleplayer() || mc.getCurrentServer() == null || mc.getConnection() == null
                || !mc.getConnection().getConnection().isConnected()) {
            Reset();
            return;
        }

        // Get the IP of this server
        String serverIP = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);

        if (!Objects.equals(currentServerIP, serverIP)){
            currentServerIP = "";
            Reset();
            AbstractModInitializer.LOGGER.info("Server ip has changed!");
        }

        if (AbstractModInitializer.getConnection() == null){
            try {
                CommonModConfig.ServerEntry serverEntry = null;
                for (CommonModConfig.ServerEntry server : CommonModConfig.Instance.serverEntries()){
                    if (Objects.equals(serverIP, server.ip.toLowerCase(Locale.ROOT))){
                        serverEntry = server;
                    }
                }
                if (Objects.equals(serverEntry, null)) {
                    if (!(CommonModConfig.Instance.ignoredServers().contains(serverIP) || cantFindServerErrorWasShown)) {
                        if ((AbstractModInitializer.INSTANCE.loaderType == LoaderType.Fabric)
                                || (AbstractModInitializer.INSTANCE.loaderType == LoaderType.Quilt)) {
                            Utils.sendToClientChat(Text.literal("[" + AbstractModInitializer.MOD_NAME + "]: " +
                                            "Could not find an online map link for this server. " +
                                            "Make sure to add it to the config. (this server ip was detected: " + serverIP + ") ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)).append(Text.literal("[ignore this server]")
                                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ignore_server")))));
                        } else {
                            Utils.sendToClientChat(Text.literal("[" + AbstractModInitializer.MOD_NAME + "]: " +
                                            "Could not find an online map link for this server. " +
                                            "Make sure to add it to the config. (this server ip was detected: " + serverIP + ") ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)).append(Text.literal("[ignore this server]")
                                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ignore_server")))));
                        }//                                                        RUN_COMMAND doesn't seem to work on Forge and NeoForge...

                        cantFindServerErrorWasShown = true;
                    }
                    AbstractModInitializer.connected = false;
                    return;
                }
                if (Objects.requireNonNull(serverEntry.maptype) == CommonModConfig.ServerEntry.Maptype.Dynmap) {
                    AbstractModInitializer.setConnection(new DynmapConnection(serverEntry, this));
                } else if (serverEntry.maptype == CommonModConfig.ServerEntry.Maptype.Squaremap) {
                    AbstractModInitializer.setConnection(new SquareMapConnection(serverEntry, this));
                } else if (serverEntry.maptype == CommonModConfig.ServerEntry.Maptype.Bluemap) {
                    AbstractModInitializer.setConnection(new BlueMapConnection(serverEntry, this));
                } else if (serverEntry.maptype == CommonModConfig.ServerEntry.Maptype.Pl3xMap) {
                    AbstractModInitializer.setConnection(new Pl3xMapConnection(serverEntry, this));
                } else {
                    throw new IllegalStateException("Unexpected value: " + serverEntry.maptype);
                }
            } catch (Exception e) {
                if (!connectionErrorWasShown){
                    connectionErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: " +
                            "Error while connecting to the online map. " +
                            "Please check you config or report a bug.");
                    e.printStackTrace();
                }
                AbstractModInitializer.connected = false;
                return;
            }
        }
        currentServerIP = serverIP;

        // Get a list of all player's positions
        PlayerPosition[] playerPositions;
        WaypointPosition[] waypointPositions;
        try {
            // this must be run no matter if it's activated in the config, to get the "currentDimension" and AFK info
            playerPositions = AbstractModInitializer.getConnection().getPlayerPositions();
            if (CommonModConfig.Instance.enableMarkerWaypoints()){
                waypointPositions = AbstractModInitializer.getConnection().getWaypointPositions();
            }
            else {
                waypointPositions = new WaypointPosition[0];
            }
        } catch (IOException e) {
            if (!cantGetPlayerPositionsErrorWasShown){
                cantGetPlayerPositionsErrorWasShown = true;
                Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: " +
                        "Failed to make online map request. Please check your config (probably your link...) or report a bug.");
            }
            e.printStackTrace();
            AbstractModInitializer.setConnection(null);
            return;
        }

        AbstractModInitializer.connected = true;
        AbstractModInitializer.AfkColor = CommonModConfig.Instance.AfkColor();
        AbstractModInitializer.unknownAfkStateColor = CommonModConfig.Instance.unknownAfkStateColor();
        AbstractModInitializer.showAfkTimeInTabList = CommonModConfig.Instance.showAfkTimeInTabList();

        if (!AbstractModInitializer.mapModInstalled){
            if (CommonModConfig.Instance.updateDelay() != AbstractModInitializer.TimerDelay){
                AbstractModInitializer.setUpdateDelay(CommonModConfig.Instance.updateDelay());
            }
            return;
        }

        WaypointWorld currentWorld = null;
        try{
            // Access the current waypoint world
            currentWorld = XaeroMinimapSession.getCurrentSession().getWaypointsManager().getCurrentWorld();
        }
        catch (Exception ignored){
        }

        // Skip if the world is null
        if (currentWorld == null) {
            AbstractModInitializer.LOGGER.info("WaypointWorld is null, disconnecting from online map");
            Reset();
            return;
        }
        HudMod.INSTANCE.getSettings().renderAllSets = true;

        if (!currentWorld.getSets().containsKey(DEFAULT_PLAYER_SET_NAME)){
            currentWorld.addSet(DEFAULT_PLAYER_SET_NAME);
        }
        ArrayList<Waypoint> waypointList = currentWorld.getSets().get(DEFAULT_PLAYER_SET_NAME).getList();
        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) return;

        try {
            synchronized (waypointList) {
                waypointList.clear();

                if (CommonModConfig.Instance.enablePlayerWaypoints()){
                    // Add each player to the map
                    for (PlayerPosition playerPosition : playerPositions) {
                        if (playerPosition == null) continue;

                        double d = clientPlayer.position().distanceTo(new Vec3(playerPosition.x, playerPosition.y, playerPosition.z));
                        if (d < CommonModConfig.Instance.minDistance() || d > CommonModConfig.Instance.maxDistance()) continue;
                        // Add waypoint for the player
                        try {
                            waypointList.add(new PlayerWaypoint(playerPosition));
                        } catch (NullPointerException e) {
                            AbstractModInitializer.LOGGER.warn("can't add player waypoint");
                            e.printStackTrace();
                        }
                    }
                }

                for( WaypointPosition waypointPosition : waypointPositions){
                    if (waypointPosition == null) continue;

                    double d = clientPlayer.position().distanceTo(new Vec3(waypointPosition.x, waypointPosition.y, waypointPosition.z));
                    if (d < CommonModConfig.Instance.minDistanceMarker() || d > CommonModConfig.Instance.maxDistanceMarker()) continue;
                    // Add waypoint for the marker
                    try {
                        waypointList.add(new FixedWaypoint(waypointPosition));
                    } catch (NullPointerException e) {
                        AbstractModInitializer.LOGGER.warn("can't add marker waypoint");
                        e.printStackTrace();
                    }
                }
            }
        } catch (ConcurrentModificationException e) {
            AbstractModInitializer.LOGGER.warn("waypoint error");
            e.printStackTrace();
        }

        if (CommonModConfig.Instance.updateDelay() != AbstractModInitializer.TimerDelay){
            AbstractModInitializer.setUpdateDelay(CommonModConfig.Instance.updateDelay());
        }
    }

    private void Reset() {
        AbstractModInitializer.setConnection(null);
        connectionErrorWasShown = false;
        cantFindServerErrorWasShown = false;
        cantGetPlayerPositionsErrorWasShown = false;
        linkBrokenErrorWasShown = false;
    }
}