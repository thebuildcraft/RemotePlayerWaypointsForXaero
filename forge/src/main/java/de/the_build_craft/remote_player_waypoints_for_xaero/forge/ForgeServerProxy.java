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
import net.minecraft.commands.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

/**
 * This handles all events sent to the server
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 22.05.2024
 */
public class ForgeServerProxy implements AbstractModInitializer.IEventProxy
{
	private static final Logger LOGGER = AbstractModInitializer.LOGGER;

	private final boolean isDedicated;

	public ForgeServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
	}
	
	@Override
	public void registerEvents()
	{
		LOGGER.info("Registering Forge Server Events");

		MinecraftForge.EVENT_BUS.register(this);

		//OR register Forge Server Events here
	}

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event){
		#if MC_VER > MC_1_18_2
		ForgeMain.registerServerCommands((CommandDispatcher<CommandSourceStack>) (CommandDispatcher<?>) event.getDispatcher(),
				(event.getCommandSelection() == Commands.CommandSelection.ALL) || (event.getCommandSelection() == Commands.CommandSelection.DEDICATED));
		#else
		ForgeMain.registerServerCommands((CommandDispatcher<CommandSourceStack>) (CommandDispatcher<?>) event.getDispatcher(),
				(event.getEnvironment() == Commands.CommandSelection.ALL) || (event.getEnvironment() == Commands.CommandSelection.DEDICATED));
		#endif
	}
}
