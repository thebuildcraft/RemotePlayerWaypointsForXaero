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


package de.the_build_craft.remote_player_waypoints_for_xaero.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.the_build_craft.remote_player_waypoints_for_xaero.RemotePlayerWaypointsForXaero;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public final class RemotePlayerWaypointsForXaeroFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        RemotePlayerWaypointsForXaero.loaderType = RemotePlayerWaypointsForXaero.LoaderType.Fabric;
        var config = new CommonModConfigFabric();

        // Run our common setup.
        RemotePlayerWaypointsForXaero.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("ignore_server")
                .executes(context -> {
                            RemotePlayerWaypointsForXaero.IgnoreServer();
                            return 1;
                        }
                )));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("set_afk_time")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .then(ClientCommandManager.argument("time", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                            var playerName = StringArgumentType.getString(context, "player");
                                            var time = IntegerArgumentType.getInteger(context, "time");
                                            RemotePlayerWaypointsForXaero.AfkTimeDic.put(playerName, time);
                                            RemotePlayerWaypointsForXaero.AfkDic.put(playerName, time > 0);
                                            context.getSource().sendFeedback(Text.literal("Set AFK time for " + playerName + " to " + time));
                                            return 1;
                                        }
                                )))));
    }
}
