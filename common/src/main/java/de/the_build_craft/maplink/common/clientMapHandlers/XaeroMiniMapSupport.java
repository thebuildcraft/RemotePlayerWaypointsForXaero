/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2026  Leander Knüttel and contributors
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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.MainThreadTaskQueue;
import de.the_build_craft.maplink.common.waypoints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
#if MC_VER >= MC_1_19_4
import org.joml.Matrix4f;
#else
import com.mojang.math.Matrix4f;
#endif
import xaero.common.HudMod;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.waypoint.WaypointColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static de.the_build_craft.maplink.common.CommonModConfig.config;
import static de.the_build_craft.maplink.common.CommonModConfig.getPlayerWaypointColor;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class XaeroMiniMapSupport implements IXaeroMiniMapSupport {
    private static XaeroMiniMapSupport instance;

    public static final Map<String, TempWaypoint> idToHudPlayer = new ConcurrentHashMap<>();
    public static final Map<String, TempWaypoint> idToMiniMapPlayer = new ConcurrentHashMap<>();
    public static final Map<String, TempWaypoint> idToHudMarker = new ConcurrentHashMap<>();
    public static final Map<String, TempWaypoint> idToMiniMapMarker = new ConcurrentHashMap<>();

    private final Map<DynamicTexture, List<XaeroIconRenderData>> textureToData = new Object2ObjectOpenHashMap<>();
    int xaeroAutoConvertToKmThreshold;

    public XaeroMiniMapSupport() {
        instance = this;
    }

    public static XaeroMiniMapSupport getInstance() {
        return instance;
    }

    @Override
    public void addOrUpdateMiniMapWaypoint(Position position, WaypointState waypointState, boolean miniMap) {
        if (!AbstractModInitializer.connected || !AbstractModInitializer.xaeroMiniMapInstalled) return;

        Map<String, TempWaypoint> idToWaypoint;
        if (waypointState.isPlayer) {
            if (miniMap) idToWaypoint = idToMiniMapPlayer;
            else idToWaypoint = idToHudPlayer;
        } else {
            if (miniMap) idToWaypoint = idToMiniMapMarker;
            else idToWaypoint = idToHudMarker;
        }

        MainThreadTaskQueue.QueuedTask<Void> queuedTask = XaeroClientMapHandler.queuedTaskMap.get(position.id);
        if (queuedTask != null) {
            if (queuedTask.future.isDone()) {
                XaeroClientMapHandler.queuedTaskMap.remove(position.id);
            } else {
                queuedTask.future.thenRun(() -> {
                    Waypoint w = idToWaypoint.get(position.id);
                    if (w != null) {
                        Int3 pos = position.pos.floorToInt3();
                        w.setX(pos.x);
                        w.setY(pos.y);
                        w.setZ(pos.z);
                    }
                });
                return;
            }
        }
        Waypoint w = idToWaypoint.get(position.id);
        if (w != null) {
            Int3 pos = position.pos.floorToInt3();
            w.setX(pos.x);
            w.setY(pos.y);
            w.setZ(pos.z);
        } else {
            XaeroClientMapHandler.queuedTaskMap.put(position.id, MainThreadTaskQueue.queueTask(() -> {
                idToWaypoint.put(position.id, new TempWaypoint(position, waypointState));
            }));
        }
    }

    private static void updateMiniMapWaypointColors(Map<String, TempWaypoint> map, Function<TempWaypoint, Integer> waypointToColor) {
        for (TempWaypoint waypoint : map.values()) {
            waypoint.setWaypointColor(WaypointColor.fromIndex(waypointToColor.apply(waypoint)));
        }
    }

    @Override
    public void removeOldPlayerWaypoints() {
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToHudPlayer, w -> !w.renderOnHud, ClientMapHandler.getInstance().currentPlayerIds);
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToMiniMapPlayer, w -> !w.renderOnMiniMap, ClientMapHandler.getInstance().currentPlayerIds);
    }

    @Override
    public void removeOldMarkerWaypoints() {
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToHudMarker, w -> !w.renderOnHud, ClientMapHandler.getInstance().currentMarkerIds);
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToMiniMapMarker, w -> !w.renderOnMiniMap, ClientMapHandler.getInstance().currentMarkerIds);
    }

    @Override
    public void updatePlayerWaypointColors() {
        updateMiniMapWaypointColors(idToHudPlayer, w -> getPlayerWaypointColor(w.getName()));
        updateMiniMapWaypointColors(idToMiniMapPlayer, w -> getPlayerWaypointColor(w.getName()));
    }

    @Override
    public void updateMarkerWaypointColors() {
        updateMiniMapWaypointColors(idToHudMarker, w -> config.general.markerWaypointColor.ordinal());
        updateMiniMapWaypointColors(idToMiniMapMarker, w -> config.general.markerWaypointColor.ordinal());
    }

    @Override
    public void removeAllPlayerWaypoints() {
        idToHudPlayer.clear();
        idToMiniMapPlayer.clear();
    }

    @Override
    public void removeAllMarkerWaypoints() {
        idToHudMarker.clear();
        idToMiniMapMarker.clear();
    }

    @Override
    public void cacheXaeroSettings() {
        xaeroAutoConvertToKmThreshold = HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getCurrentProfile().get(MinimapProfiledConfigOptions.WAYPOINT_CONVERT_DISTANCE_TO_KM_AT);
    }

    @Override
    public int getXaeroAutoConvertToKmThreshold() {
        return xaeroAutoConvertToKmThreshold;
    }

    @Override
    public void batchDrawCustomIcon(Matrix4f matrix, DynamicTexture dynamicTexture, float x, float y, int width, int height, float a) {
        if (dynamicTexture == null) return;
        if (!textureToData.containsKey(dynamicTexture)) textureToData.put(dynamicTexture, new ArrayList<>());
        try {
            #if MC_VER >= MC_1_19_4
            textureToData.get(dynamicTexture).add(new XaeroIconRenderData((Matrix4f) matrix.clone(), x, y, width, height, a));
            #else
            textureToData.get(dynamicTexture).add(new XaeroIconRenderData(new Matrix4f(matrix), x, y, width, height, a));
            #endif
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    //(but heavily modified)
    @Override
    public void drawAllCustomIcons() {
        if (textureToData.isEmpty()) return;
        MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers = BuiltInHudModules.MINIMAP.getCurrentSession().getMultiTextureRenderTypeRenderers();
        MultiTextureRenderTypeRenderer renderer = multiTextureRenderTypeRenderers.getRenderer(
                #if MC_VER >= MC_26_1_0
                CustomRenderTypes.GUI_NEAREST);
                #elif MC_VER >= MC_1_21_11
                MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                CustomRenderTypes.GUI_NEAREST);
                #elif MC_VER >= MC_1_21_6
                MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                CustomRenderTypes.GUI);
                #elif MC_VER >= MC_1_21_5
                MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                CustomRenderTypes.GUI_NEAREST);
                #elif MC_VER >= MC_1_17_1
                textureId -> RenderSystem.setShaderTexture(0, textureId),
                MultiTextureRenderTypeRendererProvider::defaultTextureBind,
                CustomRenderTypes.GUI_NEAREST);
                #else
                RenderSystem::bindTexture,
                CustomRenderTypes.GUI_NEAREST);
                #endif
        for (Map.Entry<DynamicTexture, List<XaeroIconRenderData>> iconData : textureToData.entrySet()) {
            #if MC_VER >= MC_26_1_0
            BufferBuilder buffer = renderer.begin(iconData.getKey().getTextureView());
            #elif MC_VER >= MC_1_21_5
            BufferBuilder buffer = renderer.begin(iconData.getKey().getTexture());
            #else
            BufferBuilder buffer = renderer.begin(iconData.getKey().getId());
            #endif

            for (XaeroIconRenderData xaeroIconRenderData : iconData.getValue()) {
                Matrix4f pose = xaeroIconRenderData.pose;
                float x = xaeroIconRenderData.x;
                float y = xaeroIconRenderData.y;
                float width = xaeroIconRenderData.width;
                float height = xaeroIconRenderData.height;
                int a = (int) (xaeroIconRenderData.a * 255);

                #if MC_VER >= MC_1_21_9
                buffer.addVertex(pose, x, y, 0).setColor(255, 255, 255, a).setUv(0, 0);
                buffer.addVertex(pose, x, y + height, 0).setColor(255, 255, 255, a).setUv(0, 1);
                buffer.addVertex(pose, x + width, y + height, 0).setColor(255, 255, 255, a).setUv(1, 1);
                buffer.addVertex(pose, x + width, y, 0).setColor(255, 255, 255, a).setUv(1, 0);
                #elif MC_VER >= MC_1_21_1
                buffer.addVertex(pose, x, y, 0).setWhiteAlpha(a).setUv(0, 0);
                buffer.addVertex(pose, x, y + height, 0).setWhiteAlpha(a).setUv(0, 1);
                buffer.addVertex(pose, x + width, y + height, 0).setWhiteAlpha(a).setUv(1, 1);
                buffer.addVertex(pose, x + width, y, 0).setWhiteAlpha(a).setUv(1, 0);
                #elif MC_VER >= MC_1_17_1
                buffer.vertex(pose, x, y, 0).color(255, 255, 255, a).uv(0, 0).endVertex();
                buffer.vertex(pose, x, y + height, 0).color(255, 255, 255, a).uv(0, 1).endVertex();
                buffer.vertex(pose, x + width, y + height, 0).color(255, 255, 255, a).uv(1, 1).endVertex();
                buffer.vertex(pose, x + width, y, 0).color(255, 255, 255, a).uv(1, 0).endVertex();
                #else
                buffer.vertex(pose, x, y, 0).uv(0, 0).color(255, 255, 255, a).endVertex();
                buffer.vertex(pose, x, y + height, 0).uv(0, 1).color(255, 255, 255, a).endVertex();
                buffer.vertex(pose, x + width, y + height, 0).uv(1, 1).color(255, 255, 255, a).endVertex();
                buffer.vertex(pose, x + width, y, 0).uv(1, 0).color(255, 255, 255, a).endVertex();
                #endif
            }
        }
        multiTextureRenderTypeRenderers.draw(renderer);
        textureToData.clear();
    }
}
