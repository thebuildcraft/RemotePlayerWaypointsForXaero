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

package de.the_build_craft.remote_player_waypoints_for_xaero.mixins.forge;

import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * @author coolGi
 * @author cortex
 */
public class ForgeMixinPlugin implements IMixinConfigPlugin
{
	private boolean firstRun = false;
	private boolean isForgeMixinFile;
	
	
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (!this.firstRun) {
			try {
				Class<?> cls = Class.forName("net.neoforged.fml.common.Mod"); // Check if a NeoForge exclusive class exists
				this.isForgeMixinFile = false;
			} catch (ClassNotFoundException e) {
				this.isForgeMixinFile = true;
			}
		}
		if (!this.isForgeMixinFile)
			return false;
		
		if (mixinClassName.contains(".mods."))
		{ // If the mixin wants to go into a mod then we check if that mod is loaded or not
			return ModList.get().isLoaded(
					mixinClassName
							// What these 2 regex's do is get the mod name that we are checking out of the mixinClassName
							// Eg. "de.the_build_craft.remote_player_waypoints_for_xaero.mixins.mods.sodium.MixinSodiumChunkRenderer" turns into "sodium"
							.replaceAll("^.*mods.", "") // Replaces everything before the mods
							.replaceAll("\\..*$", "") // Replaces everything after the mod name
			);
		}
		
		return true;
	}
	
	
	@Override
	public void onLoad(String mixinPackage) { }
	
	@Override
	public String getRefMapperConfig() { return null; }
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
	
	@Override
	public List<String> getMixins() { return null; }
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
	
}