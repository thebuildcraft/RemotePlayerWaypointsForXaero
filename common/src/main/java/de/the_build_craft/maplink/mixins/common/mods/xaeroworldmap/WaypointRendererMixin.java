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

package de.the_build_craft.maplink.mixins.common.mods.xaeroworldmap;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.waypoints.CustomWorldMapWaypoint;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
#if MC_VER >= MC_1_21_6
import xaero.map.element.MapElementGraphics;
#elif MC_VER >= MC_1_20_1
import net.minecraft.client.gui.GuiGraphics;
#endif
#if MC_VER >= MC_26_2_0
import xaero.lib.client.graphics.XaeroBufferProvider;
#else
import net.minecraft.client.renderer.MultiBufferSource;
#endif
#if MC_VER >= MC_1_19_4
import org.joml.Matrix4f;
#else
import com.mojang.math.Matrix4f;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#if MC_VER >= MC_26_1_0
import com.mojang.blaze3d.textures.GpuTextureView;
#elif MC_VER >= MC_1_21_5
import com.mojang.blaze3d.textures.GpuTexture;
#endif
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.icon.XaeroIcon;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointRenderer;
import xaero.map.mods.gui.WaypointSymbolCreator;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(WaypointRenderer.class)
public class WaypointRendererMixin {
    #if MC_VER >= MC_26_2_0
    @Inject(method = "preRender", at = @At("HEAD"))
    private void createGuiNearestRenderer(ElementRenderInfo renderInfo, XaeroBufferProvider xaeroBufferProvider, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow, CallbackInfo ci) {
        XaeroClientMapHandler.xaeroWorldMapSupport.createGuiNearestRenderer();
    }

