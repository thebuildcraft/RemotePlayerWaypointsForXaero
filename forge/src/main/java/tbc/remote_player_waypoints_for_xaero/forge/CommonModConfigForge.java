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


package tbc.remote_player_waypoints_for_xaero.forge;

import de.the_build_craft.remote_player_waypoints_for_xaero.CommonModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;

public class CommonModConfigForge extends CommonModConfig {
    public CommonModConfigForge(){
        super();
        AutoConfig.register(ModConfig.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
            return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
        }));
    }

    @Override
    public boolean enabled() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.enabled;
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
        var se = AutoConfig.getConfigHolder(ModConfig.class).getConfig().general.serverEntries;
        var seN = new ArrayList<ServerEntry>();
        for (var s: se){
            seN.add(new ServerEntry(s.ip, s.link, ServerEntry.Maptype.valueOf(s.maptype.toString())));
        }
        return seN;
    }
}
