package df11zomgraves.ingameinfo;

import java.nio.file.Path;

import df11zomgraves.ingameinfo.api.InGameInfoAPI;
import df11zomgraves.ingameinfo.handler.ClientEventHandler;
import df11zomgraves.ingameinfo.handler.ConfigurationHandler;
import df11zomgraves.ingameinfo.handler.KeyInputHandler;
import df11zomgraves.ingameinfo.handler.Ticker;
import df11zomgraves.ingameinfo.network.ClientNetworkHandler;
import df11zomgraves.ingameinfo.reference.Names;
import df11zomgraves.ingameinfo.tag.TagRegistry;
import df11zomgraves.ingameinfo.value.ValueRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client entrypoint. Everything that touches client-only classes is reached from here,
 * so the dedicated server never loads any of it.
 */
public class InGameInfoXMLClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Config first: the core reads ConfigurationHandler's static fields on reload.
		ConfigurationHandler.init();
		ConfigurationHandler.load();

		InGameInfoCore.INSTANCE = new InGameInfoCore();
		final Path configDir = FabricLoader.getInstance().getConfigDir();
		InGameInfoCore.INSTANCE.setConfigDirectory(configDir.toFile());

		ValueRegistry.INSTANCE.init();
		TagRegistry.INSTANCE.init();

		// Flush any tags/values third-party mods queued before this ran, and mark the
		// extension API usable from here on.
		InGameInfoAPI.markReady();

		// S2C receivers (seed / mspt responses).
		ClientNetworkHandler.register();

		// Keybinds, HUD rendering, client tick and the client-side command.
		KeyInputHandler.register();
		Ticker.register();
		ClientEventHandler.register();

		InGameInfoXML.logger.info("{} client initialised", Names.MODID);
	}
}
