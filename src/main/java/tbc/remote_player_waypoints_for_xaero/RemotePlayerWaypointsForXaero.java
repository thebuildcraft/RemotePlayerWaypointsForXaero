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

package tbc.remote_player_waypoints_for_xaero;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbc.remote_player_waypoints_for_xaero.connections.MapConnection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;

public class RemotePlayerWaypointsForXaero implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("RemotePlayerWaypointsForXaero");

	// Update task
	private static UpdateTask updateTask = new UpdateTask();
	private static Timer RemoteUpdateThread = null;
	public static int TimerDelay;

	// Connections
	private static MapConnection connection = null;
	public static boolean connected = false;

	// AFK detection
	public static HashMap<String, Boolean> AfkDic = new HashMap<>();
	public static HashMap<String, Integer> AfkTimeDic = new HashMap<>();
	public static HashMap<String, PlayerPosition> lastPlayerDataDic = new HashMap<>();
	public static int unknownAfkStateColor = 0x606060;
	public static int AfkColor = 0xFF5500;
	public static boolean showAfkTimeInTabList = true;

	public static boolean enabled = true;

	@Override
	public void onInitialize() {
		LOGGER.info("RemotePlayerWaypointsForXaero starting ...");

		AutoConfig.register(ModConfig.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
		ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		unknownAfkStateColor = config.general.unknownAfkStateColor;
		AfkColor = config.general.AfkColor;

		RemoteUpdateThread = new Timer(true);
		RemoteUpdateThread.scheduleAtFixedRate(updateTask, 0, config.general.updateDelay);
		TimerDelay = config.general.updateDelay;

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("ignore_server")
				.executes(context -> {
							IgnoreServer(context);
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

		LOGGER.info("RemotePlayerWaypointsForXaero started");
	}

	/**
	 * Set how often to check for player position updates
	 *
	 * @param ms Time in seconds
	 */
	public static void setUpdateDelay(int ms) {
		if (RemoteUpdateThread == null ) return;
		updateTask.cancel();
		updateTask = new UpdateTask();
		RemoteUpdateThread.scheduleAtFixedRate(updateTask, 0, ms);
		TimerDelay = ms;
		LOGGER.info("Remote update delay has been set to " + ms + " ms");
	}

	/**
	 * Sets the current dynmap connection
	 *
	 * @param connection Connection
	 */
	public static void setConnection(MapConnection connection) {
		connected = connection != null;
		RemotePlayerWaypointsForXaero.connection = connection;
	}

	/**
	 * Gets the current dynmap connection
	 *
	 * @return Connection
	 */
	public static @Nullable MapConnection getConnection() {
		return RemotePlayerWaypointsForXaero.connection;
	}

	public static void IgnoreServer(CommandContext<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> context){
		var server = MinecraftClient.getInstance().getCurrentServerEntry();
		if (server != null){
			ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
			var address = server.address.toLowerCase(Locale.ROOT);
			if (!config.general.ignoredServers.contains(address)) config.general.ignoredServers.add(address);
			context.getSource().sendFeedback(Text.literal("You will not receive this warning again!"));
		}
		else{
			context.getSource().sendFeedback(Text.literal("This can only be executed on a server!"));
		}
	}
}