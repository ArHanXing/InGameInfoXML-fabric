package df11zomgraves.ingameinfo.handler;

import java.nio.file.Path;

import df11zomgraves.ingameinfo.InGameInfoCore;
import df11zomgraves.ingameinfo.command.InGameInfoCommand;
import df11zomgraves.ingameinfo.reference.Names;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Replaces {@code NeoForgeClientEventHandler}: the reload/toggle hotkeys plus the
 * client-side command registration.
 */
public class ClientEventHandler {

	public static void register() {
		// NeoForge used ClientTickEvent.Pre -> START_CLIENT_TICK.
		ClientTickEvents.START_CLIENT_TICK.register(client -> onClientTick());

		// NeoForge's RegisterClientCommandsEvent -> Fabric's client command callback.
		// NOTE: Fabric client commands are typed on FabricClientCommandSource, not
		// CommandSourceStack; InGameInfoCommand is retyped accordingly.
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) -> InGameInfoCommand.register(dispatcher));
	}

	private static void onClientTick() {
		while (KeyInputHandler.KEY_RELOAD.consumeClick()) {
			final Minecraft mc = Minecraft.getInstance();
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.RELOAD));
			final InGameInfoCore core = InGameInfoCore.INSTANCE;

			final Path configPath = FabricLoader.getInstance().getConfigDir();

			core.setConfigDirectory(configPath.toFile());
			core.setConfigFile(ConfigurationHandler.configName);
			final boolean b = core.reloadConfig();
			if (b) {
				mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.SUCCESS));
			} else {
				mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.FAILURE));
			}
		}
		while (KeyInputHandler.KEY_TOGGLE.consumeClick()) {
			Ticker.enabled = !Ticker.enabled;
		}
	}
}
