package df11zomgraves.ingameinfo.handler;

import df11zomgraves.ingameinfo.InGameInfoCore;
import df11zomgraves.ingameinfo.InGameInfoXML;
import df11zomgraves.ingameinfo.network.RequestMSPTPacket;
import df11zomgraves.ingameinfo.network.RequestSeedPacket;
import df11zomgraves.ingameinfo.reference.Names;
import df11zomgraves.ingameinfo.tag.Tag;
import df11zomgraves.ingameinfo.util.MathUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/**
 * Drives the overlay: per-frame render plus the seed/mspt bookkeeping on the client tick.
 *
 * <p>Port notes:
 * <ul>
 *   <li>{@code RenderGuiEvent.Post} -> {@code HudRenderCallback.EVENT}. NeoForge allowed
 *       {@code EventPriority.HIGHEST}; Fabric's {@code Event} has no priority concept, so
 *       ordering is by registration order and this is registered in client init.</li>
 *   <li>{@code ClientTickEvent.Pre} -> {@code ClientTickEvents.START_CLIENT_TICK}.</li>
 *   <li>{@code PacketDistributor.sendToServer} -> {@code ClientPlayNetworking.send}.</li>
 *   <li>The old {@code RenderGuiLayerEvent.Pre} handler (cancelling the vanilla health/food/
 *       armour/air/vehicle/potion layers) has NO Fabric API equivalent on 1.21.1 and moved to
 *       {@code mixin.GuiMixin}.</li>
 * </ul>
 */
public class Ticker {
	public static final Ticker INSTANCE = new Ticker();
	public static boolean enabled = true;
	private final Minecraft client = Minecraft.getInstance();
	private final InGameInfoCore core = InGameInfoCore.INSTANCE;
	private boolean inGame = false;
	private long lastRemoteUpdate = 0;

	public static void register() {
		HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
			if (INSTANCE.isRunning()) {
				INSTANCE.core.onTickClient();
				INSTANCE.core.onTickRender(guiGraphics);
			}
		});
		ClientTickEvents.START_CLIENT_TICK.register(client -> INSTANCE.onClientTick());
	}

	private boolean isRunning() {
		if (!enabled) {
			return false;
		}
		// a && b || !a && !b --> a == b
		final Options options = this.client.options;
		if (options != null) {
			if (!ConfigurationHandler.showOnPlayerList && options.keyPlayerList.isDown()) {
				return false;
			}
			if (options.hideGui) {
				return false;
			}
			if (client.getDebugOverlay().showDebugScreen()) {
				return false;
			}
			if (this.client.gui == null) {
				return true;
			}
			if (this.client.screen instanceof GameModeSwitcherScreen) {
				return true;
			}
			return client.screen == null
					|| (ConfigurationHandler.showInChat && client.screen instanceof ChatScreen);
		}
		return false;
	}

	private void onClientTick() {
		final boolean inGameCurrent = (client.level != null);
		if (inGame) {
			if (!inGameCurrent) {
				InGameInfoXML.seed = ConfigurationHandler.seed;
				InGameInfoXML.mspt = -1;
				InGameInfoXML.tps = -1;
				InGameInfoXML.serverInstalled = false;
				inGame = false;
			} else if (isRunning() && InGameInfoXML.existMSPT) {
				final long delay = (System.currentTimeMillis() - lastRemoteUpdate);
				if (delay > 1500 || delay < 0) {
					final IntegratedServer singleServer = client.getSingleplayerServer();
					if (singleServer != null) {
						// NeoForge patched MinecraftServer with getTickTime(ResourceKey<Level>);
						// vanilla 1.21.1 exposes the same underlying per-tick nanos array as
						// getTickTimesNanos(). Tick times are global, not per-dimension.
						final long[] times = singleServer.getTickTimesNanos();
						if (times != null) {
							final double worldTickTime = MathUtils.mean(times) * 1.0E-6D;
							InGameInfoXML.mspt = worldTickTime;
							InGameInfoXML.tps = (worldTickTime == -1) ? -1 : Math.min(1000.0 / worldTickTime, 20);
						}
					} else if (InGameInfoXML.serverInstalled) {
						try {
							InGameInfoXML.serverInstalled = false;
							ClientPlayNetworking.send(new RequestMSPTPacket(0D));
						} catch (final Exception e) {
							InGameInfoXML.mspt = -1;
							InGameInfoXML.tps = -1;
						}
					}
					lastRemoteUpdate = System.currentTimeMillis();
				}
			}
		} else if (inGameCurrent) {
			try {
				final InGameInfoCore core = InGameInfoCore.INSTANCE;
				core.setConfigFile(ConfigurationHandler.configName);
				core.reloadConfig();
				inGame = true;
				InGameInfoXML.serverInstalled = true;
				showVersionInfo();
				ClientPlayNetworking.send(new RequestSeedPacket(0L));
			} catch (final Exception e) {
				InGameInfoXML.seed = ConfigurationHandler.seed;
			}
		}
		if (!enabled || this.client.options == null) {
			Tag.releaseResources();
		}
	}

	private void showVersionInfo() {
		// NeoForge patched KeyMapping with getKey(); vanilla returns the already-translated
		// display name for the bound key, which is what the old translatable() dance produced.
		final String keyToggle1 = KeyInputHandler.KEY_TOGGLE.getTranslatedKeyMessage().getString();
		final String keyReload1 = KeyInputHandler.KEY_RELOAD.getTranslatedKeyMessage().getString();
		client.gui.getChat().addMessage(
				Component.translatable(Names.VERSION_CHAT, Names.VERSION, keyToggle1, keyReload1));
	}
}
