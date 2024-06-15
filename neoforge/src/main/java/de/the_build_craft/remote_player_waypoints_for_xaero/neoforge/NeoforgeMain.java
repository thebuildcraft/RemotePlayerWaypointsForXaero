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

package de.the_build_craft.remote_player_waypoints_for_xaero.neoforge;

import de.the_build_craft.remote_player_waypoints_for_xaero.common.AbstractModInitializer;
import de.the_build_craft.remote_player_waypoints_for_xaero.common.LoaderType;
import de.the_build_craft.remote_player_waypoints_for_xaero.neoforge.wrappers.NeoForgeModChecker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
#if MC_VER > MC_1_20_4
import net.neoforged.neoforge.client.gui.ModListScreen;
#else
import net.neoforged.neoforge.client.ConfigScreenHandler;
#endif

/**
 * main entry point on NeoForge
 *
 * @author James Seibel
 * @author Leander Knüttel
 * @version 15.06.2024
 */
@Mod(AbstractModInitializer.MOD_ID)
public class NeoforgeMain extends AbstractModInitializer
{
	public NeoforgeMain(IEventBus eventBus)
	{
		loaderType = LoaderType.NeoForge;
        CommonModConfigNeoForge config = new CommonModConfigNeoForge();

		// Register the mod initializer (Actual event registration is done in the different proxies)
		eventBus.addListener((FMLClientSetupEvent e) -> this.onInitializeClient());
		eventBus.addListener((FMLDedicatedServerSetupEvent e) -> this.onInitializeServer());
	}

	@Override
	public void onInitializeClient(){
		super.onInitializeClient();

		//NeoForge Client init here
	}

	@Override
	public void onInitializeServer(){
		super.onInitializeServer();

		//NeoForge Server init here
	}
	
	@Override
	protected void createInitialBindings() {
		new NeoForgeModChecker();

		//NeoForge static Instances here
	}

	@Override
	protected IEventProxy createServerProxy(boolean isDedicated) { return new NeoforgeServerProxy(isDedicated); }

	@Override
	protected IEventProxy createClientProxy() { return new NeoforgeClientProxy(); }
	
	@Override
	protected void initializeModCompat()
	{//FIXME set up NeoForge Config screen
		/*ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> GetConfigScreen.getScreen(parent)));*/
	}

	/*
	@Override
	protected void subscribeClientStartedEvent(Runnable eventHandler)
	{
		// FIXME What event is this?
	}
	
	@Override
	protected void subscribeServerStartingEvent(Consumer<MinecraftServer> eventHandler)
	{
		NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> { eventHandler.accept(e.getServer()); });
	}
	
	@Override
	protected void runDelayedSetup() { *//*SingletonInjector.INSTANCE.runDelayedSetup();*//* }*/
	
}
