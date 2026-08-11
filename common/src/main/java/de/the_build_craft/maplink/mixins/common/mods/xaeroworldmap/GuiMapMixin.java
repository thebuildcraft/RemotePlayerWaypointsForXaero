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

package de.the_build_craft.maplink.mixins.common.mods.xaeroworldmap;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.connections.BlueMapConnection;
import de.the_build_craft.maplink.common.level.*;
import net.minecraft.client.gui.Font;
#if MC_VER < MC_26_1_0
import net.minecraft.client.gui.GuiGraphics;
#else
import net.minecraft.client.gui.GuiGraphicsExtractor;
#endif
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.gui.GuiMap;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

/**
 * @author Leander Knüttel
 * @version 11.08.2026
 */
@Pseudo
@Mixin(GuiMap.class)
public class GuiMapMixin {
    @Shadow
    private MapTileSelection mapTileSelection;

    @Shadow
    private GuiRightClickMenu rightClickMenu;

    @ModifyExpressionValue(
            method = "getRightClickOptions",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z",
                    ordinal = 1
            )
    )
    private boolean injectCustomOption(boolean original, @Local(name = "options") ArrayList<RightClickOption> options) {
        if (!AbstractModInitializer.connected || AbstractModInitializer.getConnection() == null
                || !(AbstractModInitializer.getConnection() instanceof BlueMapConnection)) return original;

        int minChunkX = this.mapTileSelection.getLeft() - 1;
        int minChunkZ = this.mapTileSelection.getTop() - 1;
        int maxChunkX = this.mapTileSelection.getRight() + 1;
        int maxChunkZ = this.mapTileSelection.getBottom() + 1;
        AreaSelection areaSelection = new AreaSelection(minChunkX, minChunkZ, maxChunkX, maxChunkZ);

        GuiMap t = (GuiMap)(Object)this;

        if (ProgressCounter.converting.get() || ProgressCounter.readyForRender.get()) {
            return options.add(new RightClickOption("STOP Map Tiles Download", options.size(), new EmptyRightClickableElement()) {
                @Override
                public void onAction(Screen screen) {
                    if (rightClickMenu != null) {
                        rightClickMenu.setClosed(true);
                    }
                    new Thread(TileConverter::clear).start();
                }
            }) || original;
        }

        long requiredBytes = (long)areaSelection.chunksX * areaSelection.chunksZ * 16 * 16 * 4;
        boolean areaToLarge = requiredBytes > Runtime.getRuntime().maxMemory() * 0.9;
        String errorMessage = "Not enough RAM for download!";
        if (((long)areaSelection.chunksX * (long)areaSelection.chunksZ) > 8_000_000) {
            areaToLarge = true;
            errorMessage = "Area to large to download!";
        }

        if (areaToLarge) {
            return options.add(new RightClickOption(errorMessage, options.size(), new EmptyRightClickableElement()) {
                @Override
                public void onAction(Screen screen) {
                    if (rightClickMenu != null) {
                        rightClickMenu.setClosed(true);
                    }
                }
            }) || original;
        }

        return options.add(new RightClickOption("Download Map Tiles", options.size(), new DimensionSelectionRightClickableElement(areaSelection)) {
            @Override
            public void onAction(Screen screen) {
                int x = 0;
                int y = 0;
                if (rightClickMenu != null) {
                    x = rightClickMenu.getX();
                    y = rightClickMenu.getY();
                    rightClickMenu.setClosed(true);
                }
                rightClickMenu = GuiRightClickMenu.getMenu(target, t, x, y, 250);
            }
        }) || original;
    }

    #if MC_VER >= MC_26_1_0
    @WrapOperation(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFF)V",
                    ordinal = 2))
    private void drawAndIncrement(GuiGraphicsExtractor guiGraphics, Font font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, Operation<Void> original) {
        if (ProgressCounter.converting.get() || ProgressCounter.readyForRender.get()) {
            String progressString = ProgressCounter.getProgressString();
            if (progressString != null) string = progressString;
        }
        original.call(guiGraphics, font, string, x, y, color, bgRed, bgGreen, bgBlue, bgAlpha);
    }
    #elif MC_VER >= MC_1_21_6
    @WrapOperation(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFF)V",
                    ordinal = 2))
    private void drawAndIncrement(GuiGraphics guiGraphics, Font font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, Operation<Void> original) {
        if (ProgressCounter.converting.get() || ProgressCounter.readyForRender.get()) {
            String progressString = ProgressCounter.getProgressString();
            if (progressString != null) string = progressString;
        }
        original.call(guiGraphics, font, string, x, y, color, bgRed, bgGreen, bgBlue, bgAlpha);
    }
    #else
    @WrapOperation(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V",
                    ordinal = 2))
    private void drawAndIncrement(GuiGraphics guiGraphics, Font font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha, VertexConsumer backgroundVertexBuffer, Operation<Void> original) {
        if (ProgressCounter.converting.get() || ProgressCounter.readyForRender.get()) {
            String progressString = ProgressCounter.getProgressString();
            if (progressString != null) string = progressString;
        }
        original.call(guiGraphics, font, string, x, y, color, bgRed, bgGreen, bgBlue, bgAlpha, backgroundVertexBuffer);
    }
    #endif
}
