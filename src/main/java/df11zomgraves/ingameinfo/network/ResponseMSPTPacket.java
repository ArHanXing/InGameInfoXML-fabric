package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.reference.Names;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: measured mspt. Handled in {@link ClientNetworkHandler}. */
public record ResponseMSPTPacket(double mspt) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Names.MODID, "response_mspt");
	public static final Type<ResponseMSPTPacket> TYPE = new Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, ResponseMSPTPacket> STREAM_CODEC = StreamCodec
			.composite(ByteBufCodecs.DOUBLE, ResponseMSPTPacket::mspt, ResponseMSPTPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
