package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.reference.Names;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: the world seed. Handled in {@link ClientNetworkHandler}. */
public record ResponseSeedPacket(long seed) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Names.MODID, "response_seed");
	public static final Type<ResponseSeedPacket> TYPE = new Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ResponseSeedPacket> STREAM_CODEC = StreamCodec
			.composite(ByteBufCodecs.VAR_LONG, ResponseSeedPacket::seed, ResponseSeedPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
