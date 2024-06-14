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

package de.the_build_craft.remote_player_waypoints_for_xaero.common.wrappers;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.chat.Style;

import java.util.function.Supplier;

/**
 * @author Leander Knüttel
 * @version 22.05.2024
 */
public class Utils {
    public static void sendToClientChat(Component text){
        Minecraft.getInstance().gui.getChat().addMessage(text);
    }

    public static void sendToClientChat(String text){
        sendToClientChat(Text.literal(text));
    }

    public static void sendErrorToClientChat(Component text){
        Minecraft.getInstance().gui.getChat().addMessage(text.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
    }

    public static void sendErrorToClientChat(String text){
        sendErrorToClientChat(Text.literal(text));
    }

    public static void SendFeedback(CommandContext<CommandSourceStack> context, Component text, boolean allowLogging){
        #if MC_VER < MC_1_20_1
		context.getSource().sendSuccess(text, allowLogging);
		#else
        Supplier<Component> supplier = () -> text;
        context.getSource().sendSuccess(supplier, allowLogging);
		#endif
    }

    public static void SendFeedback(CommandContext<CommandSourceStack> context, String text, boolean allowLogging){
        SendFeedback(context, Text.literal(text), allowLogging);
    }

    public static void SendError(CommandContext<CommandSourceStack> context, Component text, boolean allowLogging){
        #if MC_VER < MC_1_20_1
		context.getSource().sendSuccess(text.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), allowLogging);
		#else
        Supplier<Component> supplier = () -> text.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED));
        context.getSource().sendSuccess(supplier, allowLogging);
		#endif
    }

    public static void SendError(CommandContext<CommandSourceStack> context, String text, boolean allowLogging){
        SendError(context, Text.literal(text), allowLogging);
    }
}
