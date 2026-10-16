package de.tomnaiser.panoramaandroid;

import java.util.concurrent.CompletableFuture;

/** Wire seam shared by the real Bluetooth session and deterministic service checks. */
interface DeviceLink extends AutoCloseable {
    CompletableFuture<V2Client.Result> send(String type,String... fields);
    CompletableFuture<V2Client.Result> emergencyStop(String runId);
    default CompletableFuture<V2Client.Result> stopJog(String token) { return emergencyStop("0"); }
    void fail(String reason);
    void close();
}
