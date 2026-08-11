/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
 *    (some parts of this file are originally from the Distant Horizons mod by James Seibel)
 *
 *    Copyright (C) 2024 - 2026  Leander Knüttel and contributors
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

package de.the_build_craft.maplink.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import de.the_build_craft.maplink.common.clientMapHandlers.XaeroClientMapHandler;
import de.the_build_craft.maplink.common.connections.BlueMapConnection;
import de.the_build_craft.maplink.common.connections.MapConnection;
import de.the_build_craft.maplink.common.level.AreaSelection;
import de.the_build_craft.maplink.common.waypoints.Double3;
import de.the_build_craft.maplink.common.wrappers.Text;
import de.the_build_craft.maplink.common.wrappers.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;

import static de.the_build_craft.maplink.common.CommonModConfig.*;

/**
 * Base for all mod loader initializers 
 * and handles most setup.
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 11.08.2026
 */
public abstract class AbstractModInitializer
{
	public static final String MOD_ID = "maplink";
	public static final String MOD_NAME = "Map Link";
	public static final String VERSION = "4.5.1";
	public static final Logger LOGGER = LogManager.getLogger("MapLink");
	public static AbstractModInitializer INSTANCE;
	public LoaderType loaderType;

	// Update tasks
	public static int timerDelay = 1000;
	public static UpdateTask slowUpdateTask;
	private static FastUpdateTask fastUpdateTask;
	private static ScheduledFuture<?> scheduledSlowUpdateTask;
	private static ScheduledFuture<?> scheduledFastUpdateTask;

	// Connections
	private static MapConnection connection = null;
	public static boolean connected = false;
    public static boolean forceNextRefresh = false;

	// AFK detection
	public static final Map<String, Boolean> AfkMap = new ConcurrentHashMap<>();
	public static final Map<String, Long> lastPlayerActivityTimeMap = new ConcurrentHashMap<>();
	public static final Map<String, Double3> lastPlayerPosMap = new HashMap<>();
	public static final Map<String, Boolean> playerOverAfkTimeMap = new ConcurrentHashMap<>();
	public static final Map<String, Long> lastPlayerUpdateTimeMap = new HashMap<>();

	public static boolean xaeroMiniMapInstalled = false;
    public static boolean xaeroWorldMapInstalled = false;
	public static boolean overwriteCurrentDimension = false;

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	
	//==================//
	// abstract methods //
	//==================//
	
	protected abstract void createInitialBindings();
	protected abstract IEventProxy createClientProxy();
	protected abstract IEventProxy createServerProxy(boolean isDedicated);
	protected abstract void initializeModCompat();
	
	//===================//
	// initialize events //
	//===================//
	
	public void onInitializeClient()
	{
		LOGGER.info("Initializing " + MOD_NAME);

		this.startup();//<-- common mod init in here

        xaeroMiniMapInstalled = ModChecker.INSTANCE.classExists("xaero.minimap.XaeroMinimap") || ModChecker.INSTANCE.classExists("xaero.pvp.BetterPVP");
		LOGGER.info("xaeroMiniMapInstalled: " + xaeroMiniMapInstalled);
        xaeroWorldMapInstalled = ModChecker.INSTANCE.classExists("xaero.map.WorldMap");
        LOGGER.info("xaeroWorldMapInstalled: " + xaeroWorldMapInstalled);
		if (xaeroMiniMapInstalled || xaeroWorldMapInstalled) {
			new XaeroClientMapHandler();
		}

		slowUpdateTask = new UpdateTask();
		fastUpdateTask = new FastUpdateTask();
		scheduledFastUpdateTask = scheduler.scheduleAtFixedRate(fastUpdateTask::run, 0, 100, TimeUnit.MILLISECONDS);
		scheduledSlowUpdateTask = scheduler.scheduleAtFixedRate(slowUpdateTask::run, 0, timerDelay, TimeUnit.MILLISECONDS);

		this.printModInfo();

		this.createClientProxy().registerEvents();
		this.createServerProxy(false).registerEvents();

		this.initializeModCompat();

		//Client Init here

		LOGGER.info(MOD_NAME + " Initialized");
	}
	
