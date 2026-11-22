package net.jahus.realping;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PingServer {
	private PingServer() {}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PingPayloads.PingRequest.TYPE, (payload, context) -> {
			// Reply immediately on the server thread (no artificial next-tick delay).
			// Any delay that occurs is real server-thread contention, which is part of the measured RTT.
			ServerPlayNetworking.send(context.player(), new PingPayloads.PingResponse(payload.id()));
		});
	}
}