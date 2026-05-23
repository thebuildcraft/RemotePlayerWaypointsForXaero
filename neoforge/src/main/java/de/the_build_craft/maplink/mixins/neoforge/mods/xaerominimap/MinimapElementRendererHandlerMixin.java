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

package de.the_build_craft.maplink.mixins.neoforge.mods.xaerominimap;

import com.mojang.blaze3d.pipeline.RenderTarget;
#if MC_VER > MC_1_19_4 && MC_VER < MC_1_21_6
import net.minecraft.client.gui.GuiGraphics;
#else
import com.mojang.blaze3d.vertex.PoseStack;
#endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#if MC_VER == MC_1_17_1
import xaero.common.AXaeroMinimap;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.MinimapElementRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
#else
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.MinimapElementRendererHandler;
#endif

import java.util.Collections;
import java.util.List;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * @author Leander Knüttel
 * @version 25.08.2025
 */
@Pseudo
@Mixin(MinimapElementRendererHandler.class)
public class MinimapElementRendererHandlerMixin {
    @Shadow
    @Final
    private List<MinimapElementRenderer<?, ?>> renderers;

    @Unique
    int maplink$lastOrder;

    @Inject(method = "render", at = @At("HEAD"))
    #if MC_VER >= MC_1_21_6
    void injected(Vec3 renderPos, float partialTicks, RenderTarget framebuffer, double backgroundCoordinateScale, ResourceKey<Level> mapDimension, CallbackInfo ci) {
    #elif MC_VER > MC_1_19_4
    void injected(GuiGraphics guiGraphics, Vec3 renderPos, float partialTicks, RenderTarget framebuffer, double backgroundCoordinateScale, ResourceKey<Level> mapDimension, CallbackInfo ci) {
    #elif MC_VER == MC_1_17_1
    void injected(PoseStack matrixStack, Entity renderEntity, Player player, double renderX, double renderY, double renderZ, double playerDimDiv, double ps, double pc, double zoom, boolean cave, float partialTicks, RenderTarget framebuffer, AXaeroMinimap modMain, MinimapRendererHelper helper, MultiBufferSource.BufferSource renderTypeBuffers, Font font, MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers, CallbackInfo ci) {
    #else
    void injected(PoseStack matrixStack, Vec3 renderPos, float partialTicks, RenderTarget framebuffer, double backgroundCoordinateScale, ResourceKey<Level> mapDimension, CallbackInfo ci) {
    #endif
        int order = getWaypointLayerOrder();
        if (maplink$lastOrder == order) return;
        maplink$lastOrder = order;
        Collections.sort(renderers);
    }
}
