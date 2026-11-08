package df11zomgraves.ingameinfo.network;

import df11zomgraves.ingameinfo.reference.Names;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: "please tell me your mspt". Handled in {@link ServerNetworkHandler}. */
public record RequestMSPTPacket(double mspt) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Names.MODID, "request_mspt");
	public static final Type<RequestMSPTPacket> TYPE = new Type<>(ID);

	public static final StreamCodec<RegistryFriendlyByteBuf, RequestMSPTPacket> STREAM_CODEC = StreamCodec
			.composite(ByteBufCodecs.DOUBLE, RequestMSPTPacket::mspt, RequestMSPTPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
