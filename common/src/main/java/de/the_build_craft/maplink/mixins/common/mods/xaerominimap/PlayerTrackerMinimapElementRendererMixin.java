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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
#if MC_VER < MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
#endif
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.waypoints.MutablePlayerPosition;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
import de.the_build_craft.maplink.common.clientMapHandlers.playerTracker.RemotePlayerTrackerSystem;
#if MC_VER >= MC_26_2_0
import xaero.lib.client.graphics.XaeroBufferProvider;
#else
import net.minecraft.client.renderer.MultiBufferSource;
#endif
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
#if MC_VER >= MC_1_21_6
import xaero.hud.minimap.element.render.MinimapElementGraphics;
#elif MC_VER >= MC_1_20_1
import net.minecraft.client.gui.GuiGraphics;
#endif
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(PlayerTrackerMinimapElementRenderer.class)
public class PlayerTrackerMinimapElementRendererMixin {
    #if MC_VER >= MC_26_2_0
    @WrapOperation(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;name()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_21_9
    @WrapOperation(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;name()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_21_6
    @WrapOperation(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #else
    @WrapOperation(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #endif
    private String injectDistanceText(GameProfile instance, Operation<String> original, PlayerTrackerMinimapElement<?> e) {
        if (config.hud.showTrackerDistance) {
            return RemotePlayerTrackerSystem.injectDistanceText(instance, original, new Vec3(e.getX(), e.getY(), e.getZ()));
        } else {
            return original.call(instance);
        }
    }

    #if MC_VER >= MC_26_2_0
    @Inject(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMinimapElement<?> e, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, MinimapElementGraphics guiGraphics, XaeroBufferProvider xaeroBufferProvider, CallbackInfoReturnable<Boolean> cir) {
    #elif MC_VER >= MC_1_21_6
    @Inject(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMinimapElement<?> e, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, MinimapElementGraphics guiGraphics, MultiBufferSource.BufferSource vanillaBufferSource, CallbackInfoReturnable<Boolean> cir) {
    #elif MC_VER >= MC_1_20_1
    @Inject(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMinimapElement<?> e, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, GuiGraphics guiGraphics, MultiBufferSource.BufferSource vanillaBufferSource, CallbackInfoReturnable<Boolean> cir) {
    #else
    @Inject(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMinimapElement<?> e, boolean highlighted, boolean outOfBounds, double optionalDepth, float optionalScale, double partialX, double partialY, MinimapElementRenderInfo renderInfo, PoseStack matrixStack, MultiBufferSource.BufferSource vanillaBufferSource, CallbackInfoReturnable<Boolean> cir) {
    #endif
        MutablePlayerPosition mutablePlayerPosition = XaeroClientMapHandler.hudAndMinimapPlayerTrackerPositions.get(e.getPlayerId());
        if (mutablePlayerPosition == null) return;
        WaypointState waypointState = mutablePlayerPosition.getWaypointState();
        if (waypointState == null) return;
        if (renderInfo.location == MinimapElementRenderLocation.IN_WORLD) {
            if (!(waypointState.renderOnHud && config.hud.showPlayerWaypoints.isActive())) cir.setReturnValue(false);
        } else {
            if (!(waypointState.renderOnMiniMap
                    && (!outOfBounds || config.minimap.outOfBoundsPlayerWaypoints)
                    && config.minimap.showPlayerWaypoints.isActive())) cir.setReturnValue(false);
        }
    }
}
