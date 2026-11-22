package net.jahus.realping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ping.json");

	public static int intervalMs = 1000;
	public static int timeoutMs = 3000;
	public static boolean logToCSV = true;

	private PingConfig() {}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			saveConfig();
			return;
		}
		boolean doSave = false;
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			JsonObject obj = GSON.fromJson(reader, JsonObject.class);
			if (obj != null) {
				if (obj.has("interval_ms")) {
					int v = obj.get("interval_ms").getAsInt();
					if (v > 0) intervalMs = v;
				} else { doSave = true; }
				if (obj.has("timeout_ms")) {
					int v = obj.get("timeout_ms").getAsInt();
					if (v > 0) timeoutMs = v;
				} else { doSave = true; }
				if (obj.has("log_to_csv")) {
					logToCSV = obj.get("log_to_csv").getAsBoolean();
				} else { doSave = true; }
			}
		} catch (Exception e) {
			RealPing.LOGGER.warn("Failed to read config/ping.json, using defaults.", e);
			doSave = true;
		}
		if (doSave) saveConfig();
	}

	private static void saveConfig() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject obj = new JsonObject();
			obj.addProperty("interval_ms", intervalMs);
			obj.addProperty("timeout_ms", timeoutMs);
			obj.addProperty("log_to_csv", logToCSV);

			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(obj, writer);
			}
		} catch (IOException e) {
			RealPing.LOGGER.warn("Could not create default config/ping.json", e);
		}
	}
}
