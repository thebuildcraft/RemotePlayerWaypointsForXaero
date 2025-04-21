/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
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

import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Text;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Leander Knüttel
 * @version 21.04.2025
 */
public abstract class CommonModConfig {
    public CommonModConfig() {
        Instance = this;
    }
    public static CommonModConfig Instance;
    public abstract void saveConfig();

    public abstract boolean enabled();
    public abstract boolean enablePlayerWaypoints();
    public abstract boolean enableMarkerWaypoints();
    public abstract boolean enableEntityRadar();
    public abstract WaypointRenderBelowMode minimapWaypointsRenderBelow();
    public abstract int updateDelay();
    public abstract int minDistance();
    public abstract int maxDistance();
    public abstract int maxIconDistance();
    public abstract int minDistanceMarker();
    public abstract int maxDistanceMarker();
    public abstract int defaultY();
    public abstract int timeUntilAfk();
    public abstract int unknownAfkStateColor();
    public abstract int AfkColor();
    public abstract int playerWaypointColor();
    public abstract int markerWaypointColor();
    public abstract boolean showAfkTimeInTabList();
    public abstract boolean showAfkInTabList();
    public abstract boolean hideAfkMinutes();
    public abstract boolean debugMode();
    public abstract boolean chatLogInDebugMode();
    public abstract List<String> ignoredServers();
    public abstract List<ServerEntry> serverEntries();
    public abstract void setMarkerLayers(String ip, List<String> layers);
    public abstract void setIgnoreMarkerMessage(boolean on);
    public abstract boolean ignoreMarkerMessage();

    public abstract List<String> friendList();
    public abstract boolean onlyShowFriendsWaypoints();
    public abstract boolean onlyShowFriendsIcons();
    public abstract boolean overwriteFriendDistances();
    public abstract int minFriendDistance();
    public abstract int maxFriendDistance();
    public abstract int maxFriendIconDistance();
    public abstract boolean overwriteFriendWaypointColor();
    public abstract int friendWaypointColor();

    public int getPlayerWaypointColor(String playerName) {
        if (overwriteFriendWaypointColor() && friendList().contains(playerName)){
            return friendWaypointColor();
        }
        else {
            return playerWaypointColor();
        }
    }

    public ServerEntry getCurrentServerEntry() {
        String serverIP = Objects.requireNonNull(Minecraft.getInstance().getCurrentServer()).ip.toLowerCase(Locale.ROOT);
        ServerEntry serverEntry = null;
        for (ServerEntry server : serverEntries()){
            if (Objects.equals(serverIP, server.ip.toLowerCase(Locale.ROOT))){
                serverEntry = server;
            }
        }
        return serverEntry;
    }

    public int getWaypointLayerOrder() {
        WaypointRenderBelowMode waypointRenderBelowMode = minimapWaypointsRenderBelow();
        boolean playerListDown = Minecraft.getInstance().options.keyPlayerList.isDown();

        if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.ALWAYS) {
            return -1;
        } else if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.WHEN_PLAYER_LIST_SHOWN) {
            if (playerListDown) return -1;
        } else if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.WHEN_PLAYER_LIST_HIDDEN) {
            if (!playerListDown) return -1;
        }
        return 100;
    }

    public static class ServerEntry {
        public Maptype maptype;
        public String ip;
        public String link;
        public MarkerVisibilityMode markerVisibilityMode;
        public List<String> markerLayers;

        public ServerEntry() {
            this("", "", Maptype.Dynmap, MarkerVisibilityMode.Auto, new ArrayList<>());
        }

        public ServerEntry(String ip, String link, Maptype maptype, MarkerVisibilityMode markerVisibilityMode, List<String> markerLayers) {
            this.ip = ip;
            this.link = link;
            this.maptype = maptype;
            this.markerVisibilityMode = markerVisibilityMode;
            this.markerLayers = markerLayers;
        }

        public enum Maptype {
            Dynmap,
            Squaremap,
            Bluemap,
            Pl3xMap,
            LiveAtlas;

            Maptype() {
            }
        }

        public enum MarkerVisibilityMode {
            Auto,
            All,
            None,
            BlackList,
            WhiteList;

            MarkerVisibilityMode() {
            }
        }

        public boolean includeMarkerLayer(String layer) {
            switch (markerVisibilityMode) {
                case Auto:
                case All:
                    return true;
                case None:
                    return false;
                case BlackList:
                    return !markerLayers.contains(layer);
                case WhiteList:
                    return markerLayers.contains(layer);
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public enum WaypointColor {
        Black,
        DarkBlue,
        DarkGreen,
        DarkAqua,
        DarkRed,
        DarkPurple,
        Gold,
        Gray,
        DarkGray,
        Blue,
        Green,
        Aqua,
        Red,
        LightPurple,
        Yellow,
        White;

        WaypointColor(){
        }

        @Override
        public String toString() {
            return Text.translatable("WaypointColor." + this.name()).getString();
        }
    }

    public enum WaypointRenderBelowMode {
        NEVER,
        ALWAYS,
        WHEN_PLAYER_LIST_SHOWN,
        WHEN_PLAYER_LIST_HIDDEN;

        WaypointRenderBelowMode(){
        }

        @Override
        public String toString() {
            return Text.translatable("text.autoconfig.remote_player_waypoints_for_xaero.option.general.minimapWaypointsRenderBelow." + this.name()).getString();
        }
    }
}
