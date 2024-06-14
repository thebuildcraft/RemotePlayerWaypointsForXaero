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

package de.the_build_craft.remote_player_waypoints_for_xaero.fabric;

import com.mojang.brigadier.CommandDispatcher;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import net.minecraft.commands.CommandSourceStack;
#if MC_VER > MC_1_18_2
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
#else
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.Logger;

/**
 * This handles all events sent to the server
 *
 * @author Ran
 * @author Tomlee
 * @author Leander Knüttel
 * @version 22.05.2024
 */
public class FabricServerProxy implements AbstractModInitializer.IEventProxy
{
	private static final Logger LOGGER = AbstractModInitializer.LOGGER;

	private final boolean isDedicated;

	public void registerEvents()
	{
		LOGGER.info("Registering Fabric Server Events");

		#if MC_VER > MC_1_18_2
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				FabricMain.registerServerCommands((CommandDispatcher<CommandSourceStack>) (CommandDispatcher<?>) dispatcher,
						(environment == Commands.CommandSelection.ALL) || (environment == Commands.CommandSelection.DEDICATED)));
		#else
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
				FabricMain.registerServerCommands(((CommandDispatcher<CommandSourceStack>) (CommandDispatcher<?>) dispatcher),
						dedicated));
		#endif

		//register Fabric Server Events here
	}

	public FabricServerProxy(boolean isDedicated)
	{
		this.isDedicated = isDedicated;
	}

	private boolean isValidTime()//TODO is this needed???
	{
		if (isDedicated)
		{
			return true;
		}

		//FIXME: This may cause init issue...
		return !(Minecraft.getInstance().screen instanceof TitleScreen);
	}
}
