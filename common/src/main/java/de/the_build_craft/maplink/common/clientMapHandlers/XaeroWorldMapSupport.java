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

#if MC_VER >= MC_1_21_5
import com.mojang.blaze3d.textures.GpuTexture;
#endif
import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.MainThreadTaskQueue;
import de.the_build_craft.maplink.common.level.TileConverter;
import de.the_build_craft.maplink.common.waypoints.CustomWorldMapWaypoint;
import de.the_build_craft.maplink.common.waypoints.Int3;
import de.the_build_craft.maplink.common.waypoints.Position;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
#if MC_VER >= MC_1_21_11
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
#endif
import xaero.map.icon.XaeroIcon;
import xaero.map.icon.XaeroIconAtlas;
import xaero.map.mods.gui.Waypoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static de.the_build_craft.maplink.common.CommonModConfig.config;
import static de.the_build_craft.maplink.common.CommonModConfig.getPlayerWaypointColor;

/**
 * @author Leander Knüttel
 * @version 08.03.2026
 */
public class XaeroWorldMapSupport implements IXaeroWorldMapSupport {
    private static XaeroWorldMapSupport instance;

    private static final Map<String, XaeroIcon> iconLinkToXaeroIcon = new HashMap<>();
    public static final Map<String, Waypoint> idToWorldMapPlayer = new ConcurrentHashMap<>();
    public static final Map<String, Waypoint> idToWorldMapMarker = new ConcurrentHashMap<>();

    boolean xaeroWaypointBackground;
    #if MC_VER >= MC_1_21_11
    public final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRendererProvider;
    public MultiTextureRenderTypeRenderer GUI_NEAREST_Renderer;
    #endif

    public XaeroWorldMapSupport() {
        instance = this;
        #if MC_VER >= MC_1_21_11
        multiTextureRenderTypeRendererProvider = new MultiTextureRenderTypeRendererProvider(2);
        #endif
    }

    public static XaeroWorldMapSupport getInstance() {
        return instance;
    }

    @Override
    public void addOrUpdateWorldMapWaypoint(Position position, WaypointState waypointState) {
        if (!AbstractModInitializer.connected || !AbstractModInitializer.xaeroWorldMapInstalled) return;

        Map<String, Waypoint> idToWaypoint;
        if (waypointState.isPlayer) idToWaypoint = idToWorldMapPlayer;
        else idToWaypoint = idToWorldMapMarker;

        MainThreadTaskQueue.QueuedTask<Void> queuedTask = XaeroClientMapHandler.queuedTaskMap.get(position.id);
        if (queuedTask != null) {
            if (queuedTask.future.isDone()) {
                XaeroClientMapHandler.queuedTaskMap.remove(position.id);
            } else {
                queuedTask.future.thenRun(() -> {
                    Waypoint wp = idToWaypoint.get(position.id);
                    if (wp != null) {
                        xaero.common.minimap.waypoints.Waypoint w = (xaero.common.minimap.waypoints.Waypoint) wp.getOriginal();
                        Int3 pos = position.pos.floorToInt3();
                        w.setX(pos.x);
                        w.setY(pos.y);
                        w.setZ(pos.z);
                    }
                });
                return;
            }
        }
        Waypoint wp = idToWaypoint.get(position.id);
        if (wp != null) {
            xaero.common.minimap.waypoints.Waypoint w = (xaero.common.minimap.waypoints.Waypoint) wp.getOriginal();
            Int3 pos = position.pos.floorToInt3();
            w.setX(pos.x);
            w.setY(pos.y);
            w.setZ(pos.z);
        } else {
            XaeroClientMapHandler.queuedTaskMap.put(position.id, MainThreadTaskQueue.queueTask(() -> {
                idToWaypoint.put(position.id, new CustomWorldMapWaypoint(position, waypointState));
            }));
        }
    }

    private static void updateWorldMapWaypointColors(Map<String, Waypoint> map, Function<Waypoint, Integer> waypointToColor) {
        for (Waypoint waypoint : map.values()) {
            ((xaero.common.minimap.waypoints.Waypoint)waypoint.getOriginal()).setWaypointColor(WaypointColor.fromIndex(waypointToColor.apply(waypoint)));
        }
    }

