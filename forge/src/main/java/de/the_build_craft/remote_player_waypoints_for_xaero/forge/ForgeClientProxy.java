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

package de.the_build_craft.remote_player_waypoints_for_xaero.forge;

import com.mojang.brigadier.CommandDispatcher;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import net.minecraft.commands.CommandSourceStack;
#if MC_VER > MC_1_17_1
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
#endif
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

/**
 * This handles all events sent to the client
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 23.05.2024
 */
public class ForgeClientProxy implements AbstractModInitializer.IEventProxy
{
	private static final Logger LOGGER = AbstractModInitializer.LOGGER;
	
	@Override
	public void registerEvents()
	{
		LOGGER.info("Registering Forge Client Events");

		#if MC_VER > MC_1_17_1
		//TODO remove #if once more Events are registered
		// (Forge throws an error if this line is there, but no event is registered)
		MinecraftForge.EVENT_BUS.register(this);
		#endif

		//OR register Forge Client Events here
	}

	#if MC_VER > MC_1_17_1
	@SubscribeEvent
	public void registerClientCommands(RegisterClientCommandsEvent event) {
		ForgeMain.registerClientCommands((CommandDispatcher<CommandSourceStack>) (CommandDispatcher<?>) event.getDispatcher());
	}
	#endif
}
