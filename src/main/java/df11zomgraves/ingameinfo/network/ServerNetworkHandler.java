package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.util.MathUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side receivers. Replaces NeoForge's {@code PacketDistributor.sendToPlayer} +
 * {@code IPayloadContext.enqueueWork}.
 *
 * <p>Fabric dispatches payload handlers on the netty thread, so every body that touches
 * world state is scheduled onto the server thread via {@code context.server().execute(...)}
 * — the direct equivalent of NeoForge's {@code enqueueWork}.
 */
public final class ServerNetworkHandler {

	private ServerNetworkHandler() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(RequestSeedPacket.TYPE, ServerNetworkHandler::onRequestSeed);
		ServerPlayNetworking.registerGlobalReceiver(RequestMSPTPacket.TYPE, ServerNetworkHandler::onRequestMSPT);
	}

	private static void onRequestSeed(RequestSeedPacket payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ServerPlayer player = context.player();
			if (player == null) {
				return;
			}
			final long seed = player.serverLevel().getSeed();
			ServerPlayNetworking.send(player, new ResponseSeedPacket(seed));
		});
	}

	private static void onRequestMSPT(RequestMSPTPacket payload, ServerPlayNetworking.Context context) {
		context.server().execute(() -> {
			final ServerPlayer player = context.player();
			if (player == null) {
				return;
			}
			final MinecraftServer server = player.getServer();
			if (server == null) {
				return;
			}
			// Vanilla equivalent of NeoForge's patched getTickTime(ResourceKey<Level>).
			final long[] times = server.getTickTimesNanos();
			if (times != null) {
				final double mspt = MathUtils.mean(times) * 1.0E-6D;
				ServerPlayNetworking.send(player, new ResponseMSPTPacket(mspt));
			}
		});
	}
}
