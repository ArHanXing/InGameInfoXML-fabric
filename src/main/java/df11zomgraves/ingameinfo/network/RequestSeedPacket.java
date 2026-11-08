package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.reference.Names;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: "please tell me the world seed".
 *
 * <p>Note that the payload records in this package deliberately contain no handler
 * method. NeoForge kept {@code handleMessage} on the record, but that drags client-only
 * classes (chat, Minecraft) into classes the dedicated server also loads. Handlers live in
 * {@link ServerNetworkHandler} and {@link ClientNetworkHandler} instead, so these records
 * stay side-neutral and safe to reference from the common entrypoint.
 */
public record RequestSeedPacket(long seed) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Names.MODID, "request_seed");
	public static final Type<RequestSeedPacket> TYPE = new Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, RequestSeedPacket> STREAM_CODEC = StreamCodec
			.composite(ByteBufCodecs.VAR_LONG, RequestSeedPacket::seed, RequestSeedPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
