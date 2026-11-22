package net.jahus.realping;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PingHud {
	private PingHud() {}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
		if (!PingClient.isRunning()) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;

		graphics.text(client.font, formatLine(), 4, 4, 0xFFE0E0E0, true);
	}

	private static String formatLine() {
		return String.format("INST %s | AVG %s | MAX %s | MIN %s | P90 %s | P10 %s | LOSS %d/%d",
				fmt(PingClient.getLast()),
				fmt(PingClient.getAvg()),
				fmt(PingClient.getMax()),
				fmt(PingClient.getMin()),
				fmt(PingClient.getP90()),
				fmt(PingClient.getP10()),
				PingClient.getLost(),
				PingClient.getSent());
	}

	private static String fmt(double v) {
		if (Double.isNaN(v)) return "--";
		return String.format("%.0f", v);
	}
}