    @Inject(method = "postRender", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V", shift = At.Shift.AFTER))
    private void drawGuiNearestRenderer(ElementRenderInfo renderInfo, XaeroBufferProvider xaeroBufferProvider, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow, CallbackInfo ci) {
        XaeroClientMapHandler.xaeroWorldMapSupport.drawGuiNearestRenderer();
    }
    #elif MC_VER >= MC_1_21_11
    @Inject(method = "preRender", at = @At("HEAD"))
    private void createGuiNearestRenderer(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow, CallbackInfo ci) {
        XaeroClientMapHandler.xaeroWorldMapSupport.createGuiNearestRenderer();
    }

    @Inject(method = "postRender", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V", shift = At.Shift.AFTER))
    private void drawGuiNearestRenderer(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow, CallbackInfo ci) {
        XaeroClientMapHandler.xaeroWorldMapSupport.drawGuiNearestRenderer();
    }
    #endif

    #if MC_VER >= MC_1_21_6
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lxaero/map/mods/gui/WaypointSymbolCreator;getSymbolTexture(Lxaero/map/element/MapElementGraphics;Ljava/lang/String;)Lxaero/map/icon/XaeroIcon;"))
    private XaeroIcon getCustomIcon(WaypointSymbolCreator waypointSymbolCreator,
                                    MapElementGraphics guiGraphics,
                                    String c,
                                    Operation<XaeroIcon> original,
                                    Waypoint w) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnWorldMap) {
                return (XaeroIcon) waypointState.getXaeroIcon();
            } else {
                return original.call(waypointSymbolCreator, guiGraphics, waypointState.abbreviation);
            }
        } else {
            return original.call(waypointSymbolCreator, guiGraphics, c);
        }
    }
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/mods/gui/WaypointSymbolCreator;getSymbolTexture(Lnet/minecraft/client/gui/GuiGraphics;Ljava/lang/String;)Lxaero/map/icon/XaeroIcon;"))
    private XaeroIcon getCustomIcon(WaypointSymbolCreator waypointSymbolCreator,
                                    GuiGraphics guiGraphics,
                                    String c,
                                    Operation<XaeroIcon> original,
                                    Waypoint w) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnWorldMap) {
                return (XaeroIcon) waypointState.getXaeroIcon();
            } else {
                return original.call(waypointSymbolCreator, guiGraphics, waypointState.abbreviation);
            }
        } else {
            return original.call(waypointSymbolCreator, guiGraphics, c);
        }
    }
    #else
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/mods/gui/WaypointSymbolCreator;getSymbolTexture(Ljava/lang/String;)Lxaero/map/icon/XaeroIcon;"))
    private XaeroIcon getCustomIcon(WaypointSymbolCreator instance,
                                    String s,
                                    Operation<XaeroIcon> original,
                                    Waypoint w) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnWorldMap) {
                return (XaeroIcon) waypointState.getXaeroIcon();
            } else {
                return original.call(instance, waypointState.abbreviation);
            }
        } else {
            return original.call(instance, s);
        }
    }
    #endif

    @ModifyExpressionValue(method = "renderElement*", at = @At(value = "INVOKE", target = "Lxaero/map/mods/gui/Waypoint;getSymbol()Ljava/lang/String;", ordinal = 0))
    private String modifySymbolForCustomIcons(String original, Waypoint w) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnWorldMap) {
                return "___";
            } else {
                return waypointState.abbreviation;
            }
        } else {
            return original;
        }
    }

    @ModifyVariable(method = "renderElement*", at = @At(value = "STORE"), ordinal = 1, name = "renderBackground")
    private boolean renderBackground(boolean renderBackground, Waypoint w, boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            return hovered || (XaeroClientMapHandler.xaeroWorldMapSupport.getXaeroWaypointBackground() && config.worldmap.waypointIconBackground);
        } else {
            return renderBackground;
        }
    }

    #if MC_VER >= MC_26_2_0
    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V",
    #elif MC_VER >= MC_1_21_6
    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V",
    #elif MC_VER >= MC_1_20_1
    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V",
    #else
    @Inject(method = "renderElementShadow(Lxaero/map/mods/gui/Waypoint;ZFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V",
    #endif
            at = @At(value = "HEAD"), cancellable = true)
    private void disableShadowIfBackgroundIsDisabled(Waypoint w,
                                                     boolean hovered,
                                                     float optionalScale,
                                                     double partialX,
                                                     double partialY,
                                                     ElementRenderInfo renderInfo,
                                                     #if MC_VER >= MC_1_21_6
                                                     MapElementGraphics guiGraphics,
                                                     #elif MC_VER >= MC_1_20_1
                                                     GuiGraphics guiGraphics,
                                                     #else
                                                     PoseStack poseStack,
                                                     #endif
                                                     #if MC_VER >= MC_26_2_0
                                                     XaeroBufferProvider xaeroBufferProvider,
                                                     #else
                                                     MultiBufferSource.BufferSource vanillaBufferSource,
                                                     #endif
                                                     MultiTextureRenderTypeRendererProvider rendererProvider,
                                                     CallbackInfo ci
    ) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            if ((waypointState.isPlayer && !config.worldmap.showPlayerWaypoints.isActive())
                    || (!waypointState.isPlayer && !config.worldmap.showMarkerWaypoints.isActive())
                    || (waypointState.renderIconOnWorldMap && !hovered && !config.worldmap.waypointIconBackground)) {
                ci.cancel();
            }
        }
    }

    #if MC_VER >= MC_1_19_4
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1))
    private static void translate(PoseStack poseStack,
                                 float $$0,
                                 float $$1,
                                 float $$2,
                                 Operation<Void> original,
                                 Waypoint w,
                                 boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            boolean renderBackground = hovered || (XaeroClientMapHandler.xaeroWorldMapSupport.getXaeroWaypointBackground() && config.worldmap.waypointIconBackground);
            original.call(poseStack, -15f, renderBackground ? -41f : -12, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }
    #else
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 2))
    private static void translate(PoseStack poseStack,
                                  double $$0,
                                  double $$1,
                                  double $$2,
                                  Operation<Void> original,
                                  Waypoint w,
                                  boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            boolean renderBackground = hovered || (XaeroClientMapHandler.xaeroWorldMapSupport.getXaeroWaypointBackground() && config.worldmap.waypointIconBackground);
            original.call(poseStack, -15d, renderBackground ? -41d : -12, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }
    #endif

    #if MC_VER >= MC_1_19_4
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 2))
    private static void translateText(PoseStack poseStack,
                                  float $$0,
                                  float $$1,
                                  float $$2,
                                  Operation<Void> original,
                                  Waypoint w,
                                  boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            original.call(poseStack, $$0, -38f, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }
    #else
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 3))
    private static void translateText(PoseStack poseStack,
                                  double $$0,
                                  double $$1,
                                  double $$2,
                                  Operation<Void> original,
                                  Waypoint w,
                                  boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            original.call(poseStack, $$0, -38d, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }
    #endif

    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1))
    private static void scale(PoseStack poseStack,
                              float $$0,
                              float $$1,
                              float $$2,
                              Operation<Void> original,
                              Waypoint w,
                              boolean hovered) {
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            original.call(poseStack, .45f, .45f, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }

    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    private static void scale2(PoseStack poseStack,
                              float $$0,
                              float $$1,
                              float $$2,
                              Operation<Void> original,
                              Waypoint w,
                              boolean hovered) {
        maplink$scale(poseStack, $$0, $$1, $$2, original, w);
    }

    @WrapOperation(method = "renderElementShadow*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 0))
    private static void scale3(PoseStack poseStack,
                               float $$0,
                               float $$1,
                               float $$2,
                               Operation<Void> original,
                               Waypoint w,
                               boolean hovered) {
        maplink$scale(poseStack, $$0, $$1, $$2, original, w);
    }

    @Unique
    private static void maplink$scale(PoseStack poseStack, float $$0, float $$1, float $$2, Operation<Void> original, Waypoint w) {
        if (w instanceof CustomWorldMapWaypoint) {
            WaypointState waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
            float scale;
            if (waypointState.renderIconOnWorldMap) {
                scale = (waypointState.isPlayer ? config.worldmap.playerIconScale : config.worldmap.markerIconScale) / 100f;
            } else {
                scale = (waypointState.isPlayer ? config.worldmap.playerTextScale : config.worldmap.markerTextScale) / 100f;
            }
            original.call(poseStack, $$0 * scale, $$1 * scale, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }

    #if MC_VER >= MC_26_1_0
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lorg/joml/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIFFFFIILcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   float x,
                                   float y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   GpuTextureView texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #elif MC_VER >= MC_1_21_5
    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lorg/joml/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIFFFFIILcom/mojang/blaze3d/textures/GpuTexture;)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   float x,
                                   float y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   GpuTexture texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lorg/joml/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIFFFFIII)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   float x,
                                   float y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   int texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #elif MC_VER >= MC_1_19_4
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lorg/joml/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIFFFFIII)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   float x,
                                   float y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   int texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #elif MC_VER >= MC_1_17_1
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lcom/mojang/math/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIFFFFIII)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   float x,
                                   float y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   int texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #else
    @WrapOperation(method = "renderElement(Lxaero/map/mods/gui/Waypoint;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lcom/mojang/math/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;IIIIIIFFFFIII)V"))
    private static void customBlit(Matrix4f matrix,
                                   MultiTextureRenderTypeRenderer renderer,
                                   int x,
                                   int y,
                                   int u,
                                   int v,
                                   int width,
                                   int height,
                                   float r,
                                   float g,
                                   float b,
                                   float a,
                                   int textureWidth,
                                   int textureHeight,
                                   int texture,
                                   Operation<Void> original,
                                   Waypoint w) {
    #endif
        WaypointState waypointState = null;
        if (w instanceof CustomWorldMapWaypoint) waypointState = ((CustomWorldMapWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnWorldMap) {
            #if MC_VER >= MC_1_21_11
            original.call(matrix, (MultiTextureRenderTypeRenderer)XaeroClientMapHandler.xaeroWorldMapSupport.getGuiNearestRenderer(), x, y, u - 1, v - 1, 64, 64, r, g, b, a, 64, 64, texture);
            #else
            original.call(matrix, renderer, x, y, u - 1, v - 1, 64, 64, r, g, b, a, 64, 64, texture);
            #endif
        } else {
            original.call(matrix, renderer, x, y, u, v, width, height, r, g, b, a, textureWidth, textureHeight, texture);
        }
    }
}
