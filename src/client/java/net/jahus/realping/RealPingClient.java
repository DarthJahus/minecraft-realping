package net.jahus.realping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class RealPingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PingConfig.load();

		ClientPlayNetworking.registerGlobalReceiver(PingPayloads.PingResponse.TYPE, (payload, context) -> {
			PingClient.onResponse(payload.id());
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			String address = extractServerAddress(client);
			PingClient.start(address);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			PingClient.stop();
		});

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath(RealPing.MOD_ID, "ping"),
				PingHud::render);

		RealPing.LOGGER.info("RealPing client initialized");
	}

	private static String extractServerAddress(Minecraft client) {
		if (client.getCurrentServer() != null) {
			String addr = client.getCurrentServer().ip;
			if (addr != null && !addr.isBlank()) {
				return addr;
			}
		}
		return "localhost";
	}
}
