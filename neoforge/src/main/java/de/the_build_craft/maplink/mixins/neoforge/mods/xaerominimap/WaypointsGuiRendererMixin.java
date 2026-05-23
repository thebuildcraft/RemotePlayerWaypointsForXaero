/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by TheMrEngMan)
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

package de.the_build_craft.maplink.mixins.neoforge.mods.xaerominimap;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.waypoints.TempWaypoint;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
import net.minecraft.client.Minecraft;
#if MC_VER >= MC_1_21_6
import xaero.hud.minimap.element.render.MinimapElementGraphics;
#elif MC_VER >= MC_1_20_1
import net.minecraft.client.gui.GuiGraphics;
#endif
import net.minecraft.client.renderer.MultiBufferSource;
#if MC_VER >= MC_1_19_4
import org.joml.Matrix4f;
#else
import com.mojang.math.Matrix4f;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.Waypoint;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author TheMrEngMan
 * @author Leander Knüttel
 * @version 15.02.2026
 */

@Pseudo
@Mixin(WaypointMapRenderer.class)
public class WaypointsGuiRendererMixin {

    @Inject(method = "getOrder", at = @At("RETURN"), cancellable = true, remap = false)
    private void injected(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getWaypointLayerOrder());
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    #if MC_VER >= MC_1_21_6
    @ModifyExpressionValue(method = "drawIcon*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int modifyFontWidthForCustomIcons(int original,
                                              MinimapElementGraphics guiGraphics,
    #elif MC_VER >= MC_1_20_1
    @ModifyExpressionValue(method = "drawIconOnGUI(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int modifyFontWidthForCustomIcons(int original,
                                              GuiGraphics guiGraphics,
    #else
    @ModifyExpressionValue(method = "drawIconOnGUI(Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int modifyFontWidthForCustomIcons(int original,
                                              PoseStack matrixStack,
    #endif
                                              MinimapRendererHelper rendererHelper,
                                              Waypoint w) {
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnMiniMap) {
                return 7;
            } else {
                return Minecraft.getInstance().font.width(waypointState.abbreviation);
            }
        } else {
            return original;
        }
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    #if MC_VER >= MC_1_21_11
    @WrapOperation(method = "drawIcon(Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/common/minimap/waypoints/Waypoint;IIIIIIIIIFILxaero/lib/client/graphics/XaeroBufferProvider;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V"))
    private void drawCustomIcon(Matrix4f matrix,
                                VertexConsumer vertexBuffer,
                                float x,
                                float y,
                                int w,
                                int h,
                                float r,
                                float g,
                                float b,
                                float a,
                                Operation<Void> original,
                                MinimapElementGraphics guiGraphics,
                                Waypoint waypoint) {
    #elif MC_VER >= MC_1_21_6
    @WrapOperation(method = "drawIcon(Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/common/minimap/waypoints/Waypoint;IIIIIIIIIFILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V"))
    private void drawCustomIcon(Matrix4f matrix,
                                VertexConsumer vertexBuffer,
                                float x,
                                float y,
                                int w,
                                int h,
                                float r,
                                float g,
                                float b,
                                float a,
                                Operation<Void> original,
                                MinimapElementGraphics guiGraphics,
                                Waypoint waypoint) {
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "drawIconOnGUI(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V"))
    private void drawCustomIcon(Matrix4f matrix,
                                VertexConsumer vertexBuffer,
                                float x,
                                float y,
                                int w,
                                int h,
                                float r,
                                float g,
                                float b,
                                float a,
                                Operation<Void> original,
                                GuiGraphics guiGraphics,
                                MinimapRendererHelper rendererHelper,
                                Waypoint waypoint) {
    #elif MC_VER >= MC_1_19_4
    @WrapOperation(method = "drawIconOnGUI(Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V"))
    private void drawCustomIcon(Matrix4f matrix,
                                VertexConsumer vertexBuffer,
                                float x,
                                float y,
                                int w,
                                int h,
                                float r,
                                float g,
                                float b,
                                float a,
                                Operation<Void> original,
                                PoseStack matrixStack,
                                MinimapRendererHelper rendererHelper,
                                Waypoint waypoint) {
    #else
    @WrapOperation(method = "drawIconOnGUI(Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lcom/mojang/math/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V"))
    private void drawCustomIcon(Matrix4f matrix,
                                VertexConsumer vertexBuffer,
                                float x,
                                float y,
                                int w,
                                int h,
                                float r,
                                float g,
                                float b,
                                float a,
                                Operation<Void> original,
                                PoseStack matrixStack,
                                MinimapRendererHelper rendererHelper,
                                Waypoint waypoint) {
    #endif
        WaypointState waypointState = null;
        if (waypoint instanceof TempWaypoint) waypointState = ((TempWaypoint) waypoint).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnMiniMap) {
            XaeroClientMapHandler.xaeroMiniMapSupport.batchDrawCustomIcon(matrix, waypointState.getDynamicTexture(), x, y, w, h, a);
        } else {
            original.call(matrix, vertexBuffer, x, y, w, h, r, g, b, a);
        }
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    #if MC_VER >= MC_1_21_11
    @WrapOperation(method = "drawIcon(Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/common/minimap/waypoints/Waypoint;IIIIIIIIIFILxaero/lib/client/graphics/XaeroBufferProvider;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices,
                                                    String symbol,
                                                    float x,
                                                    float y,
                                                    int color,
                                                    boolean shadow,
                                                    MultiBufferSource renderTypeBuffer,
                                                    Operation<Void> original,
                                                    MinimapElementGraphics guiGraphics,
                                                    Waypoint w) {
    #elif MC_VER >= MC_1_21_6
    @WrapOperation(method = "drawIcon(Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/common/minimap/waypoints/Waypoint;IIIIIIIIIFILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices,
                                                    String symbol,
                                                    float x,
                                                    float y,
                                                    int color,
                                                    boolean shadow,
                                                    MultiBufferSource.BufferSource renderTypeBuffer,
                                                    Operation<Void> original,
                                                    MinimapElementGraphics guiGraphics,
                                                    Waypoint w) {
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "drawIconOnGUI(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices,
                                                    String symbol,
                                                    float x,
                                                    float y,
                                                    int color,
                                                    boolean shadow,
                                                    MultiBufferSource.BufferSource renderTypeBuffer,
                                                    Operation<Void> original,
                                                    GuiGraphics guiGraphics,
                                                    MinimapRendererHelper rendererHelper,
                                                    Waypoint w) {
    #else
    @WrapOperation(method = "drawIconOnGUI(Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;IIILnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices,
                                                    String symbol,
                                                    float x,
                                                    float y,
                                                    int color,
                                                    boolean shadow,
                                                    MultiBufferSource.BufferSource renderTypeBuffer,
                                                    Operation<Void> original,
                                                    PoseStack matrixStack,
                                                    MinimapRendererHelper rendererHelper,
                                                    Waypoint w) {
    #endif
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if (!waypointState.renderIconOnMiniMap) {
                original.call(matrices, waypointState.abbreviation, x, y, color, shadow, renderTypeBuffer);
            }
        } else {
            original.call(matrices, symbol, x, y, color, shadow, renderTypeBuffer);
        }
    }

    @WrapOperation(method = "renderElement*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void customScale(PoseStack poseStack,
                             float $$0,
                             float $$1,
                             float $$2,
                             Operation<Void> original,
                             Waypoint w,
                             boolean highlighted,
                             boolean outOfBounds) {
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            float scale;
            if (waypointState.renderIconOnMiniMap) {
                scale = (waypointState.isPlayer ? config.minimap.playerIconScale : config.minimap.markerIconScale) / 100f;
            } else {
                scale = (waypointState.isPlayer ? config.minimap.playerTextScale : config.minimap.markerTextScale) / 100f;
            }
            original.call(poseStack, $$0 * scale, $$1 * scale, $$2);
        } else {
            original.call(poseStack, $$0, $$1, $$2);
        }
    }

    @Inject(method = "renderElement*", at = @At("HEAD"), cancellable = true)
    private void cancel(Waypoint w,
                        boolean highlighted,
                        boolean outOfBounds,
                        double optionalDepth,
                        float optionalScale,
                        double partialX,
                        double partialY,
                        MinimapElementRenderInfo renderInfo,
                        #if MC_VER >= MC_1_21_6
                        MinimapElementGraphics guiGraphics,
                        #elif MC_VER >= MC_1_20_1
                        GuiGraphics guiGraphics,
                        #else
                        PoseStack poseStack,
                        #endif
                        MultiBufferSource.BufferSource vanillaBufferSource,
                        CallbackInfoReturnable<Boolean> cir) {
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if ((outOfBounds && waypointState.isPlayer && !config.minimap.outOfBoundsPlayerWaypoints)
                    || (outOfBounds && !waypointState.isPlayer && !config.minimap.outOfBoundsMarkerWaypoints)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "postRender", at = @At("HEAD"))
    private void callBatchRendering(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, CallbackInfo ci) {
        XaeroClientMapHandler.xaeroMiniMapSupport.drawAllCustomIcons();
    }
}
