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

package de.the_build_craft.remote_player_waypoints_for_xaero.fabric.wrappers;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.ModChecker;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

/**
 * can check if a mod is installed
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 23.05.2024
 */
public class FabricModChecker extends ModChecker
{
	/**
	 * Checks if a mod is loaded
	 */
	@Override
	public boolean isModLoaded(String modid) {
		return FabricLoader.getInstance().isModLoaded(modid);
	}

	@Override
	public File modLocation(String modid) {
		return new File(FabricLoader.getInstance().getModContainer(modid).get().getOrigin().getPaths().get(0).toUri());
	}
}
