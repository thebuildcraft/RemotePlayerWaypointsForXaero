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

package de.the_build_craft.remote_player_waypoints_for_xaero.quilt;

import com.mojang.brigadier.CommandDispatcher;
import de.the_build_craft.remote_player_waypoints_for_xaero.RemotePlayerWaypointsForXaero;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
//import org.quiltmc.qsl.command.api.client.*;

public final class RemotePlayerWaypointsForXaeroQuilt implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer mod) {
        RemotePlayerWaypointsForXaero.loaderType = RemotePlayerWaypointsForXaero.LoaderType.Quilt;
        var config = new CommonModConfigQuilt();

        // Run our common setup.
        RemotePlayerWaypointsForXaero.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> RemotePlayerWaypointsForXaero.register((CommandDispatcher) dispatcher));
    }
}
