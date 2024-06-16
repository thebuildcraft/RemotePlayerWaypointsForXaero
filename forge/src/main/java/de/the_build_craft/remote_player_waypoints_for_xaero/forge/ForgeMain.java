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

import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.LoaderType;
import de.the_build_craft.remote_player_waypoints_for_xaero.forge.wrappers.ForgeModChecker;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
#if MC_VER == MC_1_16_5
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
#elif MC_VER == MC_1_17_1
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
#else
#endif
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// these imports change due to forge refactoring classes in 1.19
#if MC_VER < MC_1_19_2
//import net.minecraftforge.client.model.data.ModelDataMap;

import java.util.Random;
#else
#endif

/**
 * main entry point on Forge
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 16.06.2024
 */
@Mod(AbstractModInitializer.MOD_ID)
public class ForgeMain extends AbstractModInitializer
{
	public ForgeMain()
	{
		loaderType = LoaderType.Forge;
        CommonModConfigForge config = new CommonModConfigForge();

		// Register the mod initializer (Actual event registration is done in the different proxies)
		FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent e) -> this.onInitializeClient());
		FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLDedicatedServerSetupEvent e) -> this.onInitializeServer());
	}

	@Override
	public void onInitializeClient(){
		super.onInitializeClient();

		//Forge Client init here
	}

	@Override
	public void onInitializeServer(){
		super.onInitializeServer();

		//Forge Server init here
	}
	
	@Override
	protected void createInitialBindings() {
		new ForgeModChecker();

		//Forge static Instances here
	}
	
	@Override
	protected IEventProxy createClientProxy() { return new ForgeClientProxy(); }
	
	@Override
	protected IEventProxy createServerProxy(boolean isDedicated) { return new ForgeServerProxy(isDedicated); }
	
	@Override
	protected void initializeModCompat()
	{
	}
	
	/*@Override
	protected void subscribeClientStartedEvent(Runnable eventHandler)
	{
		// FIXME What event is this?
		MinecraftForge.EVENT_BUS.addListener();
	}
	
	@Override
	protected void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler)
	{
		MinecraftForge.EVENT_BUS.addListener((#if MC_VER >= MC_1_18_2 ServerStartingEvent #else FMLServerStartingEvent #endif e) ->
		{
			eventHandler.accept(e.getServer());
		});
	}
	
	@Override
	protected void runDelayedSetup() {
		//setup after init here
	}*/
}