	public void onInitializeServer()
	{
		LOGGER.info("Initializing " + MOD_NAME);
		
		this.startup();//<-- common mod init in here
		this.printModInfo();

		this.createServerProxy(true).registerEvents();

		//Server Init here

		LOGGER.info(MOD_NAME + " Initialized");
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

	public static void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher){
		LiteralArgumentBuilder<CommandSourceStack> baseCommand = literal(MOD_ID);

		LiteralArgumentBuilder<CommandSourceStack> ignoreCommand = baseCommand.then(literal("ignore_server")
				.executes(context -> {
					IgnoreServer();
					return 1;
				}));

		dispatcher.register(ignoreCommand);

		LiteralArgumentBuilder<CommandSourceStack> setAfkTimeCommand = baseCommand.then(literal("set_afk_time")
				.then(argument("player", StringArgumentType.word())
						.then(argument("time", IntegerArgumentType.integer(0))
								.executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "player");
                                    int time = IntegerArgumentType.getInteger(context, "time");
									AbstractModInitializer.lastPlayerActivityTimeMap.put(playerName, System.currentTimeMillis() - time * 1000L);
									AbstractModInitializer.AfkMap.put(playerName, time >= Math.max(30, config.general.timeUntilAfk));
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
								connection.setCurrentDimension(dimension);
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

        LiteralArgumentBuilder<CommandSourceStack> setInvisibilityRecoveryCommand = baseCommand.then(literal("set_invisibility_recovery")
                .then(argument("on", StringArgumentType.word())
                        .executes(context -> {
                            CommonModConfig.setInvisibilityRecovery(Boolean.parseBoolean(StringArgumentType.getString(context, "on")));
                            Utils.sendToClientChat("Set invisibility recovery to: " + config.general.invisibilityRecovery);
                            return 1;
                        })));

        dispatcher.register(setInvisibilityRecoveryCommand);

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
					setIgnoreMarkerMessage(true);
					Utils.sendToClientChat("You will not receive this warning again!");
					return 1;
				}));

		dispatcher.register(ignoreMarkerMessageCommand);

        LiteralArgumentBuilder<CommandSourceStack> refreshMarkersCommand = baseCommand.then(literal("refresh_markers")
                .executes(context -> {
                    forceNextRefresh = true;
                    Utils.sendToClientChat("Markers and Area Markers will refresh now.");
                    return 1;
                }));

        dispatcher.register(refreshMarkersCommand);

