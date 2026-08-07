/*
 *    This file is part of the Map Link mod
 *    licensed under the GNU GPL v3 License.
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

package de.the_build_craft.maplink.common.wrappers;

import de.the_build_craft.maplink.common.AbstractModInitializer;
import de.the_build_craft.maplink.common.CommonModConfig;
import de.the_build_craft.maplink.common.MainThreadTaskQueue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.chat.Style;

import java.util.function.Supplier;

/**
 * @author Leander Knüttel
 * @version 06.08.2026
 */
public class Utils {
    public static void sendToClientChat(Component text){
        if (Minecraft.getInstance().level == null) {
            AbstractModInitializer.LOGGER.warn("Caught client chat message outside the game:\n{}", text.getString());
        } else {
            #if MC_VER >= MC_26_2_0
            MainThreadTaskQueue.queueTask(() -> Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(text));
            #elif MC_VER >= MC_26_1_0
            MainThreadTaskQueue.queueTask(() -> Minecraft.getInstance().gui.getChat().addClientSystemMessage(text));
            #else
            MainThreadTaskQueue.queueTask(() -> Minecraft.getInstance().gui.getChat().addMessage(text));
            #endif
        }
    }

    public static void sendToClientChat(String text){
        sendToClientChat(Text.literal(text));
    }

    public static void sendErrorToClientChat(Component text){
        if (CommonModConfig.config.general.hideAllChatErrors) return;
        sendToClientChat(text.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
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
