package net.jahus.realping;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class PingPayloads {
	private PingPayloads() {}

	public static final Identifier REQUEST_ID = Identifier.fromNamespaceAndPath(RealPing.MOD_ID, "ping_request");
	public static final Identifier RESPONSE_ID = Identifier.fromNamespaceAndPath(RealPing.MOD_ID, "ping_response");

	/** Client → Server: carries a unique request id (long). */
	public record PingRequest(long id) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<PingRequest> TYPE = new CustomPacketPayload.Type<>(REQUEST_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, PingRequest> CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, PingRequest::id, PingRequest::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server → Client: echoes the same id. */
	public record PingResponse(long id) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<PingResponse> TYPE = new CustomPacketPayload.Type<>(RESPONSE_ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, PingResponse> CODEC =
				StreamCodec.composite(ByteBufCodecs.VAR_LONG, PingResponse::id, PingResponse::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}