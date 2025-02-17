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

package de.the_build_craft.remote_player_waypoints_for_xaero.forge;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
#if MC_VER < MC_1_17_1
import net.minecraftforge.fml.ExtensionPoint;
#elif MC_VER == MC_1_17_1
import net.minecraftforge.fmlclient.ConfigGuiHandler;
#elif MC_VER >= MC_1_18_2 && MC_VER < MC_1_19_2
import net.minecraftforge.client.ConfigGuiHandler;
#else
import net.minecraftforge.client.ConfigScreenHandler;
#endif

import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Leander Knüttel
 * @version 17.02.2025
 */
public class CommonModConfigForge extends CommonModConfig {
    public CommonModConfigForge(){
        super();
        AutoConfig.register(ModConfig.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));

        #if MC_VER < MC_1_17_1
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> {
            return AutoConfig.getConfigScreen(ModConfig.class, screen).get();
        });
        #elif MC_VER >= MC_1_17_1 && MC_VER < MC_1_19_2
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((mc, screen) -> AutoConfig.getConfigScreen(ModConfig.class, screen).get()));
        #else
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
            return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
        }));
        #endif
    }

    @Override
    public void saveConfig(){
        AutoConfig.getConfigHolder(ModConfig.class).save();
    }

    @Override
    public boolean enabled() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.enabled;
    }

    @Override
    public boolean enablePlayerWaypoints() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.enablePlayerWaypoints;
    }

    @Override
    public boolean enableMarkerWaypoints() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.enableMarkerWaypoints;
    }

    @Override
    public boolean enableEntityRadar() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.enablePlayerIcons;
    }

    @Override
    public WaypointRenderBelowMode minimapWaypointsRenderBelow() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.minimapWaypointsRenderBelow;
    }

    @Override
    public int updateDelay() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.updateDelay;
    }

    @Override
    public int minDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.minDistance;
    }

    @Override
    public int maxDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.maxDistance;
    }

    @Override
    public int maxIconDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.maxIconDistance;
    }

    @Override
    public int minDistanceMarker() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.minDistanceMarker;
    }

    @Override
    public int maxDistanceMarker() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.maxDistanceMarker;
    }

    @Override
    public int defaultY() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.defaultY;
    }

    @Override
    public int timeUntilAfk() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.timeUntilAfk;
    }

    @Override
    public int unknownAfkStateColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.unknownAfkStateColor;
    }

    @Override
    public int AfkColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.AfkColor;
    }

    @Override
    public int playerWaypointColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.playerWaypointColor.ordinal();
    }

    @Override
    public int markerWaypointColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.markerWaypointColor.ordinal();
    }

    @Override
    public boolean showAfkTimeInTabList() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.showAfkTimeInTabList;
    }

    @Override
    public boolean showAfkInTabList() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.showAfkInTabList;
    }

    @Override
    public boolean hideAfkMinutes() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.hideAfkMinutes;
    }

    @Override
    public boolean debugMode() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.debugMode;
    }

    @Override
    public boolean chatLogInDebugMode() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.chatLogInDebugMode;
    }

    @Override
    public List<String> ignoredServers() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.ignoredServers;
    }

    @Override
    public List<ServerEntry> serverEntries() {
        List<ModConfig.ServerEntry> se = AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.serverEntries;
        ArrayList<ServerEntry> seN = new ArrayList<ServerEntry>();
        for (ModConfig.ServerEntry s: se){
            seN.add(new ServerEntry(s.ip,
                    s.link,
                    ServerEntry.Maptype.valueOf(s.maptype.toString()),
                    ServerEntry.MarkerVisibilityMode.valueOf(s.markerVisibilityMode.toString()),
                    s.markerLayers));
        }
        return seN;
    }

    @Override
    public void setMarkerLayers(String ip, List<String> layers) {
        List<ModConfig.ServerEntry> entries = AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.serverEntries;
        for (ModConfig.ServerEntry entry : entries) {
            if (entry.ip.toLowerCase(Locale.ROOT).equals(ip.toLowerCase(Locale.ROOT))) {
                entry.markerLayers = layers;
                entry.markerVisibilityMode = ModConfig.ServerEntry.MarkerVisibilityMode.All;
                saveConfig();
                return;
            }
        }
    }

    @Override
    public void setIgnoreMarkerMessage(boolean on) {
        AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.ignoreMarkerMessage = on;
        saveConfig();
    }

    @Override
    public boolean ignoreMarkerMessage() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.ignoreMarkerMessage;
    }

    @Override
    public List<String> friendList() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.friendList;
    }

    @Override
    public boolean onlyShowFriendsWaypoints() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.onlyShowFriendsWaypoints;
    }

    @Override
    public boolean onlyShowFriendsIcons() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.onlyShowFriendsIcons;
    }

    @Override
    public boolean overwriteFriendDistances() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.overwriteFriendDistances;
    }

    @Override
    public int minFriendDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.minFriendDistance;
    }

    @Override
    public int maxFriendDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.maxFriendDistance;
    }

    @Override
    public int maxFriendIconDistance() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.maxFriendIconDistance;
    }

    @Override
    public boolean overwriteFriendWaypointColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.overwriteFriendWaypointColor;
    }

    @Override
    public int friendWaypointColor() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().friends.friendWaypointColor.ordinal();
    }
}
