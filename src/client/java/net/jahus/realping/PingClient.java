package net.jahus.realping;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side ping loop. One outstanding request at a time.
 * Statistics are session-scoped and reset on every new connection.
 * Timeouts are counted as losses and never as a ping value.
 */
public final class PingClient {
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "RealPing-Scheduler");
		t.setDaemon(true);
		return t;
	});

	private static final AtomicBoolean running = new AtomicBoolean(false);
	private static final AtomicLong nextId = new AtomicLong(1);
	private static final AtomicLong pendingId = new AtomicLong(-1);
	private static final AtomicLong sentNanos = new AtomicLong(0);
	private static final AtomicReference<String> currentServer = new AtomicReference<>(null);

	// Session statistics (only successful responses)
	private static final List<Double> samples = Collections.synchronizedList(new ArrayList<>());
	private static final AtomicInteger sent = new AtomicInteger();
	private static final AtomicInteger lost = new AtomicInteger();
	private static volatile double lastPing = Double.NaN;
	private static volatile double avg = Double.NaN;
	private static volatile double min = Double.NaN;
	private static volatile double max = Double.NaN;
	private static volatile double p10 = Double.NaN;
	private static volatile double p90 = Double.NaN;

	private static ScheduledFuture<?> intervalTask;
	private static ScheduledFuture<?> timeoutTask;

	private PingClient() {}

	public static void start(String serverAddress) {
		stop(); // clean any previous session
		currentServer.set(serverAddress);
		samples.clear();
		sent.set(0);
		lost.set(0);
		lastPing = avg = min = max = p10 = p90 = Double.NaN;
		pendingId.set(-1);
		running.set(true);

		PingCsvLogger.openFor(serverAddress);

		intervalTask = SCHEDULER.scheduleAtFixedRate(PingClient::trySend, 0, PingConfig.intervalMs, TimeUnit.MILLISECONDS);
		RealPing.LOGGER.info("Ping measurement started for {}", serverAddress);
	}

	public static void stop() {
		if (!running.getAndSet(false)) return;
		if (intervalTask != null) {
			intervalTask.cancel(false);
			intervalTask = null;
		}
		cancelTimeout();
		pendingId.set(-1);
		PingCsvLogger.close();
		RealPing.LOGGER.info("Ping measurement stopped");
	}

	private static void trySend() {
		if (!running.get()) return;
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() == null) return;

		// One outstanding request only
		if (pendingId.get() != -1) return;

		long id = nextId.getAndIncrement();
		long now = System.nanoTime();
		pendingId.set(id);
		sentNanos.set(now);
		sent.incrementAndGet();

		ClientPlayNetworking.send(new PingPayloads.PingRequest(id));

		cancelTimeout();
		timeoutTask = SCHEDULER.schedule(() -> onTimeout(id), PingConfig.timeoutMs, TimeUnit.MILLISECONDS);
	}

	private static void onTimeout(long id) {
		if (!pendingId.compareAndSet(id, -1)) return;
		lost.incrementAndGet();
		RealPing.LOGGER.info("Ping request {} lost (no response in {} ms)", id, PingConfig.timeoutMs);
		PingCsvLogger.logLoss();
	}

	public static void onResponse(long id) {
		if (!pendingId.compareAndSet(id, -1)) return;

		cancelTimeout();

		long now = System.nanoTime();
		long sentAt = sentNanos.get();
		double rttMs = (now - sentAt) / 1_000_000.0;

		samples.add(rttMs);
		lastPing = rttMs;
		recomputeStats();

		PingCsvLogger.log(rttMs);
	}

	private static void cancelTimeout() {
		if (timeoutTask != null) {
			timeoutTask.cancel(false);
			timeoutTask = null;
		}
	}

	private static void recomputeStats() {
		List<Double> copy;
		synchronized (samples) {
			if (samples.isEmpty()) return;
			copy = new ArrayList<>(samples);
		}
		Collections.sort(copy);
		int n = copy.size();

		double sum = 0;
		for (double v : copy) sum += v;
		avg = sum / n;
		min = copy.get(0);
		max = copy.get(n - 1);
		p10 = percentile(copy, 0.10);
		p90 = percentile(copy, 0.90);
	}

	private static double percentile(List<Double> sorted, double p) {
		if (sorted.isEmpty()) return Double.NaN;
		int n = sorted.size();
		double idx = p * (n - 1);
		int lo = (int) Math.floor(idx);
		int hi = (int) Math.ceil(idx);
		if (lo == hi) return sorted.get(lo);
		double w = idx - lo;
		return sorted.get(lo) * (1 - w) + sorted.get(hi) * w;
	}

	public static boolean isRunning() { return running.get(); }
	public static double getLast() { return lastPing; }
	public static double getAvg() { return avg; }
	public static double getMin() { return min; }
	public static double getMax() { return max; }
	public static double getP10() { return p10; }
	public static double getP90() { return p90; }
	public static int getSent() { return sent.get(); }
	public static int getLost() { return lost.get(); }
}
