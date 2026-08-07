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

package de.the_build_craft.maplink.mixins.common.mods.xaerominimap;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.waypoints.TempWaypoint;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
import net.minecraft.client.Minecraft;
#if MC_VER >= MC_26_2_0
import xaero.lib.client.graphics.XaeroBufferProvider;
#else
import net.minecraft.client.renderer.MultiBufferSource;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(WaypointWorldRenderer.class)
public class WaypointWorldRendererMixin {
    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    @ModifyExpressionValue(method = "renderIcon", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int modifyFontWidthForCustomIcons(int original,
                                              Waypoint w) {
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnHud) {
                return 7;
            } else {
                return Minecraft.getInstance().font.width(waypointState.abbreviation);
            }
        } else {
            return original;
        }
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    @WrapOperation(
            method = "renderIcon",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I")),
            at = @At(value = "INVOKE", target = "Lxaero/hud/minimap/waypoint/render/world/WaypointWorldRenderer;renderColorBackground(Lcom/mojang/blaze3d/vertex/PoseStack;IFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V", ordinal = 0)
    )
    private void drawCustomIcon(WaypointWorldRenderer instance,
                                PoseStack matrixStack,
                                int addedFrame,
                                float r,
                                float g,
                                float b,
                                float a,
                                VertexConsumer waypointBackgroundConsumer,
                                Operation<Void> original,
                                Waypoint w) {
        WaypointState waypointState = null;
        if (w instanceof TempWaypoint) waypointState = ((TempWaypoint) w).getWaypointState();
        if (waypointState != null && waypointState.renderIconOnHud) {
            XaeroClientMapHandler.xaeroMiniMapSupport.batchDrawCustomIcon(matrixStack.last().pose(), waypointState.getDynamicTexture(), -5 - addedFrame, -9, 9, 9, a);
        } else {
            original.call(instance, matrixStack, addedFrame, r, g, b, a, waypointBackgroundConsumer);
        }
    }

    //partially from Earthcomputer/minimap-sync licensed under the MIT License
    #if MC_VER >= MC_26_2_0
    @WrapOperation(method = "renderIcon", at = @At(value = "INVOKE", target = "Lxaero/lib/client/graphics/font/util/FontUtils;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLxaero/lib/client/graphics/XaeroBufferProvider;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices, String symbol, float x, float y, int color, boolean shadow, XaeroBufferProvider renderTypeBuffer, Operation<Void> original, @Local(argsOnly = true) Waypoint w) {
    #elif MC_VER >= MC_1_21_11
    @WrapOperation(method = "renderIcon", at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices, String symbol, float x, float y, int color, boolean shadow, MultiBufferSource renderTypeBuffer, Operation<Void> original, Waypoint w) {
    #else
    @WrapOperation(method = "renderIcon", at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"))
    private void dontDrawSymbolStringForCustomIcons(PoseStack matrices,
                                                    String symbol,
                                                    float x,
                                                    float y,
                                                    int color,
                                                    boolean shadow,
                                                    MultiBufferSource.BufferSource renderTypeBuffer,
                                                    Operation<Void> original,
                                                    Waypoint w) {
    #endif
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if (!waypointState.renderIconOnHud) {
                original.call(matrices, waypointState.abbreviation, x, y, color, shadow, renderTypeBuffer);
            }
        } else {
            original.call(matrices, symbol, x, y, color, shadow, renderTypeBuffer);
        }
    }

    @ModifyVariable(method = "renderElement*", at = @At(value = "STORE"), ordinal = 1)
    private float customScale(float original, Waypoint w) {
        if (w instanceof TempWaypoint) {
            WaypointState waypointState = ((TempWaypoint) w).getWaypointState();
            if (waypointState.renderIconOnHud) {
                return original * (waypointState.isPlayer ? config.hud.playerIconScale : config.hud.markerIconScale) / 100f;
            } else {
                return original * (waypointState.isPlayer ? config.hud.playerTextScale : config.hud.markerTextScale) / 100f;
            }
        } else {
            return original;
        }
    }

    #if MC_VER >= MC_26_2_0
    @Inject(method = "postRender", at = @At("HEAD"))
    private void postRender(MinimapElementRenderInfo renderInfo, XaeroBufferProvider xaeroBufferProvider, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, CallbackInfo ci) {
    #else
    @Inject(method = "postRender", at = @At("HEAD"))
    private void postRender(MinimapElementRenderInfo renderInfo, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, CallbackInfo ci) {
    #endif
        XaeroClientMapHandler.xaeroMiniMapSupport.drawAllCustomIcons();
    }
}
