/*
 *    This file is part of the Remote player waypoints for Xaero's Map mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from the Distant Horizons mod by James Seibel)
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

package de.the_build_craft.remote_player_waypoints_for_xaero.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.connections.MapConnection;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.commands.CommandSourceStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;

/**
 * Base for all mod loader initializers 
 * and handles most setup.
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 17.02.2025
 */
public abstract class AbstractModInitializer
{
	public static final String MOD_ID = "remote_player_waypoints_for_xaero";
	public static final String MOD_NAME = "Remote Player Waypoints For Xaero's Map";
	public static final String VERSION = "3.3.0";
	public static final Logger LOGGER = LogManager.getLogger("RemotePlayerWaypointsForXaero");
	public static AbstractModInitializer INSTANCE;

	// Update task
	private static UpdateTask updateTask;
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
	public static boolean showAfkInTabList = true;
	public static boolean showAfkTimeInTabList = true;
	public static boolean hideAfkMinutes = false;

	public static boolean enabled = true;
	public static boolean mapModInstalled = false;
	public static boolean overwriteCurrentDimension = false;

	public static HashMap<ClientLevel, HashMap<String, RemotePlayer>> fakePlayerEntities = new HashMap<>();
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract void createInitialBindings();
	protected abstract IEventProxy createClientProxy();
	protected abstract IEventProxy createServerProxy(boolean isDedicated);
	protected abstract void initializeModCompat();
	
	//protected abstract void subscribeClientStartedEvent(Runnable eventHandler);
	//protected abstract void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler);
	//protected abstract void runDelayedSetup();

	public LoaderType loaderType;
	
	//===================//
	// initialize events //
	//===================//
	
	public void onInitializeClient()
	{
		LOGGER.info("Initializing " + MOD_NAME);

		this.startup();//<-- common mod init in here

		mapModInstalled = ModChecker.INSTANCE.classExists("xaero.minimap.XaeroMinimap") || ModChecker.INSTANCE.classExists("xaero.pvp.BetterPVP");
		LOGGER.info("mapModInstalled: " + mapModInstalled);

		unknownAfkStateColor = CommonModConfig.Instance.unknownAfkStateColor();
		AfkColor = CommonModConfig.Instance.AfkColor();

		RemoteUpdateThread = new Timer(true);
		updateTask = new UpdateTask();
		RemoteUpdateThread.scheduleAtFixedRate(updateTask, 0, CommonModConfig.Instance.updateDelay());
		TimerDelay = CommonModConfig.Instance.updateDelay();

		this.printModInfo();

		this.createClientProxy().registerEvents();
		this.createServerProxy(false).registerEvents();

		this.initializeModCompat();
		this.initConfig();

		//Client Init here

		LOGGER.info(MOD_NAME + " Initialized");

		//this.subscribeClientStartedEvent(this::postInit);
	}
	
	public void onInitializeServer()
	{
		LOGGER.info("Initializing " + MOD_NAME);
		
		this.startup();//<-- common mod init in here
		this.printModInfo();

		this.createServerProxy(true).registerEvents();

		this.initConfig();

		//Server Init here

		LOGGER.info(MOD_NAME + " Initialized");

		/*this.subscribeServerStartingEvent(server ->
		{
			this.postInit();
			
			LOGGER.info("Dedicated server initialized at " + server.getServerDirectory());
		});*/
	}
	
	//===========================//
	// inner initializer methods //
	//===========================//

	/**
	 * common mod init for client and server
	 */
	private void startup()
	{
		INSTANCE = this;
		this.createInitialBindings();
		//do common mod init here
	}
	
	private void printModInfo()
	{
		LOGGER.info(MOD_NAME + ", Version: " + VERSION);
	}
	
	private void initConfig()
	{

	}
	
	/*private void postInit()
	{
		LOGGER.info("Post-Initializing Mod");
		this.runDelayedSetup();
		LOGGER.info("Mod Post-Initialized");
	}*/

