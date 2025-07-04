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

package de.the_build_craft.remote_player_waypoints_for_xaero.common.connections;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.PlayerPosition;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.WaypointPosition;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Text;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 21.04.2025
 */
public abstract class MapConnection {
    public URL queryURL;
    public final Minecraft mc;
    public String currentDimension = "";
    public String onlineMapConfigLink;
    long lastUpdateTimeMs;
    public boolean foundPlayer;

    public MapConnection() {
        this.mc = Minecraft.getInstance();
        lastUpdateTimeMs = System.currentTimeMillis();
    }

    public void setCurrentDimension(String currentDimension) {
        this.currentDimension = currentDimension;
    }

    @NotNull
    protected String getBaseURL(CommonModConfig.ServerEntry serverEntry, boolean useHttps) {
        String baseURL = serverEntry.link;
        if (!baseURL.startsWith(useHttps ? "https://" : "http://")){
            baseURL = (useHttps ? "https://" : "http://") + baseURL;
        }

        int i = baseURL.indexOf("?");
        if (i != -1){
            baseURL = baseURL.substring(0, i - 1);
        }

        i = baseURL.indexOf("#");
        if (i != -1){
            baseURL = baseURL.substring(0, i - 1);
        }

        if (baseURL.endsWith("index.html")){
            baseURL = baseURL.substring(0, baseURL.length() - 10);
        }

        if (baseURL.endsWith("/")){
            baseURL = baseURL.substring(0, baseURL.length() - 1);
        }
        return baseURL.replace(" ", "%20");
    }

    public abstract HashMap<String, PlayerPosition> getPlayerPositions() throws IOException;

    public HashMap<String, PlayerPosition> HandlePlayerPositions(PlayerPosition[] playerPositions){
        HashMap<String, PlayerPosition> newPlayerPositions = new HashMap<>();
        if (mc.player == null) {
            return newPlayerPositions;
        }
        String clientName = mc.player.getName().getString();
        foundPlayer = false;
        if (!AbstractModInitializer.overwriteCurrentDimension) {
            currentDimension = "";
            for (PlayerPosition p : playerPositions){
                if (Objects.equals(p.player, clientName)) {
                    currentDimension = p.world;
                    foundPlayer = true;
                }
            }
        }

        if (CommonModConfig.Instance.debugMode() && CommonModConfig.Instance.chatLogInDebugMode()) {
            Utils.sendToClientChat("---");
        }
        for (PlayerPosition p : playerPositions) {
            UpdateAfkInfo(p);

            if (CommonModConfig.Instance.debugMode() || (Objects.equals(p.world, currentDimension) && !Objects.equals(p.player, clientName))) {
                newPlayerPositions.put(p.player, p);
            }
        }
        lastUpdateTimeMs = System.currentTimeMillis();

        return newPlayerPositions;
    }

    public void UpdateAfkInfo(PlayerPosition playerPosition){
        if (AbstractModInitializer.lastPlayerDataDic.containsKey(playerPosition.player)) {
            if (AbstractModInitializer.lastPlayerDataDic.get(playerPosition.player).CompareCords(playerPosition)) {
                if (AbstractModInitializer.AfkTimeDic.containsKey(playerPosition.player)) {
                    AbstractModInitializer.AfkTimeDic.put(playerPosition.player, AbstractModInitializer.AfkTimeDic.get(playerPosition.player) + (System.currentTimeMillis() - lastUpdateTimeMs));
                } else {
                    AbstractModInitializer.AfkTimeDic.put(playerPosition.player, (System.currentTimeMillis() - lastUpdateTimeMs));
                }
                if (CommonModConfig.Instance.debugMode() && CommonModConfig.Instance.chatLogInDebugMode()) {
                    Utils.sendToClientChat(playerPosition.player + "  afk_time: " + AbstractModInitializer.AfkTimeDic.get(playerPosition.player) / 1000);
                }
                if (AbstractModInitializer.AfkTimeDic.get(playerPosition.player) / 1000 >= CommonModConfig.Instance.timeUntilAfk()) {
                    AbstractModInitializer.AfkDic.put(playerPosition.player, true);
                }
            } else {
                AbstractModInitializer.AfkTimeDic.put(playerPosition.player, 0L);
                AbstractModInitializer.AfkDic.put(playerPosition.player, false);
            }
        }
        AbstractModInitializer.lastPlayerDataDic.put(playerPosition.player, playerPosition);
    }

    public abstract HashMap<String, WaypointPosition> getWaypointPositions() throws IOException;

    public void OpenOnlineMapConfig(){
        #if MC_VER < MC_1_21_5
        Utils.sendToClientChat(Text.literal(onlineMapConfigLink).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, onlineMapConfigLink))));
        #else
        Utils.sendToClientChat(Text.literal(onlineMapConfigLink).withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(URI.create(onlineMapConfigLink)))));
        #endif
    }

    public abstract HashSet<String> getMarkerLayers();
}
