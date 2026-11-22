package net.jahus.realping;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-blocking CSV writer. Disk I/O never runs on the networking / game thread.
 */
public final class PingCsvLogger {
	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "RealPing-CSV");
		t.setDaemon(true);
		return t;
	});

	private static final AtomicReference<BufferedWriter> currentWriter = new AtomicReference<>(null);

	private PingCsvLogger() {}

	public static void openFor(String serverAddress) {
		close();

		String safe = sanitizeFilename(serverAddress);
		Path path = FabricLoader.getInstance().getConfigDir().resolve("ping_" + safe + ".csv");

		WRITER.execute(() -> {
			try {
				Files.createDirectories(path.getParent());
				boolean writeHeader = !Files.exists(path) || Files.size(path) == 0;
				BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				if (writeHeader) {
					w.write("timestamp,ping_ms,lost");
					w.newLine();
					w.flush();
				}
				currentWriter.set(w);
			} catch (IOException e) {
				RealPing.LOGGER.error("Failed to open CSV {}", path, e);
			}
		});
	}

	public static void log(double pingMs) {
		write(TS.format(LocalDateTime.now()) + "," + String.format(java.util.Locale.US, "%.2f", pingMs) + ",0");
	}

	public static void logLoss() {
		write(TS.format(LocalDateTime.now()) + ",,1");
	}

	private static void write(String line) {
		WRITER.execute(() -> {
			BufferedWriter w = currentWriter.get();
			if (w == null) return;
			try {
				w.write(line);
				w.newLine();
				w.flush();
			} catch (IOException e) {
				RealPing.LOGGER.warn("Failed to write CSV line", e);
			}
		});
	}

	public static void close() {
		WRITER.execute(() -> {
			BufferedWriter w = currentWriter.getAndSet(null);
			if (w != null) {
				try {
					w.close();
				} catch (IOException ignored) {}
			}
		});
	}

	static String sanitizeFilename(String address) {
		if (address == null || address.isBlank()) return "unknown";
		int colon = address.lastIndexOf(':');
		if (colon > 0 && address.indexOf(']') < 0) {
			address = address.substring(0, colon);
		}
		address = address.replace('[', '_').replace(']', '_').replace(':', '_');
		return address.replaceAll("[\\\\/:*?\"<>|]", "_");
	}
}
