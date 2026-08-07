/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2024 - 2026  Leander Knüttel and contributors
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

package de.the_build_craft.maplink.common.connections;

import de.the_build_craft.maplink.common.HTTP;
import de.the_build_craft.maplink.common.ModConfig;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.waypoints.PlayerPosition;
import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.wrappers.Text;
import de.the_build_craft.maplink.common.wrappers.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.the_build_craft.maplink.common.AbstractModInitializer.LOGGER;
import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @author eatmyvenom
 * @version 06.08.2026
 */
public abstract class MapConnection {
    public final ModConfig.ServerEntry serverEntry;
    public URL queryURL;
    public final Minecraft mc;
    public String currentDimension = "";
    public String onlineMapConfigLink;
    public boolean foundPlayer;
    public boolean partOfLiveAtlas;
    private boolean firstPlayerUpdate = true;
    private static final Pattern BASE_URL_PATTERN = Pattern.compile("https?://[^/?#]+(/(?!index\\.html)[^/?#]+)*");
    protected boolean autoUpdateDimensionMappings = true;

    public MapConnection(ModConfig.ServerEntry serverEntry) {
        this.mc = Minecraft.getInstance();
        this.serverEntry = serverEntry;
    }

    public void setCurrentDimension(String currentDimension) {
        this.currentDimension = currentDimension;
    }

    @NotNull
    protected String getBaseURL(ModConfig.ServerEntry serverEntry, boolean useHttps) throws IOException {
        String baseURL = serverEntry.link.trim();

        if (!baseURL.startsWith(useHttps ? "https://" : "http://")){
            baseURL = (useHttps ? "https://" : "http://") + baseURL;
        }

        Matcher matcher = BASE_URL_PATTERN.matcher(baseURL);
        if (!matcher.find()) throw new RuntimeException("wrong url pattern: " + baseURL);
        baseURL = HTTP.checkForRedirects(matcher.group());
        matcher = BASE_URL_PATTERN.matcher(baseURL);
        if (!matcher.find()) throw new RuntimeException("wrong url pattern after redirect check: " + baseURL);
        baseURL = matcher.group();

        LOGGER.info("baseURL: {}", baseURL);

        return baseURL.replace(" ", "%20");
    }

    public abstract HashMap<String, PlayerPosition> getPlayerPositions() throws IOException;

    public HashMap<String, PlayerPosition> HandlePlayerPositions(PlayerPosition[] playerPositions) {
        return HandlePlayerPositions(playerPositions, "");
    }

    public HashMap<String, PlayerPosition> HandlePlayerPositions(PlayerPosition[] playerPositions, String worldOverwrite) {
        HashMap<String, PlayerPosition> newPlayerPositions = new HashMap<>();
        if (mc.player == null) {
            return newPlayerPositions;
        }
        String clientName = mc.player.getName().getString();
        foundPlayer = false;
        if (!AbstractModInitializer.overwriteCurrentDimension) {
            currentDimension = worldOverwrite;
            for (PlayerPosition p : playerPositions){
                if (Objects.equals(p.name, clientName)) {
                    currentDimension = p.world;
                    foundPlayer = true;
                    break;
                }
            }
        }//                     if sb is invisible on Dynmap
        if (currentDimension.equals("-some-other-bogus-world-")) currentDimension = "";

        if (autoUpdateDimensionMappings && !AbstractModInitializer.overwriteCurrentDimension) {
            #if MC_VER >= MC_1_21_11
            String clientDimension = mc.level.dimension().identifier().toString();
            #else
            String clientDimension = mc.level.dimension().location().toString();
            #endif
            if (currentDimension.isEmpty()) {
                String[] mappedDimension = serverEntry.dimensionMapping.get(clientDimension);
                if (config.general.invisibilityRecovery && mappedDimension != null && mappedDimension.length > 0) {
                    currentDimension = mappedDimension[0];
                    foundPlayer = true;
                }
            } else {
                serverEntry.addDimensionMapping(clientDimension, new String[]{currentDimension});
            }
        }

        if (config.general.debugMode && config.general.chatLogInDebugMode) {
            Utils.sendToClientChat("---");
        }

        if (!firstPlayerUpdate) {
            for (Map.Entry<String, Long> lastUpdateEntry : AbstractModInitializer.lastPlayerUpdateTimeMap.entrySet()) {
                if ((System.currentTimeMillis() - lastUpdateEntry.getValue()) / 1000
                        >= (Math.max(30, config.general.timeUntilAfk) - 5)) {
                    String playerName = lastUpdateEntry.getKey();
                    AbstractModInitializer.lastPlayerPosMap.remove(playerName);
                    AbstractModInitializer.playerOverAfkTimeMap.remove(playerName);
                    AbstractModInitializer.AfkMap.remove(playerName);
                }
            }
        }

        for (PlayerPosition p : playerPositions) {
            UpdateAfkInfo(p, clientName);

            if (config.general.debugMode || (Objects.equals(p.world, currentDimension) && !Objects.equals(p.name, clientName))) {
                newPlayerPositions.put(p.name, p);
            }
        }

        firstPlayerUpdate = false;

        return newPlayerPositions;
    }

