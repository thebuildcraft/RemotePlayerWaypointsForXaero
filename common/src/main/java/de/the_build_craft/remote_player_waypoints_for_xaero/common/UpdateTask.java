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
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
#if MC_VER == MC_1_17_1
import xaero.common.AXaeroMinimap;
#else
import xaero.common.HudMod;
#endif
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;

import java.io.IOException;
import java.util.*;

/**
 * Threaded task that is run once every few seconds to fetch data from the online map
 * and update the local maps
 *
 * @author ewpratten
 * @author eatmyvenom
 * @author TheMrEngMan
 * @author Leander Knüttel
 * @version 07.07.2024
 */
public class UpdateTask extends TimerTask {
    private final Minecraft mc;

    public UpdateTask() {
        this.mc = Minecraft.getInstance();
    }
    private static final String PLAYER_SET_NAME = AbstractModInitializer.MOD_NAME +  "_Player";
    private static final String MARKER_SET_NAME = AbstractModInitializer.MOD_NAME +  "_Marker";

    private boolean connectionErrorWasShown = false;
    private boolean cantFindServerErrorWasShown = false;
    private boolean cantGetPlayerPositionsErrorWasShown = false;
    private boolean cantGetMarkerPositionsErrorWasShown = false;
    private boolean markerMessageWasShown = false;
    public boolean linkBrokenErrorWasShown = false;

    private String currentServerIP = "";
    private final int maxMarkerCountBeforeWarning = 25;

    public static HashMap<String, PlayerPosition> playerPositions;
    public static HashMap<String, WaypointPosition> markerPositions;
    private ArrayList<Waypoint> playerWaypointList = null;
    private ArrayList<Waypoint> markerWaypointList = null;

    private int previousPlayerWaypointColor = 0;
    private int previousMarkerWaypointColor = 0;
    private int previousFriendWaypointColor = 0;
    private boolean previousFriendColorOverride = false;
    private int previousFriendListHashCode = 0;

