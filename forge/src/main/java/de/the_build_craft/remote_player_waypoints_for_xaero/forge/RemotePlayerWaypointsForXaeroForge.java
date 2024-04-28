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


package de.the_build_craft.remote_player_waypoints_for_xaero.forge;

import com.mojang.brigadier.CommandDispatcher;
import de.the_build_craft.remote_player_waypoints_for_xaero.RemotePlayerWaypointsForXaero;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(RemotePlayerWaypointsForXaero.MOD_ID)
public final class RemotePlayerWaypointsForXaeroForge {
    public RemotePlayerWaypointsForXaeroForge() {
        RemotePlayerWaypointsForXaero.loaderType = RemotePlayerWaypointsForXaero.LoaderType.Forge;
        var config = new CommonModConfigForge();

        // Run our common setup.
        RemotePlayerWaypointsForXaero.init();

        MinecraftForge.EVENT_BUS.addListener(this::onCommandRegister);
    }
    public void onCommandRegister(RegisterClientCommandsEvent event) {
        RemotePlayerWaypointsForXaero.register((CommandDispatcher) event.getDispatcher());
    }
}
