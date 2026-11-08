package df11zomgraves.ingameinfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import df11zomgraves.ingameinfo.network.RequestMSPTPacket;
import df11zomgraves.ingameinfo.network.RequestSeedPacket;
import df11zomgraves.ingameinfo.network.ResponseMSPTPacket;
import df11zomgraves.ingameinfo.network.ResponseSeedPacket;
import df11zomgraves.ingameinfo.network.ServerNetworkHandler;
import df11zomgraves.ingameinfo.reference.Names;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Common entrypoint (runs on both the dedicated server and the client).
 *
 * <p>This class is deliberately free of any client-only reference. The HUD, keybinds,
 * config screen and client command all live behind {@link InGameInfoXMLClient}, which
 * Fabric only invokes on the physical client — that is the Fabric equivalent of NeoForge
 * gating the old {@code @Mod} constructor behind {@code FMLEnvironment.dist == Dist.CLIENT}.
 *
 * <p>Only two things must happen on both sides:
 * <ul>
 *   <li>the payload types must be registered for both directions, because the server
 *       encodes the S2C responses and the client encodes the C2S requests;</li>
 *   <li>the server needs the C2S receivers to answer seed/mspt requests.</li>
 * </ul>
 */
public class InGameInfoXML implements ModInitializer {
	public static final Logger logger = LoggerFactory.getLogger(Names.MODID);

	public static long seed = 0;
	public static double mspt = -1;
	public static double tps = -1;
	public static boolean serverInstalled = false;
	public static boolean existMSPT = false;

	@Override
	public void onInitialize() {
		// Payload types: both directions, both sides (server encodes S2C, client encodes C2S).
		PayloadTypeRegistry.playC2S().register(RequestSeedPacket.TYPE, RequestSeedPacket.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(RequestMSPTPacket.TYPE, RequestMSPTPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ResponseSeedPacket.TYPE, ResponseSeedPacket.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ResponseMSPTPacket.TYPE, ResponseMSPTPacket.STREAM_CODEC);

		// Server-side receivers (seed / mspt requests).
		ServerNetworkHandler.register();

		logger.info("{} common initialised", Names.MODID);
	}
}
