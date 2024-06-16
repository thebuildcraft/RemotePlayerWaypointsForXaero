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

package de.the_build_craft.remote_player_waypoints_for_xaero.neoforge;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.neoforged.fml.ModLoadingContext;
#if MC_VER < MC_1_20_6
import net.neoforged.neoforge.client.ConfigScreenHandler;
#else
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
#endif

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leander Knüttel
 * @version 16.06.2024
 */
public class CommonModConfigNeoForge extends CommonModConfig {
    public CommonModConfigNeoForge(){
        super();
        AutoConfig.register(ModConfig.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));

        #if MC_VER < MC_1_20_6
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
            return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
        }));
		#else
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (client, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get());
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
    public boolean debugMode() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.debugMode;
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
            seN.add(new ServerEntry(s.ip, s.link, ServerEntry.Maptype.valueOf(s.maptype.toString())));
        }
        return seN;
    }
}
