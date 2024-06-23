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

package de.the_build_craft.remote_player_waypoints_for_xaero.mixins.fabric;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * @author coolGi
 * @author cortex
 * @author Leander Knüttel
 * @version 23.06.2024
 */
public class FabricMixinPlugin implements IMixinConfigPlugin
{
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (mixinClassName.contains(".mods."))
		{ // If the mixin wants to go into a mod then we check if that mod is loaded or not
			String modId = mixinClassName
					// What these 2 regex's do is get the mod name that we are checking out of the mixinClassName
					// Eg. "de.the_build_craft.remote_player_waypoints_for_xaero.mixins.mods.sodium.MixinSodiumChunkRenderer" turns into "sodium"
					.replaceAll("^.*mods.", "") // Replaces everything before the mods
					.replaceAll("\\..*$", ""); // Replaces everything after the mod name
			boolean isModLoaded = false;
			for (String aliasId : AbstractModInitializer.getModIdAliases(modId)){
				if (FabricLoader.getInstance().isModLoaded(aliasId)) isModLoaded = true;
			}

			return isModLoaded;
		}
		return true;
	}
	
	@Override
	public void onLoad(String mixinPackage)
	{
		
	}
	
	@Override
	public String getRefMapperConfig()
	{
		return null;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
		
	}
	
	@Override
	public List<String> getMixins()
	{
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
		
	}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
		
	}
	
}