        LiteralArgumentBuilder<CommandSourceStack> loadMapTiles = baseCommand.then(literal("download_tiles")
                .then(argument("chunksX", IntegerArgumentType.integer(4, 2500))
                .then(argument("chunksZ", IntegerArgumentType.integer(4, 2500))
                .then(argument("center", ColumnPosArgument.columnPos())
                    .executes(context -> {
                        if (!(connection instanceof BlueMapConnection)) {
                            Utils.sendErrorToClientChat("Currently only available for Bluemap. This will be expanded in future updates ☺");
                            return 0;
                        }
                        int chunksX = IntegerArgumentType.getInteger(context, "chunksX") + 2;
                        int chunksZ = IntegerArgumentType.getInteger(context, "chunksZ") + 2;
                        BlockPos center = parseClientPos(context.getArgument("center", Coordinates.class));
                        float GB = (float) (Math.ceil(chunksX * chunksZ * 16*16 * 4 * 1e-9f * 100) / 100f);
                        Utils.sendToClientChat(Text.literal("(I recommend using the right-click option on the world map!) | This will require at least ")
                                .append(Text.literal(GB + " GB").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
                                .append(Text.literal(" of memory!")));
                        Utils.sendToClientChat("Select one of these maps to confirm:");
                        for (String[] map : connection.getPossibleTileMaps()) {
                            Utils.sendToClientChat(Text.literal("[" + map[1] + "] ").withStyle(Style.EMPTY.withClickEvent(
                                    #if MC_VER < MC_1_21_5
                                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + MOD_ID + " download_tiles " + chunksX + " " + chunksZ + " " + center.getX() + " " + center.getZ() + " " + map[0]))
                                    #else
                                    new ClickEvent.RunCommand("/" + MOD_ID + " download_tiles " + chunksX + " " + chunksZ + " " + center.getX() + " " + center.getZ() + " " + map[0]))
                                    #endif
                                    .withColor(ChatFormatting.GREEN)));
                        }
                        return 1;
                    })
                    .then(argument("map", StringArgumentType.word())
                            .executes(context -> {
                                if (!(connection instanceof BlueMapConnection)) {
                                    Utils.sendErrorToClientChat("Currently only available for Bluemap. This will be expanded in future updates ☺");
                                    return 0;
                                }
                                int chunksX = IntegerArgumentType.getInteger(context, "chunksX") + 2;
                                int chunksZ = IntegerArgumentType.getInteger(context, "chunksZ") + 2;
                                BlockPos center = parseClientPos(context.getArgument("center", Coordinates.class));
                                String map = StringArgumentType.getString(context, "map");
                                Thread thread = new Thread(() -> {
                                    Utils.sendToClientChat("converting tiles...");
                                    if (connection.downloadTiles(map, AreaSelection.fromCenter(center.getX() >> 4, center.getZ() >> 4, chunksX, chunksZ))) {
                                        Utils.sendToClientChat(Text.literal("Tiles converted. Don't forget to ")
                                                .append(Text.literal("[stop]").withStyle(Style.EMPTY.withClickEvent(
                                                        #if MC_VER < MC_1_21_5
                                                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + MOD_ID + " stop_tile_rendering"))
                                                        #else
                                                        new ClickEvent.RunCommand("/" + MOD_ID + " stop_tile_rendering"))
                                                        #endif
                                                        .withColor(ChatFormatting.RED)))
                                                .append(Text.literal(" the rendering once its finished to get your memory back!")));
                                        Utils.sendToClientChat(Text.literal("(Some chunks might be skipped by the first pass... just wait a bit longer)").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)));
                                    } else {
                                        Utils.sendErrorToClientChat("Failed to convert Map-Tiles!");
                                        XaeroClientMapHandler.xaeroWorldMapSupport.clearTiles();
                                    }
                                });
                                thread.start();
                                return 1;
                            }))))));

        dispatcher.register(loadMapTiles);

        LiteralArgumentBuilder<CommandSourceStack> stopCommand = baseCommand.then(literal("stop_tile_rendering")
                .executes(context -> {
                    XaeroClientMapHandler.xaeroWorldMapSupport.clearTiles();
                    Utils.sendToClientChat("Tile rendering stopped & cache cleared");
                    return 1;
                }));

        dispatcher.register(stopCommand);

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
    private static BlockPos parseClientPos(Coordinates coordinates) {
        WorldCoordinates worldCoordinates = (WorldCoordinates) coordinates;
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        #if MC_VER > MC_1_21_6
        return new BlockPos((int) worldCoordinates.x().get(playerPos.getX()), (int) worldCoordinates.y().get(playerPos.getY()), (int) worldCoordinates.z().get(playerPos.getZ()));
        #else
        return new BlockPos((int) worldCoordinates.x.get(playerPos.getX()), (int) worldCoordinates.y.get(playerPos.getY()), (int) worldCoordinates.z.get(playerPos.getZ()));
        #endif
    }

	/**
	 * Set how often to check for player position updates
	 *
	 * @param ms Time in seconds
	 */
	public static void setUpdateDelay(int ms) {
		int maxUpdateDelay = Math.min(4000, Math.max(config.general.maxUpdateDelay, 1000));
		ms = Math.min(maxUpdateDelay, Math.max(ms, 1000));
		if (ms == timerDelay || scheduledSlowUpdateTask == null) return;
		timerDelay = ms;
		scheduledSlowUpdateTask.cancel(true);
		scheduledSlowUpdateTask = scheduler.scheduleAtFixedRate(slowUpdateTask::run, 0, timerDelay, TimeUnit.MILLISECONDS);
		LOGGER.info("Remote update delay has been set to " + ms + " ms");
		if (config.general.debugMode) Utils.sendToClientChat("Remote update delay has been set to " + ms + " ms");
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
			if (!config.general.ignoredServers.contains(address)) config.general.ignoredServers.add(address);
			saveConfig();

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

	public static boolean checkIfInGame() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null
				&& mc.player != null
				#if MC_VER >= MC_1_21_9
				&& mc.getCameraEntity() != null
                #else
                && mc.cameraEntity != null
                #endif;
	}

	public static boolean checkIfInSingleplayer() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
	}

	public static boolean checkIfInMultiplayer() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getCurrentServer() != null
				&& mc.getConnection() != null
				&& mc.getConnection().getConnection().isConnected();
	}

	@Nullable
    @SuppressWarnings("DataFlowIssue")
	public static String getCurrentServerIP() {
		if (AbstractModInitializer.checkIfInSingleplayer()) {
			return "lan";
		} else if (AbstractModInitializer.checkIfInMultiplayer()) {
			return Minecraft.getInstance().getCurrentServer().ip.toLowerCase(Locale.ROOT);
		} else {
			return null;
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
