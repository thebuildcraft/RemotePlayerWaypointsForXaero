/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
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

package de.the_build_craft.remote_player_waypoints_for_xaero.mixins.common.client;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.Style;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Leander Knüttel
 * @author MeerBiene
 * @version 22.08.2024
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {
    // duration in min
    @Unique
    private static String formatDuration(int duration) {
        int hours = (int) Math.floor((double) duration / 60);
        int minutes = duration % 60;

        if (hours == 0) {
            return minutes + " min";
        }
        if (AbstractModInitializer.hideAfkMinutes) {
            return hours + " h";
        }
        return hours + " h  " + minutes + " min";
    }


    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void injected(PlayerInfo entry, CallbackInfoReturnable<Component> cir){
        Component newText;
        String playerNameString = entry.getProfile().getName();
        if (entry.getTabListDisplayName() == null) {
            newText = ((PlayerTabOverlay)(Object)this).decorateName(entry, PlayerTeam.formatNameForTeam(entry.getTeam(), Text.literal(playerNameString)));
        } else {
            newText = ((PlayerTabOverlay)(Object)this).decorateName(entry, entry.getTabListDisplayName().copy());
        }

        if (!(AbstractModInitializer.enabled && AbstractModInitializer.connected && AbstractModInitializer.showAfkInTabList)) {
            cir.setReturnValue(newText);
            return;
        }

        if (AbstractModInitializer.AfkDic.containsKey(playerNameString)) {
            if (AbstractModInitializer.AfkDic.get(playerNameString)) {
                if (AbstractModInitializer.showAfkTimeInTabList){
                    cir.setReturnValue(newText.copy().append(Text.literal("  [AFK: " + formatDuration(AbstractModInitializer.AfkTimeDic.get(playerNameString) / 60) + "]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(AbstractModInitializer.AfkColor)))));
                }
                else{
                    cir.setReturnValue(newText.copy().append(Text.literal("  [AFK]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(AbstractModInitializer.AfkColor)))));
                }
                return;
            }
        } else {
            cir.setReturnValue(newText.copy().append(Text.literal("  [???]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(AbstractModInitializer.unknownAfkStateColor)))));
            return;
        }
        cir.setReturnValue(newText);
    }
}
