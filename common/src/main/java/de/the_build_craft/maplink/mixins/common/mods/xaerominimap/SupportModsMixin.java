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

import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.clientMapHandlers.*;
import de.the_build_craft.maplink.common.clientMapHandlers.playerTracker.MiniMapPlayerTrackerReader;
import de.the_build_craft.maplink.common.clientMapHandlers.playerTracker.MiniMapPlayerTrackerSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.mods.SupportMods;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
@Pseudo
@Mixin(SupportMods.class)
public class SupportModsMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(IXaeroMinimap modMain, CallbackInfo ci) {
        try {
            XaeroClientMapHandler.xaeroMiniMapSupport = new XaeroMiniMapSupport();
            #if MC_VER >= MC_1_21_5
            modMain.getRenderedPlayerTrackerManager().register(
            #else
            modMain.getPlayerTracker().register(
            #endif
                    AbstractModInitializer.MOD_ID, new MiniMapPlayerTrackerSystem(
                            new MiniMapPlayerTrackerReader(), XaeroClientMapHandler.hudAndMinimapPlayerTrackerPositions));
        } catch (Exception e) {
            AbstractModInitializer.LOGGER.error("Error initializing XaeroMiniMap Support!", e);
        }
    }
}
