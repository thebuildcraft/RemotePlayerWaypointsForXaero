/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <https://www.gnu.org/licenses/>.*/


package tbc.remote_player_waypoints_for_xaero.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tbc.remote_player_waypoints_for_xaero.RemotePlayerWaypointsForXaero;




@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    // duration in min
    private static String formatDuration(int duration) {
        var hours = 0;
        var minutes = 0;

        while (duration > 60) {
            hours += 1;
            duration = duration - 60;
        }
        minutes = duration;
        if (hours == 0) {
            return  minutes + " min"
        } else {
            return hours + " h " + minutes + "min";
        }
    }


    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void injected(PlayerListEntry entry, CallbackInfoReturnable<Text> cir){
        Text newText;
        var playerNameString = entry.getProfile().getName();
        if (entry.getDisplayName() == null) {
            newText = ((PlayerListHud)(Object)this).applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), Text.literal(playerNameString)));
        } else {
            newText = ((PlayerListHud)(Object)this).applyGameModeFormatting(entry, entry.getDisplayName().copy());
        }

        if (!(RemotePlayerWaypointsForXaero.enabled && RemotePlayerWaypointsForXaero.connected)) {
            cir.setReturnValue(newText);
            return;
        }

        if (RemotePlayerWaypointsForXaero.AfkDic.containsKey(playerNameString)) {
            if (RemotePlayerWaypointsForXaero.AfkDic.get(playerNameString)) {
                if (RemotePlayerWaypointsForXaero.showAfkTimeInTabList){
                    cir.setReturnValue(newText.copy().append(Text.literal("  [AFK " + formatDuration(RemotePlayerWaypointsForXaero.AfkTimeDic.get(playerNameString) / 60) + " min]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.AfkColor))));
                }
                else{
                    cir.setReturnValue(newText.copy().append(Text.literal("  [AFK]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.AfkColor))));
                }
                return;
            }
        } else {
            cir.setReturnValue(newText.copy().append(Text.literal("  [???]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.unknownAfkStateColor))));
            return;
        }
        cir.setReturnValue(newText);
    }
}