    @Override
    public void run() {
        try{
            runUpdate();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void runUpdate() {
        // Skip if not in game
        if (mc.level == null
                || mc.player == null
                || mc.cameraEntity == null
                || (mc.getSingleplayerServer() != null && !mc.getSingleplayerServer().isPublished())
                || mc.getCurrentServer() == null
                || mc.getConnection() == null
                || !mc.getConnection().getConnection().isConnected()) {
            Reset();
            return;
        }

        playerWaypointList = null;
        markerWaypointList = null;

        if (AbstractModInitializer.mapModInstalled){
            try{
                // Access the current waypoint world
                WaypointWorld currentWorld = XaeroMinimapSession.getCurrentSession().getWaypointsManager().getCurrentWorld();
                if (!currentWorld.getSets().containsKey(PLAYER_SET_NAME)){
                    currentWorld.addSet(PLAYER_SET_NAME);
                }
                if (!currentWorld.getSets().containsKey(MARKER_SET_NAME)){
                    currentWorld.addSet(MARKER_SET_NAME);
                }
                playerWaypointList = currentWorld.getSets().get(PLAYER_SET_NAME).getList();
                markerWaypointList = currentWorld.getSets().get(MARKER_SET_NAME).getList();
            }
            catch (Exception ignored){
            }
        }

        AbstractModInitializer.enabled = CommonModConfig.Instance.enabled();

        // Skip if disabled
        if (!AbstractModInitializer.enabled) {
            Reset();
            if (playerWaypointList != null){
                playerWaypointList.clear();
            }
            if (markerWaypointList != null){
                markerWaypointList.clear();
            }
            return;
        }

        // Get the IP of this server
        String serverIP = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);

        if (!Objects.equals(currentServerIP, serverIP)){
            currentServerIP = serverIP;
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
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + AbstractModInitializer.MOD_ID + " ignore_server")))));
                        } else {
                            Utils.sendToClientChat(Text.literal("[" + AbstractModInitializer.MOD_NAME + "]: " +
                                            "Could not find an online map link for this server. " +
                                            "Make sure to add it to the config. (this server ip was detected: " + serverIP + ") ")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)).append(Text.literal("[ignore this server]")
                                            .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + AbstractModInitializer.MOD_ID + " ignore_server")))));
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

        // Get a list of all player's positions
        try {
            // this must be run no matter if it's activated in the config, to get the "currentDimension" and AFK info
            playerPositions = AbstractModInitializer.getConnection().getPlayerPositions();
        } catch (IOException e) {
            if (!cantGetPlayerPositionsErrorWasShown){
                cantGetPlayerPositionsErrorWasShown = true;
                Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: " +
                        "Failed to make online map request (for player waypoints). Please check your config (probably your link...) or report a bug.");
            }
            e.printStackTrace();
            AbstractModInitializer.setConnection(null);
            return;
        }

        if (CommonModConfig.Instance.enableMarkerWaypoints()){
            try {
                markerPositions = AbstractModInitializer.getConnection().getWaypointPositions();
            } catch (IOException e) {
                markerPositions = new HashMap<>();
                if (!cantGetMarkerPositionsErrorWasShown) {
                    cantGetMarkerPositionsErrorWasShown = true;
                    Utils.sendErrorToClientChat("[" + AbstractModInitializer.MOD_NAME + "]: " +
                            "Failed to make online map request (for marker waypoints). Please check your config (probably your link...) or report a bug.");
                }
                e.printStackTrace();
            }
        }
        else {
            markerPositions = new HashMap<>();
        }

        AbstractModInitializer.connected = true;
        AbstractModInitializer.AfkColor = CommonModConfig.Instance.AfkColor();
        AbstractModInitializer.unknownAfkStateColor = CommonModConfig.Instance.unknownAfkStateColor();
        AbstractModInitializer.showAfkTimeInTabList = CommonModConfig.Instance.showAfkTimeInTabList();

        if (!AbstractModInitializer.mapModInstalled || playerWaypointList == null || markerWaypointList == null){
            if (CommonModConfig.Instance.updateDelay() != AbstractModInitializer.TimerDelay){
                AbstractModInitializer.setUpdateDelay(CommonModConfig.Instance.updateDelay());
            }
            return;
        }

        #if MC_VER == MC_1_17_1
        AXaeroMinimap.INSTANCE.getSettings().renderAllSets = true;
        #else
        HudMod.INSTANCE.getSettings().renderAllSets = true;
        #endif

        // Update the player positions obtained from Dynmap with GameProfile data from the actual logged-in players
        // This is required so that the entity radar properly shows the player's skin on player head icons
        if(CommonModConfig.Instance.enableEntityRadar()) {
            Collection<PlayerInfo> playerList = mc.getConnection().getOnlinePlayers();
            for (PlayerInfo playerListEntity : playerList) {
                String playerName = playerListEntity.getProfile().getName();
                if (playerPositions.containsKey(playerName)) {
                    playerPositions.get(playerName).gameProfile = playerListEntity.getProfile();
                }
            }
        }

        Vec3 camPosition = mc.cameraEntity.position();

        try {
            synchronized (playerWaypointList) {
                if (CommonModConfig.Instance.enablePlayerWaypoints()){
                    // Create indexes of matching player names to waypoints to update the waypoints by index
                    HashMap<String, Integer> waypointNamesIndexes = new HashMap<>(playerWaypointList.size());
                    for (int i = 0; i < playerWaypointList.size(); i++) {
                        Waypoint waypoint = playerWaypointList.get(i);
                        waypointNamesIndexes.put(waypoint.getName(), i);
                    }

                    // Create indexes of matching player names to player client entities
                    // to get distances to each player in range by index
                    List<AbstractClientPlayer> playerClientEntityList = mc.level.players();
                    HashMap<String, Integer> playerClientEntityIndexes = new HashMap<>(playerWaypointList.size());
                    for (int i = 0; i < playerClientEntityList.size(); i++) {
                        AbstractClientPlayer playerClientEntity = playerClientEntityList.get(i);
                        playerClientEntityIndexes.put(playerClientEntity.getGameProfile().getName(), i);
                    }

                    // Keep track of which waypoints were previously shown
                    // to remove any that are not to be shown anymore
                    ArrayList<String> currentPlayerWaypointNames = new ArrayList<>();

                    // Add each player to the map
                    for (PlayerPosition playerPosition : playerPositions.values()) {
                        if (playerPosition == null) continue;
                        String playerName = playerPosition.player;

                        boolean isFriend = CommonModConfig.Instance.friendList().contains(playerName);

                        if (CommonModConfig.Instance.onlyShowFriendsWaypoints() && !isFriend) continue;

                        int minimumWaypointDistanceToUse;
                        int maximumWaypointDistanceToUse;
                        if (CommonModConfig.Instance.overwriteFriendDistances() && isFriend) {
                            minimumWaypointDistanceToUse = CommonModConfig.Instance.minFriendDistance();
                            maximumWaypointDistanceToUse = CommonModConfig.Instance.maxFriendDistance();
                        }
                        else {
                            minimumWaypointDistanceToUse = CommonModConfig.Instance.minDistance();
                            maximumWaypointDistanceToUse = CommonModConfig.Instance.maxDistance();
                        }

                        if (minimumWaypointDistanceToUse > maximumWaypointDistanceToUse)
                            maximumWaypointDistanceToUse = minimumWaypointDistanceToUse;

                        // If closer than the minimum waypoint distance or further away than the maximum waypoint distance,
                        // don't show waypoint
                        double d = camPosition.distanceTo(new Vec3(playerPosition.x, playerPosition.y, playerPosition.z));
                        if (d < minimumWaypointDistanceToUse || d > maximumWaypointDistanceToUse) continue;

                        // Check if this player is within the server's player entity tracking range
                        if (playerClientEntityIndexes.containsKey(playerName)) {
                            AbstractClientPlayer playerClientEntity = playerClientEntityList.get(playerClientEntityIndexes.get(playerName));

                            ClipContext clipContext = new ClipContext(mc.cameraEntity.position(), playerClientEntity.position(), ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.cameraEntity);
                            // If this player is visible, don't show waypoint
                            if (mc.level.clip(clipContext).getType() != HitResult.Type.BLOCK) {
                                continue;
                            }
                        }

                        // If a waypoint for this player already exists, update it
                        if (waypointNamesIndexes.containsKey(playerName)) {
                            Waypoint waypoint = playerWaypointList.get(waypointNamesIndexes.get(playerName));

                            waypoint.setX(playerPosition.x);
                            waypoint.setY(playerPosition.y);
                            waypoint.setZ(playerPosition.z);

                            currentPlayerWaypointNames.add(waypoint.getName());
                        }
                        // Otherwise, add a waypoint for the player
                        else {
                            try {
                                PlayerWaypoint currentPlayerWaypoint = new PlayerWaypoint(playerPosition);
                                playerWaypointList.add(currentPlayerWaypoint);
                                currentPlayerWaypointNames.add(currentPlayerWaypoint.getName());
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                    // Remove any waypoints for players not shown on the map anymore
                    playerWaypointList.removeIf(waypoint -> !currentPlayerWaypointNames.contains(waypoint.getName()));

                    int newPlayerWaypointColor = CommonModConfig.Instance.playerWaypointColor();
                    int newFriendWaypointColor = CommonModConfig.Instance.friendWaypointColor();
                    boolean newFriendColorOverride = CommonModConfig.Instance.overwriteFriendWaypointColor();
                    int newFriendListHashCode = CommonModConfig.Instance.friendList().hashCode();
                    if ((previousPlayerWaypointColor != newPlayerWaypointColor)
                            || (previousFriendWaypointColor != newFriendWaypointColor)
                            || (previousFriendColorOverride != newFriendColorOverride)
                            || (previousFriendListHashCode != newFriendListHashCode)) {
                        previousPlayerWaypointColor = newPlayerWaypointColor;
                        previousFriendWaypointColor = newFriendWaypointColor;
                        previousFriendColorOverride = newFriendColorOverride;
                        previousFriendListHashCode = newFriendListHashCode;
                        for (Waypoint waypoint : playerWaypointList){
                            waypoint.setColor(CommonModConfig.Instance.getPlayerWaypointColor(waypoint.getName()));
                        }
                    }
                }
                else {
                    playerWaypointList.clear();
                }
            }

            synchronized (markerWaypointList) {
                if (CommonModConfig.Instance.enableMarkerWaypoints()){
                    // Create indexes of matching marker names to waypoints to update the waypoints by index
                    HashMap<String, Integer> waypointNamesIndexes = new HashMap<>(markerWaypointList.size());
                    for (int i = 0; i < markerWaypointList.size(); i++) {
                        Waypoint waypoint = markerWaypointList.get(i);
                        waypointNamesIndexes.put(waypoint.getName(), i);
                    }

                    // Keep track of which waypoints were previously shown
                    // to remove any that are not to be shown anymore
                    ArrayList<String> currentMarkerWaypointNames = new ArrayList<>();

                    // Add each marker to the map
                    for (WaypointPosition markerPosition : markerPositions.values()) {
                        if (markerPosition == null) continue;
                        String markerName = markerPosition.name;

                        int minimumWaypointDistanceToUse = CommonModConfig.Instance.minDistanceMarker();
                        int maximumWaypointDistanceToUse = CommonModConfig.Instance.maxDistanceMarker();
                        if (minimumWaypointDistanceToUse > maximumWaypointDistanceToUse)
                            maximumWaypointDistanceToUse = minimumWaypointDistanceToUse;

                        // If closer than the minimum waypoint distance or further away than the maximum waypoint distance,
                        // don't show waypoint
                        double d = camPosition.distanceTo(new Vec3(markerPosition.x, markerPosition.y, markerPosition.z));
                        if (d < minimumWaypointDistanceToUse || d > maximumWaypointDistanceToUse) continue;

                        // If a waypoint for this marker already exists, update it
                        if (waypointNamesIndexes.containsKey(markerName)) {
                            Waypoint waypoint = markerWaypointList.get(waypointNamesIndexes.get(markerName));

                            waypoint.setX(markerPosition.x);
                            waypoint.setY(markerPosition.y);
                            waypoint.setZ(markerPosition.z);

                            currentMarkerWaypointNames.add(waypoint.getName());
                        }
                        // Otherwise, add a waypoint for the marker
                        else {
                            try {
                                FixedWaypoint currentMarkerWaypoint = new FixedWaypoint(markerPosition);
                                markerWaypointList.add(currentMarkerWaypoint);
                                currentMarkerWaypointNames.add(currentMarkerWaypoint.getName());
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                    // Remove any waypoints for markers not shown on the map anymore
                    markerWaypointList.removeIf(waypoint -> !currentMarkerWaypointNames.contains(waypoint.getName()));

                    int newMarkerWaypointColor = CommonModConfig.Instance.markerWaypointColor();
                    if (previousMarkerWaypointColor != newMarkerWaypointColor){
                        previousMarkerWaypointColor = newMarkerWaypointColor;
                        for (Waypoint waypoint : markerWaypointList){
                            waypoint.setColor(newMarkerWaypointColor);
                        }
                    }

                    if (!markerMessageWasShown && currentMarkerWaypointNames.size() > maxMarkerCountBeforeWarning && !CommonModConfig.Instance.ignoreMarkerMessage()) {
                        markerMessageWasShown = true;
                        Utils.sendToClientChat(Text.literal("[" + AbstractModInitializer.MOD_NAME + "]: " +
                                        "Looks like you have quite a lot of markers from the server visible! " +
                                        "Did you know that you can decrease their maximum distance or disable marker waypoints entirely? ")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
                                .append(Text.literal("[Don't show this again]")
                                        .withStyle(Style.EMPTY.withClickEvent(
                                                        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + AbstractModInitializer.MOD_ID + " ignore_marker_message"))
                                                .withColor(ChatFormatting.GREEN).withBold(true))));
                    }
                }
                else {
                    markerWaypointList.clear();
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
        cantGetMarkerPositionsErrorWasShown = false;
        linkBrokenErrorWasShown = false;
        markerMessageWasShown = false;
    }
}