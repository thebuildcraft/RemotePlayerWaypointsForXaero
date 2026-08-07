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

package de.the_build_craft.maplink.common;

import de.the_build_craft.maplink.common.wrappers.Text;
import de.the_build_craft.maplink.common.wrappers.Utils;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.*;

import static de.the_build_craft.maplink.common.CommonModConfig.config;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Config(name = "maplink")
#if MC_VER < MC_1_20_6
@Config.Gui.Background("minecraft:textures/block/acacia_planks.png")
@Config.Gui.CategoryBackground(
        category = "friends",
        background = "minecraft:textures/block/oak_planks.png"
)
@Config.Gui.CategoryBackground(
        category = "hud",
        background = "minecraft:textures/block/birch_planks.png"
)
@Config.Gui.CategoryBackground(
        category = "miniMap",
        background = "minecraft:textures/block/dark_oak_planks.png"
)
@Config.Gui.CategoryBackground(
        category = "worldMap",
        background = "minecraft:textures/block/spruce_planks.png"
)
#endif
public class ModConfig extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public General general = new General();

    @ConfigEntry.Category("friends")
    @ConfigEntry.Gui.TransitiveObject
    public Friends friends = new Friends();

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.TransitiveObject
    public HudModule hud = new HudModule();

    @ConfigEntry.Category("minimap")
    @ConfigEntry.Gui.TransitiveObject
    public MiniMapModule minimap = new MiniMapModule();

    @ConfigEntry.Category("worldmap")
    @ConfigEntry.Gui.TransitiveObject
    public WorldMapModule worldmap = new WorldMapModule();

    public ModConfig() {
    }

    @Config(name = "general")
    public static class General implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public List<ServerEntry> serverEntries = new ArrayList<>();

        @ConfigEntry.Gui.Tooltip
        public int maxUpdateDelay = 2000;

        @ConfigEntry.Gui.Tooltip()
        public int defaultY = 64;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode minimapWaypointsRenderBelow = ConditionalActiveMode.WHEN_PLAYER_LIST_SHOWN;

        //Player options
        @ConfigEntry.Gui.PrefixText
        public boolean enablePlayerWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean showPlayerWaypointsAsTrackedPlayers = true;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int interpolationTime = 100;

        @ConfigEntry.Gui.Tooltip()
        public boolean enablePlayerIconWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean useMcHeadsPlayerNameIcons = false;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public WaypointColor playerWaypointColor = WaypointColor.Black;

        //AFK options
        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public boolean showAfkInTabList = true;

        @ConfigEntry.Gui.Tooltip()
        public int timeUntilAfk = 120;

        @ConfigEntry.Gui.Tooltip()
        public boolean showAfkTimeInTabList = true;

        public boolean hideAfkMinutes = false;

        @ConfigEntry.ColorPicker
        public int AfkColor = 0xFF5500;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.ColorPicker
        public int unknownAfkStateColor = 0x606060;

        //Marker options
        @ConfigEntry.Gui.PrefixText
        public boolean enableMarkerWaypoints = true;

        public boolean enableMarkerIcons = true;

        @ConfigEntry.Gui.Tooltip
        public boolean showDefaultMarkerIcons = true;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public WaypointColor markerWaypointColor = WaypointColor.Gray;

        //Area Marker options
        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip
        public boolean enableAreaMarkerOverlay = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
        public int areaFillAlphaMul = 100;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int areaFillAlphaMin = 0;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int areaFillAlphaMax = 42;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 200)
        public int areaLineAlphaMul = 100;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int areaLineAlphaMin = 0;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int areaLineAlphaMax = 100;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
        public int blocksPerChunkThreshold = 128;

        @ConfigEntry.Gui.Tooltip
        public int maxChunkArea = 500_000;

        public boolean excludeOPAC = true;

        //misc options
        @ConfigEntry.Gui.PrefixText
        public List<String> ignoredServers = new ArrayList<>();

        public boolean ignoreMarkerMessage = false;

        public boolean hideAllChatErrors = false;

        @ConfigEntry.Gui.Tooltip
        public boolean invisibilityRecovery = true;

        //dev options
        @ConfigEntry.Gui.PrefixText
        public boolean debugMode = false;

        public boolean chatLogInDebugMode = false;

        @ConfigEntry.Gui.Excluded
        public boolean ignoreCertificatesUseAtYourOwnRisk = false;

        @ConfigEntry.Gui.Excluded
        public int configVersionDoNotEdit = 2;

        public General() {
        }
    }

    @Config(name = "friends")
    public static class Friends implements ConfigData {
        @ConfigEntry.Gui.Tooltip()
        public List<String> friendList = new ArrayList<>();

        public boolean onlyShowFriendsWaypoints = false;

        public boolean onlyShowFriendsIconWaypoints = false;

        @ConfigEntry.Gui.Tooltip()
        public boolean alwaysShowFriendsWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean alwaysShowFriendsIconWaypoints = true;

        public boolean overwriteFriendWaypointColor = false;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public WaypointColor friendWaypointColor = WaypointColor.Black;

        public Friends() {
        }
    }

    @Config(name = "hud")
    public static class HudModule implements ConfigData {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showPlayerWaypoints = ConditionalActiveMode.ALWAYS;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showMarkerWaypoints = ConditionalActiveMode.ALWAYS;

        public boolean hidePlayersInRange = false;

        public boolean hidePlayersVisible = false;

        public boolean showTrackerDistance = true;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerTextScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerIconScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerTextScale = 90;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerIconScale = 85;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minVisiblePlayerDistance = 30;

        @ConfigEntry.Gui.Tooltip()
        public int minNotVisiblePlayerDistance = 10;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerWaypoints = 40;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minPlayerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconWaypoints = 40;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerWaypoints = 20;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconWaypoints = 20;

        public HudModule() {
        }
    }

    @Config(name = "minimap")
    public static class MiniMapModule implements ConfigData {
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showPlayerWaypoints = ConditionalActiveMode.ALWAYS;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showMarkerWaypoints = ConditionalActiveMode.ALWAYS;

        @ConfigEntry.Gui.Tooltip()
        public boolean hidePlayersInRange = false;

        @ConfigEntry.Gui.Tooltip()
        public boolean outOfBoundsPlayerWaypoints = true;

        @ConfigEntry.Gui.Tooltip()
        public boolean outOfBoundsMarkerWaypoints = true;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerTextScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerIconScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerTextScale = 90;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerIconScale = 80;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minPlayerDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerWaypoints = 40;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minPlayerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconWaypoints = 40;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerWaypoints = 30;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconDistance = 100000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconWaypoints = 30;

        public MiniMapModule() {
        }
    }

    @Config(name = "worldmap")
    public static class WorldMapModule implements ConfigData {
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showPlayerWaypoints = ConditionalActiveMode.ALWAYS;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ConditionalActiveMode showMarkerWaypoints = ConditionalActiveMode.ALWAYS;

        @ConfigEntry.Gui.Tooltip()
        public boolean hidePlayersInRange = false;

        public boolean waypointIconBackground = false;

        public boolean showTrackerDistance = true;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerTextScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int playerIconScale = 100;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerTextScale = 90;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.BoundedDiscrete(min = 10, max = 200)
        public int markerIconScale = 80;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minPlayerDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerDistance = 1000000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerWaypoints = 1000;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minPlayerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconDistance = 1000000;

        @ConfigEntry.Gui.Tooltip()
        public int maxPlayerIconWaypoints = 1000;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerDistance = 1000000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerWaypoints = 10000;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        public int minMarkerIconDistance = 0;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconDistance = 1000000;

        @ConfigEntry.Gui.Tooltip()
        public int maxMarkerIconWaypoints = 10000;

        public WorldMapModule() {
        }
    }

    public static class ServerEntry {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public MapType maptype;

        public String ip;

        public String link;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public MarkerVisibilityMode markerVisibilityMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> markerLayers;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SimpleMarkerVisibilityMode individualMarkerMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> markers;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public MarkerVisibilityMode areaMarkerVisibilityMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> areaMarkerLayers;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SimpleMarkerVisibilityMode individualAreaMarkerMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> areaMarkers;

        @ConfigEntry.Gui.PrefixText
        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public MarkerVisibilityMode iconMarkerVisibilityMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> iconMarkerLayers;

        @ConfigEntry.Gui.Tooltip()
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SimpleMarkerVisibilityMode individualIconMode;

        @ConfigEntry.Gui.Tooltip()
        public List<String> icons;

        @ConfigEntry.Gui.Excluded()
        public Map<String, String[]> dimensionMapping;

        public ServerEntry() {
            this("",
                    "",
                    MapType.Bluemap,
                    MarkerVisibilityMode.Auto,
                    new ArrayList<>(),
                    SimpleMarkerVisibilityMode.BlackList,
                    new ArrayList<>(),
                    MarkerVisibilityMode.Auto,
                    new ArrayList<>(),
                    SimpleMarkerVisibilityMode.BlackList,
                    new ArrayList<>(),
                    MarkerVisibilityMode.Auto,
                    new ArrayList<>(),
                    SimpleMarkerVisibilityMode.BlackList,
                    new ArrayList<>(),
                    new HashMap<>());
        }

        public ServerEntry(String ip,
                           String link,
                           MapType maptype,
                           MarkerVisibilityMode markerVisibilityMode,
                           List<String> markerLayers,
                           SimpleMarkerVisibilityMode individualMarkerMode,
                           List<String> markers,
                           MarkerVisibilityMode areaMarkerVisibilityMode,
                           List<String> areaMarkerLayers,
                           SimpleMarkerVisibilityMode individualAreaMarkerMode,
                           List<String> areaMarkers,
                           MarkerVisibilityMode iconMarkerVisibilityMode,
                           List<String> iconMarkerLayers,
                           SimpleMarkerVisibilityMode individualIconMode,
                           List<String> icons,
                           Map<String, String[]> dimensionMapping) {
            this.ip = ip;
            this.link = link;
            this.maptype = maptype;
            this.markerVisibilityMode = markerVisibilityMode;
            this.markerLayers = markerLayers;
            this.individualMarkerMode = individualMarkerMode;
            this.markers = markers;
            this.areaMarkerVisibilityMode = areaMarkerVisibilityMode;
            this.areaMarkerLayers = areaMarkerLayers;
            this.individualAreaMarkerMode = individualAreaMarkerMode;
            this.areaMarkers = areaMarkers;
            this.iconMarkerVisibilityMode = iconMarkerVisibilityMode;
            this.iconMarkerLayers = iconMarkerLayers;
            this.individualIconMode = individualIconMode;
            this.icons = icons;
            this.dimensionMapping = dimensionMapping;
        }

        public void setMarkerLayers(List<String> layers) {
            if (markerVisibilityMode == MarkerVisibilityMode.Auto) {
                markerLayers = layers;
                markerVisibilityMode = MarkerVisibilityMode.All;
            }
            if (areaMarkerVisibilityMode == MarkerVisibilityMode.Auto) {
                areaMarkerLayers = layers;
                areaMarkerVisibilityMode = MarkerVisibilityMode.All;
            }
            if (iconMarkerVisibilityMode == MarkerVisibilityMode.Auto) {
                iconMarkerLayers = layers;
                iconMarkerVisibilityMode = MarkerVisibilityMode.All;
            }
            CommonModConfig.saveConfig();
        }

        public boolean needsMarkerLayerUpdate() {
            return markerVisibilityMode == MarkerVisibilityMode.Auto
                    || areaMarkerVisibilityMode == MarkerVisibilityMode.Auto
                    || iconMarkerVisibilityMode == MarkerVisibilityMode.Auto;
        }

        public enum MapType {
            Bluemap,
            Dynmap,
            LiveAtlas,
            Pl3xMap,
            Squaremap;

            MapType() {
            }
        }

        public enum MarkerVisibilityMode {
            Auto(false),
            All(false),
            None(false),
            BlackList(true),
            WhiteList(true);

            public final boolean allowsEdit;

            MarkerVisibilityMode(boolean allowsEdit) {
                this.allowsEdit = allowsEdit;
            }
        }

        public enum SimpleMarkerVisibilityMode {
            BlackList,
            WhiteList;

            SimpleMarkerVisibilityMode() {
            }
        }

        public boolean includeMarkerLayer(String layer) {
            return includeLayer(markerVisibilityMode, markerLayers, layer);
        }

        public boolean includeAreaMarkerLayer(String layer) {
            return includeLayer(areaMarkerVisibilityMode, areaMarkerLayers, layer);
        }

        public boolean includeIconMarkerLayer(String layer) {
            return includeLayer(iconMarkerVisibilityMode, iconMarkerLayers, layer);
        }

        private boolean includeLayer(MarkerVisibilityMode markerVisibilityMode, List<String> layers, String layer) {
            switch (markerVisibilityMode) {
                case Auto:
                case All:
                    return true;
                case None:
                    return false;
                case BlackList:
                    return !layers.contains(layer);
                case WhiteList:
                    return layers.contains(layer);
                default:
                    throw new IllegalArgumentException();
            }
        }

        public boolean includeMarker(String name) {
            return include(individualMarkerMode, markers, name);
        }

        public boolean includeAreaMarker(String name) {
            return include(individualAreaMarkerMode, areaMarkers, name);
        }

        public boolean includeIcon(String name) {
            return include(individualIconMode, icons, name);
        }

        private boolean include(SimpleMarkerVisibilityMode visibilityMode, List<String> words, String name) {
            if (visibilityMode == SimpleMarkerVisibilityMode.BlackList) {
                for (String word : words) {
                    if (name.contains(word)) return false;
                }
                return true;
            } else {
                for (String word : words) {
                    if (name.contains(word)) return true;
                }
                return false;
            }
        }

        public int getMarkerVisibilityHash() {
            return Objects.hash(config.general.enableMarkerWaypoints, markerVisibilityMode, markerLayers,
                    individualMarkerMode, markers);
        }

        public int getAreaMarkerVisibilityHash() {
            return Objects.hash(config.general.enableAreaMarkerOverlay, areaMarkerVisibilityMode, areaMarkerLayers,
                    config.general.blocksPerChunkThreshold, config.general.areaFillAlphaMul, config.general.areaFillAlphaMin, config.general.areaFillAlphaMax,
                    config.general.areaLineAlphaMul, config.general.areaLineAlphaMin, config.general.areaLineAlphaMax,
                    config.general.maxChunkArea, individualAreaMarkerMode, areaMarkers);
        }

        public void addDimensionMapping(String clientDimension, String[] webMapDimensions) {
            String[] prevMapping = dimensionMapping.get(clientDimension);
            if (Arrays.equals(prevMapping, webMapDimensions)) return;
            if (config.general.debugMode && config.general.chatLogInDebugMode) {
                Utils.sendErrorToClientChat("mapping: " + clientDimension + " -> " + Arrays.toString(webMapDimensions));
            }
            dimensionMapping.put(clientDimension, webMapDimensions);
            CommonModConfig.saveConfig();
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
            return toText().getString();
        }

        public Component toText() {
            return Text.translatable("maplink.WaypointColor." + this.name()).setStyle(Style.EMPTY.withColor(ChatFormatting.values()[this.ordinal()]));
        }
    }

    public enum ConditionalActiveMode {
        NEVER,
        ALWAYS,
        WHEN_PLAYER_LIST_SHOWN,
        WHEN_PLAYER_LIST_HIDDEN;

        ConditionalActiveMode(){
        }

        public boolean isActive() {
            boolean playerListShown = Minecraft.getInstance().options.keyPlayerList.isDown();

            switch (this) {
                case NEVER: return false;
                case ALWAYS: return true;
                case WHEN_PLAYER_LIST_SHOWN: return playerListShown;
                case WHEN_PLAYER_LIST_HIDDEN: return !playerListShown;
            }
            return false;
        }

        @Override
        public String toString() {
            return toText().getString();
        }

        public Component toText() {
            return Text.translatable("maplink.ConditionalActiveMode." + this.name());
        }
    }
}