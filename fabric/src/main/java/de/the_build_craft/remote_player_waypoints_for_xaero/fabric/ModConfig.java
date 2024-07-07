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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leander Knüttel
 * @version 07.07.2024
 */
@Config(name = "remote_player_waypoints_for_xaero")
#if MC_VER < MC_1_20_6
@Config.Gui.Background("minecraft:textures/block/acacia_planks.png")
@Config.Gui.CategoryBackground(
        category = "b",
        background = "minecraft:textures/block/oak_planks.png"
)
#endif
public class ModConfig extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("a")
    @ConfigEntry.Gui.TransitiveObject
    public ModuleA general = new ModuleA();

    @ConfigEntry.Category("b")
    @ConfigEntry.Gui.TransitiveObject
    public ModuleB friends = new ModuleB();

    public ModConfig() {
    }

    @Config(name = "general")
    public static class ModuleA implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 2000, max = 10000)
        public int updateDelay = 2000;

        @ConfigEntry.Gui.Tooltip
        public List<ServerEntry> serverEntries = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = -100, max = 400)
        public int defaultY = 64;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CommonModConfig.WaypointRenderBelowMode minimapWaypointsRenderBelow = CommonModConfig.WaypointRenderBelowMode.WHEN_PLAYER_LIST_SHOWN;

        //Player options
        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public boolean enablePlayerWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean enablePlayerIcons = true;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        int minDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxIconDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 60, max = 600)
        public int timeUntilAfk = 120;

        @ConfigEntry.Gui.Tooltip()
        public boolean showAfkTimeInTabList = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int unknownAfkStateColor = 0x606060;

        @ConfigEntry.ColorPicker
        public int AfkColor = 0xFF5500;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CommonModConfig.WaypointColor playerWaypointColor = CommonModConfig.WaypointColor.Black;

        //Marker options
        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public boolean enableMarkerWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        int minDistanceMarker = 0;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxDistanceMarker = 100000;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CommonModConfig.WaypointColor markerWaypointColor = CommonModConfig.WaypointColor.Gray;

        //auto handled options
        @ConfigEntry.Gui.PrefixText
        public List<String> ignoredServers = new ArrayList<>();

        public boolean ignoreMarkerMessage = false;

        //dev options
        @ConfigEntry.Gui.PrefixText
        public boolean debugMode = false;

        public ModuleA() {
        }
    }

    @Config(name = "friends")
    public static class ModuleB implements ConfigData {
        @ConfigEntry.Gui.Tooltip()
        public List<String> friendList = new ArrayList<>();

        public boolean onlyShowFriendsWaypoints = false;

        public boolean onlyShowFriendsIcons = false;

        public boolean overwriteFriendDistances = false;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 0, max = 20)
        int minFriendDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxFriendDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 100, max = 100000)
        int maxFriendIconDistance = 100000;

        public boolean overwriteFriendWaypointColor = false;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CommonModConfig.WaypointColor friendWaypointColor = CommonModConfig.WaypointColor.Black;

        public ModuleB() {
        }
    }

    public static class ServerEntry {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
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