	public static void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher){
		LiteralArgumentBuilder<CommandSourceStack> baseCommand = literal(MOD_ID);

		LiteralArgumentBuilder<CommandSourceStack> ignoreCommand = baseCommand.then(literal("ignore_server")
				.executes(context -> {IgnoreServer(); return 1;}));

		dispatcher.register(ignoreCommand);

		LiteralArgumentBuilder<CommandSourceStack> setAfkTimeCommand = baseCommand.then(literal("set_afk_time")
				.then(argument("player", StringArgumentType.word())
						.then(argument("time", IntegerArgumentType.integer(0))
								.executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    int time = IntegerArgumentType.getInteger(context, "time");
									AbstractModInitializer.AfkTimeDic.put(playerName, time);
									AbstractModInitializer.AfkDic.put(playerName, time > 0);
									Utils.sendToClientChat("Set AFK time for " + playerName + " to " + time);
									return 1;
								}))));

		dispatcher.register(setAfkTimeCommand);

		LiteralArgumentBuilder<CommandSourceStack> setCurrentDimensionCommand = baseCommand.then(literal("set_current_dimension")
				.then(argument("dimension", StringArgumentType.word())
						.executes(context -> {
							if (connection == null){
								Utils.sendErrorToClientChat("Not connected to a server!");
							}
							else{
								String dimension = StringArgumentType.getString(context, "dimension");
								connection.currentDimension = dimension;
								Utils.sendToClientChat("Set current-dimension to: " + dimension);
							}
							return 1;
						})));

		dispatcher.register(setCurrentDimensionCommand);

		LiteralArgumentBuilder<CommandSourceStack> setCurrentDimensionOverwriteCommand = baseCommand.then(literal("set_current_dimension_overwrite")
				.then(argument("on", StringArgumentType.word())
						.executes(context -> {
							overwriteCurrentDimension = Boolean.parseBoolean(StringArgumentType.getString(context, "on"));
							Utils.sendToClientChat("Set dimension-overwrite to: " + overwriteCurrentDimension);
							return 1;
						})));

		dispatcher.register(setCurrentDimensionOverwriteCommand);

		LiteralArgumentBuilder<CommandSourceStack> openOnlineMapConfig = baseCommand.then(literal("open_online_map_config")
				.executes(context -> {
					if (connection == null){
						Utils.sendErrorToClientChat("Not connected to a server!");
					}
					else{
						connection.OpenOnlineMapConfig();
					}
					return 1;
				}));

		dispatcher.register(openOnlineMapConfig);

		LiteralArgumentBuilder<CommandSourceStack> ignoreMarkerMessageCommand = baseCommand.then(literal("ignore_marker_message")
				.executes(context -> {
					CommonModConfig.Instance.setIgnoreMarkerMessage(true);
					Utils.sendToClientChat("You will not receive this warning again!");
					return 1;
				}));

		dispatcher.register(ignoreMarkerMessageCommand);

		//register client commands here
	}

	public static void registerServerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean allOrDedicated) {
		//register server commands here
	}

	private static LiteralArgumentBuilder<CommandSourceStack> literal(String string) {
		return LiteralArgumentBuilder.literal(string);
	}
	private static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
		return RequiredArgumentBuilder.argument(name, type);
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
		AbstractModInitializer.connection = connection;
	}

	/**
	 * Gets the current dynmap connection
	 *
	 * @return Connection
	 */
	public static @Nullable MapConnection getConnection() {
		return AbstractModInitializer.connection;
	}

	public static void IgnoreServer(){
        ServerData server = Minecraft.getInstance().getCurrentServer();
		if (server != null){
            String address = server.ip.toLowerCase(Locale.ROOT);
			if (!CommonModConfig.Instance.ignoredServers().contains(address)) CommonModConfig.Instance.ignoredServers().add(address);
			CommonModConfig.Instance.saveConfig();

			Utils.sendToClientChat("You will not receive this warning again!");
		}
		else{
			Utils.sendToClientChat("This can only be executed on a server!");
		}
	}

	public static String[] getModIdAliases(String id){
		HashMap<String, String[]> modIdAliases = new HashMap<>();

		modIdAliases.put("xaerominimap", new String[]{"xaerominimapfair", "xaerobetterpvp", "xaerobetterpvpfair"});

		if (modIdAliases.containsKey(id)){
			return modIdAliases.get(id);
		}
		else{
			return new String[]{id};
		}
	}
	
	//================//
	// helper classes //
	//================//
	
	public interface IEventProxy
	{
		void registerEvents();
	}
}
