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

package de.the_build_craft.remote_player_waypoints_for_xaero.fabric;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leander Knüttel
 * @version 16.06.2024
 */
@Config(name = "remote_player_waypoints_for_xaero")
#if MC_VER < MC_1_20_6
@Config.Gui.Background("minecraft:textures/block/acacia_planks.png")
@Config.Gui.CategoryBackground(
        category = "b",
        background = "minecraft:textures/block/stone.png"
)
#endif
public class ModConfig extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("a")
    @ConfigEntry.Gui.TransitiveObject
    public ModuleA general = new ModuleA();

    public ModConfig() {
    }

    @Config(
            name = "general"
    )
    public static class ModuleA implements ConfigData {
        public boolean enabled = true;
        public boolean enablePlayerWaypoints = true;
        public boolean enableMarkerWaypoints = true;
        @Comment("in ms")
        @ConfigEntry.BoundedDiscrete(min = 2000, max = 10000)
        public int updateDelay = 2000;
        @Comment("in m")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        int minDistance = 0;
        @Comment("in m")
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxDistance = 100000;
        @Comment("in m")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        int minDistanceMarker = 0;
        @Comment("in m")
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxDistanceMarker = 100000;
        public List<ServerEntry> serverEntries = new ArrayList<>();
        @Comment("default Y coordinate for maps that don't provide Y coordinates")
        @ConfigEntry.BoundedDiscrete(min = -100, max = 400)
        public int defaultY = 64;
        @Comment("in sec")
        @ConfigEntry.BoundedDiscrete(min = 60, max = 600)
        public int timeUntilAfk = 120;
        @ConfigEntry.ColorPicker
        public int unknownAfkStateColor = 0x606060;
        @ConfigEntry.ColorPicker
        public int AfkColor = 0xFF5500;
        @ConfigEntry.Gui.EnumHandler(
                option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON
        )
        public CommonModConfig.WaypointColor playerWaypointColor = CommonModConfig.WaypointColor.Black;
        @ConfigEntry.Gui.EnumHandler(
                option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON
        )
        public CommonModConfig.WaypointColor markerWaypointColor = CommonModConfig.WaypointColor.Gray;
        public boolean showAfkTimeInTabList = true;
        public boolean debugMode = false;

        public List<String> ignoredServers = new ArrayList<>();

        public ModuleA() {
        }
    }

    public static class ServerEntry {
        @ConfigEntry.Gui.EnumHandler(
                option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON
        )
        public Maptype maptype;
        public String ip;
        public String link;

        public ServerEntry() {
            this("", "", Maptype.Dynmap);
        }

        public ServerEntry(String ip, String link, Maptype maptype) {
            this.ip = ip;
            this.link = link;
            this.maptype = maptype;
        }

        public enum Maptype {
            Dynmap,
            Squaremap,
            Bluemap,
            Pl3xMap;

            Maptype() {
            }
        }
    }
}