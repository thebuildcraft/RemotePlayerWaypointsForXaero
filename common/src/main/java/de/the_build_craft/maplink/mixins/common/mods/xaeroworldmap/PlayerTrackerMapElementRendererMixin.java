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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.waypoints.MutablePlayerPosition;
import de.the_build_craft.maplink.common.waypoints.WaypointState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import de.the_build_craft.maplink.common.clientMapHandlers.playerTracker.RemotePlayerTrackerSystem;
#if MC_VER >= MC_26_2_0
import xaero.lib.client.graphics.XaeroBufferProvider;
#else
import net.minecraft.client.renderer.MultiBufferSource;
#endif
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
#if MC_VER >= MC_1_21_6
import xaero.map.element.MapElementGraphics;
#elif MC_VER >= MC_1_20_1
import net.minecraft.client.gui.GuiGraphics;
#endif
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.map.radar.tracker.PlayerTrackerMapElementRenderer;

import static de.the_build_craft.maplink.common.CommonModConfig.config;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(PlayerTrackerMapElementRenderer.class)
public class PlayerTrackerMapElementRendererMixin {
    #if MC_VER >= MC_26_2_0
    @WrapOperation(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;name()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_21_9
    @WrapOperation(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;name()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_21_6
    @WrapOperation(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #elif MC_VER >= MC_1_20_1
    @WrapOperation(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #else
    @WrapOperation(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getName()Ljava/lang/String;"))
    #endif
    private String injectDistanceText(GameProfile instance, Operation<String> original, PlayerTrackerMapElement<?> e) {
        if (config.worldmap.showTrackerDistance) {
            return RemotePlayerTrackerSystem.injectDistanceText(instance, original, new Vec3(e.getX(), e.getY(), e.getZ()));
        } else {
            return original.call(instance);
        }
    }

    #if MC_VER >= MC_26_2_0
    @Inject(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lxaero/lib/client/graphics/XaeroBufferProvider;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMapElement<?> e, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, MapElementGraphics guiGraphics, XaeroBufferProvider xaeroBufferProvider, MultiTextureRenderTypeRendererProvider rendererProvider, CallbackInfoReturnable<Boolean> cir) {
    #elif MC_VER >= MC_1_21_6
    @Inject(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMapElement<?> e, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, MapElementGraphics guiGraphics, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, CallbackInfoReturnable<Boolean> cir) {
    #elif MC_VER >= MC_1_20_1
    @Inject(method = "renderElement(ILxaero/map/radar/tracker/PlayerTrackerMapElement;ZLnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/GuiGraphics;DDDDFDDLnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;IDFDDZF)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(int location, PlayerTrackerMapElement<?> e, boolean hovered, Minecraft mc, GuiGraphics guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ, float brightness, double scale, double screenSizeBasedScale, TextureManager textureManager, Font fontRenderer, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex, double optionalDepth, float optionalScale, double partialX, double partialY, boolean cave, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
    #else
    @Inject(method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(value = "HEAD"), cancellable = true)
    private void cancel(PlayerTrackerMapElement<?> e, boolean hovered, double optionalDepth, float optionalScale, double partialX, double partialY, ElementRenderInfo renderInfo, PoseStack matrixStack, MultiBufferSource.BufferSource vanillaBufferSource, MultiTextureRenderTypeRendererProvider rendererProvider, CallbackInfoReturnable<Boolean> cir) {
    #endif
        MutablePlayerPosition mutablePlayerPosition = XaeroClientMapHandler.worldmapPlayerTrackerPositions.get(e.getPlayerId());
        if (mutablePlayerPosition == null) return;
        WaypointState waypointState = mutablePlayerPosition.getWaypointState();
        if (waypointState == null) return;
        if (!(waypointState.renderOnWorldMap && config.worldmap.showPlayerWaypoints.isActive())) cir.setReturnValue(false);
    }
}
