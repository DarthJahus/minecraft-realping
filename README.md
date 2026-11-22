# RealPing

Measures the real application-level RTT between a Minecraft client and server using only the existing Minecraft network connection (CustomPayload). ExitLag and similar tools therefore see the traffic as normal Minecraft packets.

## Features

- Periodic `PingRequest` / `PingResponse` over Fabric networking
- Instant server reply (no forced next-tick delay)
- One outstanding request at a time + configurable timeout
- Compact HUD: `INST 00 | AVG 00 | MAX 00 | MIN 00 | P90 00 | P10 00 | LOSS n/sent`
- Session statistics (reset on each connection). AVG/MIN/MAX/P10/P90 use successful replies only.
- CSV logging per server: `config/ping_<hostname-or-ip>.csv`
- Simple JSON config, no ModMenu / Cloth / AutoConfig

## Configuration (`config/ping.json`)

Created automatically on first launch:

```json
{
  "interval_ms": 1000,
  "timeout_ms": 5000
}
```

Invalid or missing values fall back to the defaults above.

## CSV format

```
timestamp,ping_ms,lost
2026-11-18T17:00:00.123,42.51,0
2026-11-18T17:00:05.123,,1
```

- `lost=1` and an empty `ping_ms` mark a timeout. It is never stored as 0 ms.
- Append mode (sessions do not overwrite previous data)
- Header written only when the file is empty. Delete an old file if it still has the two-column header.

## Design notes

- Payloads are registered on both logical sides.
- Server handler runs on the server thread and replies immediately; any observed delay is real contention (desired behaviour).
- CSV writes are offloaded to a dedicated single-thread executor so disk I/O never blocks networking or the client tick.
- Timed-out requests increment the session loss counter and are written with `lost=1`. They are never recorded as 0 ms or any artificial RTT. Disconnect cancels the in-flight probe without counting it as a loss.
