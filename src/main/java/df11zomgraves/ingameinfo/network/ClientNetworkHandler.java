package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.InGameInfoXML;
import df11zomgraves.ingameinfo.util.StringConvertUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side receivers. Registered only from the client entrypoint, so the dedicated
 * server never loads this class.
 *
 * <p>Fabric runs the handler on the netty thread; {@code context.client().execute(...)}
 * hops to the client thread, matching NeoForge's {@code IPayloadContext.enqueueWork}.
 */
public final class ClientNetworkHandler {

	private ClientNetworkHandler() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ResponseSeedPacket.TYPE,
				(payload, context) -> context.client().execute(() -> {
					InGameInfoXML.seed = payload.seed();
					StringConvertUtils.sendSeedToChat(payload.seed());
				}));

		ClientPlayNetworking.registerGlobalReceiver(ResponseMSPTPacket.TYPE,
				(payload, context) -> context.client().execute(() -> {
					InGameInfoXML.mspt = payload.mspt();
					InGameInfoXML.tps = (payload.mspt() == -1) ? -1 : Math.min(1000.0 / payload.mspt(), 20);
					InGameInfoXML.serverInstalled = true;
				}));
	}
}
