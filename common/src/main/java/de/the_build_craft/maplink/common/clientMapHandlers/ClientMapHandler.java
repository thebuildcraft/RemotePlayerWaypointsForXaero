/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2025 - 2026  Leander Knüttel and contributors
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

package de.the_build_craft.maplink.common.clientMapHandlers;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.HTTP;
import de.the_build_craft.maplink.common.ModConfig;
import de.the_build_craft.maplink.common.waypoints.*;
import de.the_build_craft.maplink.common.wrappers.Text;
import de.the_build_craft.maplink.common.wrappers.Utils;
import net.minecraft.ChatFormatting;
#if MC_VER >= MC_1_21_11
import net.minecraft.util.Util;
#else
import net.minecraft.Util;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
#if MC_VER >= MC_1_21_11
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static de.the_build_craft.maplink.common.CommonModConfig.*;
import static de.the_build_craft.maplink.common.FastUpdateTask.playerPositions;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public abstract class ClientMapHandler {
    public static final String waypointPrefix = "maplink_";
    private static final Map<String, NativeImage> iconLinkToNativeImage = new HashMap<>();
    private static final Map<String, DynamicTexture> iconLinkToTexture = new HashMap<>();

    private static final Map<String, WaypointState> idToWaypointState = new ConcurrentHashMap<>();

    private static ClientMapHandler instance;
    protected final Minecraft mc;

    private static final int maxMarkerCountBeforeWarning = 15;
    private boolean markerMessageWasShown = false;

    private int previousPlayerWaypointColor = 0;
    private int previousMarkerWaypointColor = 0;
    private int previousFriendWaypointColor = 0;
    private boolean previousFriendColorOverride = false;
    private int previousFriendListHashCode = 0;

    private final List<PlayerPosition> tempPlayerWaypointPositions = new ArrayList<>();
    protected final Set<String> currentPlayerIds = new HashSet<>();
    protected final Set<String> currentMarkerIds = new HashSet<>();

    public ClientMapHandler() {
        instance = this;
        mc = Minecraft.getInstance();
    }

    public static ClientMapHandler getInstance() {
        return instance;
    }

    public static WaypointState getWaypointState(String id) {
        return ClientMapHandler.idToWaypointState.get(id);
    }

    public static void registerPlayerPosition(PlayerPosition playerPosition, String iconLink) {
        if (config.general.useMcHeadsPlayerNameIcons) {
            iconLink = "https://mc-heads.net/avatar/" + playerPosition.name + "/32";
        }
        registerPosition(playerPosition, iconLink, true, false);
    }

    public static void registerTempPlayerPosition(PlayerPosition playerPosition) {
        registerPosition(playerPosition, "https://mc-heads.net/avatar/" + playerPosition.name + "/32", true, true);
    }

    public static void registerPosition(Position position, String iconLink) {
        registerPosition(position, iconLink, false, false);
    }

    public static void registerPosition(Position position, String iconLink, boolean isPlayer, boolean isTemp) {
        WaypointState waypointState = idToWaypointState.get(position.id);
        if (waypointState != null && (!waypointState.isTemp || isTemp)) return;
        if (!iconLinkToNativeImage.containsKey(iconLink)) {
            try {
                NativeImage nativeImage;
                try {
                    nativeImage = HTTP.makeImageHttpRequest(URI.create(iconLink).toURL());
                } catch (Exception e) {
                    if (isPlayer) {
                        nativeImage = HTTP.makeImageHttpRequest(URI.create("https://mc-heads.net/avatar/" + position.name + "/32").toURL());
                    } else {
                        throw e;
                    }
                }
                if (nativeImage.getWidth() == nativeImage.getHeight() && nativeImage.getWidth() <= 64) {
                    iconLinkToNativeImage.put(iconLink, nativeImage);
                } else {
                    NativeImage nativeImage1 = new NativeImage(64, 64, true);
                    nativeImage.resizeSubRectTo(0, 0, nativeImage.getWidth(), nativeImage.getHeight(), nativeImage1);
                    iconLinkToNativeImage.put(iconLink, nativeImage1);
                    nativeImage.close();
                }
            } catch (Exception e) {
                iconLink = null;
            }
        }
        WaypointState prev = idToWaypointState.put(position.id, new WaypointState(position.name, iconLink, isPlayer, isTemp));
        if (prev != null) prev.isOld = true;
    }

    public static void clearRegisteredPositions() {
        idToWaypointState.clear();
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    public static DynamicTexture getDynamicTexture(String link) {
        if (iconLinkToTexture.containsKey(link)) return iconLinkToTexture.get(link);
        if (!iconLinkToNativeImage.containsKey(link)) return null;
        #if MC_VER >= MC_1_21_5
        DynamicTexture texture = new DynamicTexture(() -> link, iconLinkToNativeImage.get(link));
        #else
        DynamicTexture texture = new DynamicTexture(iconLinkToNativeImage.get(link));
        #endif

        #if MC_VER < MC_1_21_11
        texture.setFilter(false, false);
        #endif

        Minecraft.getInstance().getTextureManager().register(ClientMapHandler.getIconResourceLocation(link), texture);
        DynamicTexture old = iconLinkToTexture.put(link, texture);
        if (old != null) {
            old.close();
        }
        return texture;
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    #if MC_VER >= MC_1_21_11
    public static Identifier getIconResourceLocation(String icon) {
        return Identifier.fromNamespaceAndPath("maplink", "xaeros_" + ClientMapHandler.makeResourceSafeString(icon));
    }
    #elif MC_VER >= MC_1_21_5
    public static ResourceLocation getIconResourceLocation(String icon) {
        return ResourceLocation.fromNamespaceAndPath("maplink", "xaeros_" + ClientMapHandler.makeResourceSafeString(icon));
    }
    #elif MC_VER >= MC_1_19_2
    public static ResourceLocation getIconResourceLocation(String icon) {
        return ResourceLocation.tryBuild("maplink", "xaeros_" + ClientMapHandler.makeResourceSafeString(icon));
    }
    #else
    public static ResourceLocation getIconResourceLocation(String icon) {
        return new ResourceLocation("maplink", "xaeros_" + ClientMapHandler.makeResourceSafeString(icon));
    }
    #endif

    //from Earthcomputer/minimap-sync licensed under the MIT License
    public static String makeResourceSafeString(String original) {
        //noinspection deprecation
        String hash = Hashing.sha1().hashUnencodedChars(original).toString();
        original = original.toLowerCase(Locale.ROOT);
        #if MC_VER >= MC_1_21_11
        original = Util.sanitizeName(original, Identifier::validPathChar);
        #else
        original = Util.sanitizeName(original, ResourceLocation::validPathChar);
        #endif
        return original + "/" + hash;
    }

    public void handlePlayerWaypoints() {
        #if MC_VER >= MC_1_21_9
        if (mc.getCameraEntity() == null || mc.level == null) return;
        #else
        if (mc.cameraEntity == null || mc.level == null) return;
        #endif
        if (config.general.enablePlayerWaypoints) {
            // Keep track of which waypoints were previously shown
            // to remove any that are not to be shown anymore
            currentPlayerIds.clear();

            Map<String, AbstractClientPlayer> playerClientEntityMap = mc.level.players().stream().collect(
                    #if MC_VER >= MC_1_21_9
                    Collectors.toMap(a -> a.getGameProfile().name(), a -> a));
                    #else
                    Collectors.toMap(a -> a.getGameProfile().getName(), a -> a));
                    #endif

            int maxHudD = config.hud.maxPlayerDistance;
            int minMiniD = config.minimap.minPlayerDistance;
            int maxMiniD = config.minimap.maxPlayerDistance;
            int minWorldD = config.worldmap.minPlayerDistance;
            int maxWorldD = config.worldmap.maxPlayerDistance;

            int minIconHudD = config.hud.minPlayerIconDistance;
            int maxIconHudD = config.hud.maxPlayerIconDistance;
            int minIconMiniD = config.minimap.minPlayerIconDistance;
            int maxIconMiniD = config.minimap.maxPlayerIconDistance;
            int minIconWorldD = config.worldmap.minPlayerIconDistance;
            int maxIconWorldD = config.worldmap.maxPlayerIconDistance;

            int onHud = 0;
            int maxOnHud = config.hud.maxPlayerWaypoints;
            int onMiniMap = 0;
            int maxOnMiniMap = config.minimap.maxPlayerWaypoints;
            int onWorldMap = 0;
            int maxOnWorldMap = config.worldmap.maxPlayerWaypoints;

            int iconsOnHud = 0;
            int maxIconsOnHud = config.hud.maxPlayerIconWaypoints;
            int iconsOnMiniMap = 0;
            int maxIconsOnMiniMap = config.minimap.maxPlayerIconWaypoints;
            int iconsOnWorldMap = 0;
            int maxIconsOnWorldMap = config.worldmap.maxPlayerIconWaypoints;

            boolean onlyShowFriends = config.friends.onlyShowFriendsWaypoints;
            boolean onlyShowFriendsIcons = config.friends.onlyShowFriendsIconWaypoints;
            boolean alwaysShowFriends = config.friends.alwaysShowFriendsWaypoints;
            boolean alwaysShowFriendIcons = config.friends.alwaysShowFriendsIconWaypoints;

            boolean enablePlayerIcons = config.general.enablePlayerIconWaypoints;

            #if MC_VER >= MC_1_21_9
            Vec3 cameraPos = mc.getCameraEntity().position();
            #else
            Vec3 cameraPos = mc.cameraEntity.position();
            #endif
            tempPlayerWaypointPositions.clear();
            tempPlayerWaypointPositions.addAll(playerPositions.values());
            tempPlayerWaypointPositions.sort(Comparator.comparing(p -> cameraPos.distanceToSqr(p.pos.x, p.pos.y, p.pos.z)));

            for (PlayerPosition playerPosition : tempPlayerWaypointPositions) {
                if (playerPosition == null) continue;
                String playerName = playerPosition.name;
                WaypointState waypointState = idToWaypointState.get(playerPosition.id);

                boolean isFriend = config.friends.friendList.contains(playerName);
                if (onlyShowFriends && !isFriend) continue;

                waypointState.renderOnHud = config.hud.showPlayerWaypoints != ModConfig.ConditionalActiveMode.NEVER;
                waypointState.renderOnMiniMap = config.minimap.showPlayerWaypoints != ModConfig.ConditionalActiveMode.NEVER;
                waypointState.renderOnWorldMap = config.worldmap.showPlayerWaypoints != ModConfig.ConditionalActiveMode.NEVER;

                // Check if this player is within the server's player entity tracking range
                int minHudD = config.hud.minNotVisiblePlayerDistance;
                if (playerClientEntityMap.containsKey(playerName)) {
                    if (config.hud.hidePlayersInRange) waypointState.renderOnHud = false;
                    if (config.minimap.hidePlayersInRange) waypointState.renderOnMiniMap = false;
                    if (config.worldmap.hidePlayersInRange) waypointState.renderOnWorldMap = false;

                    if (waypointState.renderOnHud) {
                        #if MC_VER >= MC_1_21_9
                        ClipContext clipContext = new ClipContext(mc.getCameraEntity().getEyePosition(),
                        #elif MC_VER >= MC_1_17_1
                        ClipContext clipContext = new ClipContext(mc.cameraEntity.getEyePosition(),
                        #else
                        ClipContext clipContext = new ClipContext(mc.cameraEntity.getEyePosition(1),
                        #endif
                                playerClientEntityMap.get(playerName).position().add(0, 1, 0),
                                #if MC_VER >= MC_1_21_9
                                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.getCameraEntity());
                                #else
                                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.cameraEntity);
                                #endif
                        // If this player is visible, don't show waypoint on Hud (depending on the config settings)
                        if (mc.level.clip(clipContext).getType() != HitResult.Type.BLOCK) {
                            if (config.hud.hidePlayersVisible) waypointState.renderOnHud = false;
                            minHudD = config.hud.minVisiblePlayerDistance;
                        }
                    }
                }

                double d = cameraPos.distanceTo(new Vec3(playerPosition.pos.x, playerPosition.pos.y, playerPosition.pos.z));

                if (alwaysShowFriends && isFriend) {
                    waypointState.renderOnHud &= d >= minHudD;
                } else {
                    waypointState.renderOnHud &= onHud < maxOnHud && d >= minHudD && d <= maxHudD;
                    waypointState.renderOnMiniMap &= onMiniMap < maxOnMiniMap && d >= minMiniD && d <= maxMiniD;
                    waypointState.renderOnWorldMap &= onWorldMap < maxOnWorldMap && d >= minWorldD && d <= maxWorldD;
                    if (waypointState.renderOnHud) onHud++;
                    if (waypointState.renderOnMiniMap) onMiniMap++;
                    if (waypointState.renderOnWorldMap) onWorldMap++;
                }

                if (!(waypointState.renderOnHud || waypointState.renderOnMiniMap || waypointState.renderOnWorldMap)) continue;

                if (!enablePlayerIcons || !waypointState.hasIcon) {
                    waypointState.renderIconOnHud = false;
                    waypointState.renderIconOnMiniMap = false;
                    waypointState.renderIconOnWorldMap = false;
                } else if (alwaysShowFriendIcons && isFriend) {
                    waypointState.renderIconOnHud = true;
                    waypointState.renderIconOnMiniMap = true;
                    waypointState.renderIconOnWorldMap = true;
                } else {
                    waypointState.renderIconOnHud = iconsOnHud < maxIconsOnHud && d >= minIconHudD && d <= maxIconHudD;
                    waypointState.renderIconOnMiniMap = iconsOnMiniMap < maxIconsOnMiniMap && d >= minIconMiniD && d <= maxIconMiniD;
                    waypointState.renderIconOnWorldMap = iconsOnWorldMap < maxIconsOnWorldMap && d >= minIconWorldD && d <= maxIconWorldD;
                    if (onlyShowFriendsIcons) {
                        waypointState.renderIconOnHud &= isFriend;
                        waypointState.renderIconOnMiniMap &= isFriend;
                        waypointState.renderIconOnWorldMap &= isFriend;
                    }
                    if (waypointState.renderIconOnHud) iconsOnHud++;
                    if (waypointState.renderIconOnMiniMap) iconsOnMiniMap++;
                    if (waypointState.renderIconOnWorldMap) iconsOnWorldMap++;
                }

                if (waypointState.renderOnHud || waypointState.renderOnMiniMap || waypointState.renderOnWorldMap) {
                    currentPlayerIds.add(playerPosition.id);
                    if (!config.general.showPlayerWaypointsAsTrackedPlayers) addOrUpdatePlayerWaypoint(playerPosition, waypointState);
                }
            }

            removeOldPlayerWaypoints();

            int newPlayerWaypointColor = config.general.playerWaypointColor.ordinal();
            int newFriendWaypointColor = config.friends.friendWaypointColor.ordinal();
            boolean newFriendColorOverride = config.friends.overwriteFriendWaypointColor;
            int newFriendListHashCode = config.friends.friendList.hashCode();
            if ((previousPlayerWaypointColor != newPlayerWaypointColor)
                    || (previousFriendWaypointColor != newFriendWaypointColor)
                    || (previousFriendColorOverride != newFriendColorOverride)
                    || (previousFriendListHashCode != newFriendListHashCode)) {
                previousPlayerWaypointColor = newPlayerWaypointColor;
                previousFriendWaypointColor = newFriendWaypointColor;
                previousFriendColorOverride = newFriendColorOverride;
                previousFriendListHashCode = newFriendListHashCode;

                updatePlayerWaypointColors();
            }
        } else {
            removeAllPlayerWaypoints();
        }
    }

    public abstract void reset();

    abstract void addOrUpdatePlayerWaypoint(PlayerPosition playerPosition, WaypointState waypointState);

    abstract void removeOldPlayerWaypoints();

    abstract void removeAllPlayerWaypoints();

    abstract void updatePlayerWaypointColors();

    public void handleMarkerWaypoints(List<Position> markerPositions) {
        #if MC_VER >= MC_1_21_9
        if (mc.getCameraEntity() == null) return;
        #else
        if (mc.cameraEntity == null) return;
        #endif

        if (config.general.enableMarkerWaypoints && AbstractModInitializer.getConnection() != null) {
            ModConfig.ServerEntry serverEntry = AbstractModInitializer.getConnection().serverEntry;

            // Keep track of which waypoints were previously shown
            // to remove any that are not to be shown anymore
            currentMarkerIds.clear();

            int minHudD = config.hud.minMarkerDistance;
            int maxHudD = config.hud.maxMarkerDistance;
            int minMiniD = config.minimap.minMarkerDistance;
            int maxMiniD = config.minimap.maxMarkerDistance;
            int minWorldD = config.worldmap.minMarkerDistance;
            int maxWorldD = config.worldmap.maxMarkerDistance;

            int minIconHudD = config.hud.minMarkerIconDistance;
            int maxIconHudD = config.hud.maxMarkerIconDistance;
            int minIconMiniD = config.minimap.minMarkerIconDistance;
            int maxIconMiniD = config.minimap.maxMarkerIconDistance;
            int minIconWorldD = config.worldmap.minMarkerIconDistance;
            int maxIconWorldD = config.worldmap.maxMarkerIconDistance;

            int onHud = 0;
            int maxOnHud = config.hud.maxMarkerWaypoints;
            int onMiniMap = 0;
            int maxOnMiniMap = config.minimap.maxMarkerWaypoints;
            int onWorldMap = 0;
            int maxOnWorldMap = config.worldmap.maxMarkerWaypoints;

            int iconsOnHud = 0;
            int maxIconsOnHud = config.hud.maxMarkerIconWaypoints;
            int iconsOnMiniMap = 0;
            int maxIconsOnMiniMap = config.minimap.maxMarkerIconWaypoints;
            int iconsOnWorldMap = 0;
            int maxIconsOnWorldMap = config.worldmap.maxMarkerIconWaypoints;

            boolean enableMarkerIcons = config.general.enableMarkerIcons;

            #if MC_VER >= MC_1_21_9
            Vec3 cameraPos = mc.getCameraEntity().position();
            #else
            Vec3 cameraPos = mc.cameraEntity.position();
            #endif
            markerPositions.sort(Comparator.comparing(p -> cameraPos.distanceToSqr(p.pos.x, p.pos.y, p.pos.z)));

            for (Position markerPosition : markerPositions) {
                WaypointState waypointState = idToWaypointState.get(markerPosition.id);
                double d = cameraPos.distanceTo(new Vec3(markerPosition.pos.x, markerPosition.pos.y, markerPosition.pos.z));

                waypointState.renderOnHud = config.hud.showMarkerWaypoints != ModConfig.ConditionalActiveMode.NEVER
                        && onHud < maxOnHud && d >= minHudD && d <= maxHudD;
                waypointState.renderOnMiniMap = config.minimap.showMarkerWaypoints != ModConfig.ConditionalActiveMode.NEVER
                        && onMiniMap < maxOnMiniMap && d >= minMiniD && d <= maxMiniD;
                waypointState.renderOnWorldMap = config.worldmap.showMarkerWaypoints != ModConfig.ConditionalActiveMode.NEVER
                        && onWorldMap < maxOnWorldMap && d >= minWorldD && d <= maxWorldD;
                if (waypointState.renderOnHud) onHud++;
                if (waypointState.renderOnMiniMap) onMiniMap++;
                if (waypointState.renderOnWorldMap) onWorldMap++;

                if (!(waypointState.renderOnHud || waypointState.renderOnMiniMap || waypointState.renderOnWorldMap)) continue;

                if (enableMarkerIcons && waypointState.hasIcon
                        && serverEntry.includeIconMarkerLayer(markerPosition.layer.id) && serverEntry.includeIcon(markerPosition.name)) {
                    waypointState.renderIconOnHud = iconsOnHud < maxIconsOnHud && d >= minIconHudD && d <= maxIconHudD;
                    waypointState.renderIconOnMiniMap = iconsOnMiniMap < maxIconsOnMiniMap && d >= minIconMiniD && d <= maxIconMiniD;
                    waypointState.renderIconOnWorldMap = iconsOnWorldMap < maxIconsOnWorldMap && d >= minIconWorldD && d <= maxIconWorldD;
                    if (waypointState.renderIconOnHud) iconsOnHud++;
                    if (waypointState.renderIconOnMiniMap) iconsOnMiniMap++;
                    if (waypointState.renderIconOnWorldMap) iconsOnWorldMap++;
                } else {
                    waypointState.renderIconOnHud = false;
                    waypointState.renderIconOnMiniMap = false;
                    waypointState.renderIconOnWorldMap = false;
                }

                currentMarkerIds.add(markerPosition.id);
                addOrUpdateMarkerWaypoint(markerPosition, waypointState);
            }

            removeOldMarkerWaypoints();

            int newMarkerWaypointColor = config.general.markerWaypointColor.ordinal();
            if (previousMarkerWaypointColor != newMarkerWaypointColor) {
                previousMarkerWaypointColor = newMarkerWaypointColor;
                updateMarkerWaypointColors();
            }

            if (!markerMessageWasShown && (onHud > maxMarkerCountBeforeWarning || onMiniMap > maxMarkerCountBeforeWarning) && !config.general.ignoreMarkerMessage) {
                markerMessageWasShown = true;
                Utils.sendToClientChat(Text.literal("[" + AbstractModInitializer.MOD_NAME + "]: " +
                                "Looks like you have quite a lot of markers from the server visible! " +
                                "Did you know that you can chose the marker layers that are shown in the config, decrease their maximum distance, set a limit on how many are displayed or disable marker waypoints entirely? (The default config already limits the amount to 20) ")
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
                        .append(Text.literal("[Don't show this again]")
                                .withStyle(Style.EMPTY.withClickEvent(
                                    #if MC_VER < MC_1_21_5
                                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + AbstractModInitializer.MOD_ID + " ignore_marker_message"))
                                    #else
                                    new ClickEvent.SuggestCommand("/" + AbstractModInitializer.MOD_ID + " ignore_marker_message"))
                                    #endif
                                        .withColor(ChatFormatting.GREEN).withBold(true))));
            }
        } else {
            removeAllMarkerWaypoints();
        }
    }

    abstract void addOrUpdateMarkerWaypoint(Position markerPosition, WaypointState waypointState);

    abstract void removeOldMarkerWaypoints();

    public abstract void removeAllMarkerWaypoints();

    abstract void updateMarkerWaypointColors();

    public abstract void handleAreaMarkers(List<AreaMarker> markerPositions);

    public abstract void removeAllAreaMarkers(boolean clearXaeroHash);
}
