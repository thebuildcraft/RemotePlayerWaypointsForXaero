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
import de.the_build_craft.remote_player_waypoints_for_xaero.common.UpdateTask;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.WaypointPosition;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 14.06.2024
 */
public abstract class MapConnection {
    public URL queryURL;
    public final Minecraft mc;
    protected String currentDimension;

    public MapConnection(CommonModConfig.ServerEntry serverEntry, UpdateTask updateTask) {
        this.mc = Minecraft.getInstance();
    }

    @NotNull
    protected String getBaseURL(CommonModConfig.ServerEntry serverEntry, boolean useHttps) {
        var baseURL = serverEntry.link.toLowerCase(Locale.ROOT);
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
        return baseURL;
    }

    public abstract PlayerPosition[] getPlayerPositions() throws IOException;

    public PlayerPosition[] HandlePlayerPositions(PlayerPosition[] playerPositions){
        if (mc.player == null) {
            return new PlayerPosition[0];
        }
        String clientName = mc.player.getName().getString();
        currentDimension = "";
        for (var p : playerPositions){
            if (Objects.equals(p.player, clientName)) {
                currentDimension = p.world;
            }
        }

        for (int i = 0; i < playerPositions.length; i++) {
            UpdateAfkInfo(playerPositions[i]);

            if (!CommonModConfig.Instance.debugMode() && (!Objects.equals(playerPositions[i].world, currentDimension) || Objects.equals(playerPositions[i].player, clientName))) {
                playerPositions[i] = null;
            }
        }

        return playerPositions;
    }

    public void UpdateAfkInfo(PlayerPosition playerPosition){
        if (AbstractModInitializer.lastPlayerDataDic.containsKey(playerPosition.player)) {
            if (AbstractModInitializer.lastPlayerDataDic.get(playerPosition.player).CompareCords(playerPosition)) {
                if (AbstractModInitializer.AfkTimeDic.containsKey(playerPosition.player)) {
                    AbstractModInitializer.AfkTimeDic.put(playerPosition.player, AbstractModInitializer.AfkTimeDic.get(playerPosition.player) + (CommonModConfig.Instance.updateDelay() / 1000));
                } else {
                    AbstractModInitializer.AfkTimeDic.put(playerPosition.player, (CommonModConfig.Instance.updateDelay() / 1000));
                }
                if (CommonModConfig.Instance.debugMode()) {
                    Utils.sendToClientChat(playerPosition.player + "  afk_time: " + AbstractModInitializer.AfkTimeDic.get(playerPosition.player));
                }
                if (AbstractModInitializer.AfkTimeDic.get(playerPosition.player) >= CommonModConfig.Instance.timeUntilAfk()) {
                    AbstractModInitializer.AfkDic.put(playerPosition.player, true);
                }
            } else {
                AbstractModInitializer.AfkTimeDic.put(playerPosition.player, 0);
                AbstractModInitializer.AfkDic.put(playerPosition.player, false);
            }
        }
        AbstractModInitializer.lastPlayerDataDic.put(playerPosition.player, playerPosition);
    }

    public abstract WaypointPosition[] getWaypointPositions() throws IOException;
}