    public void UpdateAfkInfo(PlayerPosition playerPosition, String clientName) {
        AbstractModInitializer.lastPlayerUpdateTimeMap.put(playerPosition.name, System.currentTimeMillis());

        if (AbstractModInitializer.lastPlayerPosMap.containsKey(playerPosition.name)) {
            if (AbstractModInitializer.lastPlayerPosMap.get(playerPosition.name).roughlyEqual(playerPosition.pos)) {
                if (config.general.debugMode && config.general.chatLogInDebugMode) {
                    Utils.sendToClientChat(playerPosition.name + "  afk_time: "
                            + (System.currentTimeMillis() - AbstractModInitializer.lastPlayerActivityTimeMap.get(playerPosition.name)) / 1000);
                }
                if ((System.currentTimeMillis() - AbstractModInitializer.lastPlayerActivityTimeMap.get(playerPosition.name)) / 1000
                        >= Math.max(30, config.general.timeUntilAfk)) {
                    AbstractModInitializer.AfkMap.put(playerPosition.name, true);
                }
            } else {
                AbstractModInitializer.AfkMap.put(playerPosition.name, false);
                AbstractModInitializer.lastPlayerActivityTimeMap.put(playerPosition.name, System.currentTimeMillis());
                AbstractModInitializer.lastPlayerPosMap.put(playerPosition.name, playerPosition.pos);
                AbstractModInitializer.playerOverAfkTimeMap.put(playerPosition.name, false);
            }
        } else {
            AbstractModInitializer.lastPlayerActivityTimeMap.put(playerPosition.name, System.currentTimeMillis());
            AbstractModInitializer.lastPlayerPosMap.put(playerPosition.name, playerPosition.pos);
            if (firstPlayerUpdate) {
                AbstractModInitializer.playerOverAfkTimeMap.put(playerPosition.name, !playerPosition.name.equals(clientName));
            } else {
                AbstractModInitializer.playerOverAfkTimeMap.put(playerPosition.name, false);
                AbstractModInitializer.AfkMap.put(playerPosition.name, false);
            }
        }
    }

    public abstract void getWaypointPositions(boolean forceRefresh) throws IOException;

    public void OpenOnlineMapConfig() {
        #if MC_VER < MC_1_21_5
        Utils.sendToClientChat(Text.literal(onlineMapConfigLink).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, onlineMapConfigLink))));
        #else
        Utils.sendToClientChat(Text.literal(onlineMapConfigLink).withStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(URI.create(onlineMapConfigLink)))));
        #endif
    }

    public abstract Set<String> getMarkerLayers();
    public abstract List<String[]> getPossibleTileMaps();
    public abstract boolean downloadTiles(String map, AreaSelection areaSelection);
}
