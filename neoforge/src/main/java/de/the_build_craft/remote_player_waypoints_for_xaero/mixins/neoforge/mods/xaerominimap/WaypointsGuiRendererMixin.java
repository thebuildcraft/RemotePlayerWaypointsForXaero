/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from "RemotePlayers" by TheMrEngMan)
 *
 *    Copyright (C) 2024  Leander Knüttel
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

package de.the_build_craft.remote_player_waypoints_for_xaero.mixins.neoforge.mods.xaerominimap;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.CommonModConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.render.WaypointsGuiRenderer;
import xaero.common.settings.ModSettings;

/**
 * @author TheMrEngMan
 * @author Leander Knüttel
 * @version 06.07.2024
 */

@Pseudo
@Mixin(WaypointsGuiRenderer.class)
public class WaypointsGuiRendererMixin {

    @Inject(method = "getOrder", at = @At("RETURN"), cancellable = true, remap = false)
    private void injected(CallbackInfoReturnable<Integer> cir) {
        CommonModConfig.WaypointRenderBelowMode waypointRenderBelowMode = CommonModConfig.Instance.minimapWaypointsRenderBelow();
        #if MC_VER > MC_1_16_5
        boolean playerListDown = Minecraft.getInstance().options.keyPlayerList.isDown() || ModSettings.keyAlternativeListPlayers.isDown();
        #else
        boolean playerListDown = Minecraft.getInstance().options.keyPlayerList.isDown();
        #endif

        if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.ALWAYS) {
            cir.setReturnValue(-1);
        } else if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.WHEN_PLAYER_LIST_SHOWN) {
            if (playerListDown) cir.setReturnValue(-1);
        } else if (waypointRenderBelowMode == CommonModConfig.WaypointRenderBelowMode.WHEN_PLAYER_LIST_HIDDEN) {
            if (!playerListDown) cir.setReturnValue(-1);
        }
    }

}
