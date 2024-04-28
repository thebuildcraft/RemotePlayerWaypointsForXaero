/*      Remote player waypoints for Xaero's Map
        Copyright (C) 2024  Leander Knüttel
        (some parts of this file are originally from "RemotePlayers" by ewpratten)

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


package de.the_build_craft.remote_player_waypoints_for_xaero;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import de.the_build_craft.remote_player_waypoints_for_xaero.connections.MapConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;

public class RemotePlayerWaypointsForXaero {
    public static final String MOD_ID = "remote_player_waypoints_for_xaero";
    public static final String MOD_NAME = "Remote Player Waypoints For Xaero's Map";

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
    public static boolean mapModInstalled = false;
    public static LoaderType loaderType;

    public static void init() {
        mapModInstalled = IsModInstalled("xaero.minimap.XaeroMinimap") || IsModInstalled("xaero.pvp.BetterPVP");
        LOGGER.info("mapModInstalled: " + mapModInstalled);

        unknownAfkStateColor = CommonModConfig.Instance.unknownAfkStateColor();
        AfkColor = CommonModConfig.Instance.AfkColor();

        RemoteUpdateThread = new Timer(true);
        RemoteUpdateThread.scheduleAtFixedRate(updateTask, 0, CommonModConfig.Instance.updateDelay());
        TimerDelay = CommonModConfig.Instance.updateDelay();
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

    public static void IgnoreServer(){
        var server = MinecraftClient.getInstance().getCurrentServerEntry();
        if (server != null){
            var address = server.address.toLowerCase(Locale.ROOT);
            if (!CommonModConfig.Instance.ignoredServers().contains(address)) CommonModConfig.Instance.ignoredServers().add(address);
            CommonModConfig.Instance.saveConfig();

            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("You will not receive this warning again!"));
        }
        else{
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("This can only be executed on a server!"));
        }
    }

    public static boolean IsModInstalled(String modMainClassName){
        try {
            var ClassTest = Class.forName(modMainClassName);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public enum LoaderType{
        Fabric,
        Quilt,
        Forge,
        NeoForge;
        LoaderType(){
        }
    }

    public static void register(CommandDispatcher<SuggestionProviders> dispatcher){
        LiteralArgumentBuilder<SuggestionProviders> ignoreCommand = literal("ignore_server")
                .executes(context -> {IgnoreServer(); return 1;});

        dispatcher.register(ignoreCommand);

        LiteralArgumentBuilder<SuggestionProviders> setAfkTimeCommand = literal("set_afk_time")
                .then(argument("player", StringArgumentType.word())
                        .then(argument("time", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    var playerName = StringArgumentType.getString(context, "player");
                                    var time = IntegerArgumentType.getInteger(context, "time");
                                    RemotePlayerWaypointsForXaero.AfkTimeDic.put(playerName, time);
                                    RemotePlayerWaypointsForXaero.AfkDic.put(playerName, time > 0);
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Set AFK time for " + playerName + " to " + time));
                                    return 1;
                                })));

        dispatcher.register(setAfkTimeCommand);
    }
    private static LiteralArgumentBuilder<SuggestionProviders> literal(String string) {
        return LiteralArgumentBuilder.literal(string);
    }
    public static <T> RequiredArgumentBuilder<SuggestionProviders, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
