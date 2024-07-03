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

import net.minecraft.network.chat.*;

/**
 * @author Leander Knüttel
 * @version 03.07.2024
 */
public class Text {
    #if MC_VER > MC_1_18_2
    public static MutableComponent literal(String string) {
        return Component.literal(string);
    }

    public static MutableComponent translatable(String translateKey) {
        return Component.translatable(translateKey);
    }

    //TODO implement Style functions & Actions
    #else
    public static MutableComponent literal(String string) {
        return new TextComponent(string);
    }

    public static MutableComponent translatable(String translateKey) {
        return new TranslatableComponent(translateKey);
    }

    //TODO implement Style functions & Actions (write to chat, if not supported in current version)
    #endif
}