    @Override
    public Object getXaeroIcon(String link) {
        if (iconLinkToXaeroIcon.containsKey(link))
            return iconLinkToXaeroIcon.get(link);
        DynamicTexture dynamicTexture = ClientMapHandler.getDynamicTexture(link);
        if (dynamicTexture == null) return null;
        #if MC_VER >= MC_1_21_5
        GpuTexture texture = dynamicTexture.getTexture();
        XaeroIcon icon = XaeroIconAtlas.Builder.begin().setPreparedTexture(texture)
                .setIconWidth(texture.getWidth(0)).setWidth(texture.getWidth(0)).build().createIcon();
        #else
        int textureId = dynamicTexture.getId();
        XaeroIcon icon = XaeroIconAtlas.Builder.begin().setPreparedTexture(textureId)
            .setIconWidth(dynamicTexture.getPixels().getWidth()).setWidth(dynamicTexture.getPixels().getWidth()).build().createIcon();
        #endif
        iconLinkToXaeroIcon.put(link, icon);
        return icon;
    }

    @Override
    public void removeOldPlayerWaypoints() {
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToWorldMapPlayer, w -> !w.renderOnWorldMap, ClientMapHandler.getInstance().currentPlayerIds);
    }

    @Override
    public void removeOldMarkerWaypoints() {
        XaeroClientMapHandler.removeOldWaypointsFromMap(idToWorldMapMarker, w -> !w.renderOnWorldMap, ClientMapHandler.getInstance().currentMarkerIds);
    }

    @Override
    public void updatePlayerWaypointColors() {
        updateWorldMapWaypointColors(idToWorldMapPlayer, w -> getPlayerWaypointColor(w.getName()));
    }

    @Override
    public void updateMarkerWaypointColors() {
        updateWorldMapWaypointColors(idToWorldMapMarker, w -> config.general.markerWaypointColor.ordinal());
    }

    @Override
    public void removeAllPlayerWaypoints() {
        idToWorldMapPlayer.clear();
    }

    @Override
    public void removeAllMarkerWaypoints() {
        idToWorldMapMarker.clear();
    }

    #if MC_VER >= MC_26_1_2
    @Override
    public void createGuiNearestRenderer() {
        GUI_NEAREST_Renderer = multiTextureRenderTypeRendererProvider.getRenderer(CustomRenderTypes.GUI_NEAREST);
    }
    #elif MC_VER >= MC_1_21_11
    @Override
    public void createGuiNearestRenderer() {
        GUI_NEAREST_Renderer = multiTextureRenderTypeRendererProvider.getRenderer(MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.GUI_NEAREST);
    }
    #endif

    #if MC_VER >= MC_1_21_11
    @Override
    public Object getGuiNearestRenderer() {
        return GUI_NEAREST_Renderer;
    }

    @Override
    public void drawGuiNearestRenderer() {
        multiTextureRenderTypeRendererProvider.draw(GUI_NEAREST_Renderer);
    }
    #endif

    @Override
    public boolean getXaeroWaypointBackground() {
        return xaeroWaypointBackground;
    }

    @Override
    public void cacheXaeroSettings() {
        xaeroWaypointBackground = WorldMap.INSTANCE.getConfigs().getClientConfigManager().getCurrentProfile().get(WorldMapProfiledConfigOptions.WAYPOINT_BACKGROUNDS);
    }

    @Override
    public int getXaeroAutoConvertToKmThreshold() {
        return 10_000;
    }

    @Override
    public void clearTiles() {
        TileConverter.clear();
    }

    @Override
    public void init(int centerChunkX, int centerChunkZ, int maxChunksX, int maxChunksZ) {
        TileConverter.init(centerChunkX, centerChunkZ, maxChunksX, maxChunksZ);
    }

    @Override
    public void writeBlock(int x, int z, int light, int height, int pixelRgb) {
        TileConverter.writeBlock(x, z, light, height, pixelRgb);
    }

    @Override
    public void setReadyForRender() {
        TileConverter.readyForRender = true;
    }
}
