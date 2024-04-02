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

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tbc.remote_player_waypoints_for_xaero.*;

@Mixin(PlayerListEntry.class)
public class PlayerListMixin {
    @Inject(at = @At("RETURN"), method = "getDisplayName()Lnet/minecraft/text/Text;", cancellable = true)
    void injected(CallbackInfoReturnable<Text> cir) {
        var thisObject = (PlayerListEntry) (Object) this;
        var playerNameString = thisObject.getProfile().getName();
        MutableText playerName;
        var rv = cir.getReturnValue();
        if (rv == null) {
            playerName = Text.literal(playerNameString);
        } else {
            playerName = rv.copy();
        }

        if (!RemotePlayerWaypointsForXaero.enabled) {
            cir.setReturnValue(playerName);
            return;
        }

        if (RemotePlayerWaypointsForXaero.AfkDic.containsKey(playerNameString)) {
            if (RemotePlayerWaypointsForXaero.AfkDic.get(playerNameString)) {
                if (RemotePlayerWaypointsForXaero.showAfkTimeInTabList){
                    cir.setReturnValue(playerName.append(Text.literal("  [AFK " + (RemotePlayerWaypointsForXaero.AfkTimeDic.get(playerNameString) / 60) + " min]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.AfkColor))));
                }
                else{
                    cir.setReturnValue(playerName.append(Text.literal("  [AFK]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.AfkColor))));
                }
            }
        } else {
            cir.setReturnValue(playerName.append(Text.literal("  [???]").setStyle(Style.EMPTY.withColor(RemotePlayerWaypointsForXaero.unknownAfkStateColor))));
        }
    }
}
