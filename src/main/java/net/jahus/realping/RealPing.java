package net.jahus.realping;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealPing implements ModInitializer {
	public static final String MOD_ID = "realping";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register payloads on both sides (common)
		PayloadTypeRegistry.serverboundPlay().register(PingPayloads.PingRequest.TYPE, PingPayloads.PingRequest.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PingPayloads.PingResponse.TYPE, PingPayloads.PingResponse.CODEC);

		// Server-side handler: reply immediately with the same id
		PingServer.register();

		LOGGER.info("RealPing initialized (server side ready)");
	}
}