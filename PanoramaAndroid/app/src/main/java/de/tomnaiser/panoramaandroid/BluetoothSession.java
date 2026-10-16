package de.tomnaiser.panoramaandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** One transport and request lifetime; closed requests never move to a new socket. */
public final class BluetoothSession implements DeviceLink {
    public interface Listener {
        void onConnected();
        void onAwaitingProtocol();
        void onFrame(P2Codec.Frame frame);
        void onDisconnected(String reason);
    }
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final Listener listener;
    private final boolean secure;
    private volatile BluetoothSocket socket;
    private volatile V2Client client;
    private volatile boolean closed, transportConnected, protocolReady;
    private volatile String failure;
    private volatile String runId = "0";
    private final Runnable deadline = () -> fail(transportConnected
        ? "Bluetooth verbunden, aber keine V2-Antwort vom Panoramagerät"
        : "Zeitüberschreitung beim Bluetooth-Aufbau");

    public BluetoothSession(Listener listener) { this(listener, true); }
    BluetoothSession(Listener listener, boolean secure) { this.listener = listener; this.secure = secure; }
    public void connect(BluetoothDevice device) {
        ui.postDelayed(deadline, 15000);
        new Thread(() -> run(device), "Panorama-Bluetooth").start();
    }
    private void run(BluetoothDevice device) {
        String reason = "Bluetooth-Verbindung beendet";
        try {
            BluetoothSocket current = secure ? device.createRfcommSocketToServiceRecord(SPP)
                : device.createInsecureRfcommSocketToServiceRecord(SPP);
            synchronized (this) {
                if (closed) { current.close(); return; }
                socket = current;
            }
            current.connect();
            if (closed) return;
            transportConnected = true;
            ui.post(() -> {
                ui.removeCallbacks(deadline);
                if (!closed) { listener.onAwaitingProtocol(); ui.postDelayed(deadline, 8000); }
            });
            java.io.OutputStream output = current.getOutputStream();
            synchronized (this) {
                if (closed) return;
                client = new V2Client(wire -> {
                    output.write(wire.getBytes(StandardCharsets.US_ASCII)); output.flush();
                }, worker, new SecureRandom().nextInt(65535) + 1, 750, 3);
            }
            client.send("HELLO", "PanoramaAndroid").whenComplete((result, error) -> {
                if (closed) return;
                if (error != null || !result.accepted || !result.code.equals("HELLO_ACK")) {
                    fail("V2-Handschlag fehlgeschlagen"); return;
                }
                protocolReady = true;
                ui.post(() -> {
                    ui.removeCallbacks(deadline);
                    if (!closed) listener.onConnected();
                });
                worker.scheduleAtFixedRate(() -> {
                    V2Client active = client;
                    if (!closed && active != null) active.heartbeat(runId);
                }, 0, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
            });
            readFrames(current.getInputStream());
        } catch (IOException | SecurityException ex) {
            if (ex instanceof SecurityException) reason = "Bluetooth-Berechtigung fehlt";
            else if (!transportConnected) reason = "Gerät nicht erreichbar. Stromversorgung, Reichweite und PC-Verbindung prüfen.";
            else reason = "Bluetooth-Verbindung abgebrochen";
        } finally {
            close();
            String finalReason = failure == null ? reason : failure;
            ui.post(() -> listener.onDisconnected(finalReason));
        }
    }
    private void readFrames(InputStream input) throws IOException {
        FrameReader parser = new FrameReader();
        int value;
        while (!closed && (value = input.read()) != -1) {
            P2Codec.Frame frame = parser.accept(value);
            if (frame == null) continue;
            V2Client active = client;
            if (active != null) active.receive(frame);
            if (frame.type.equals("STATUS") && frame.fields.length >= 18) runId = frame.fields[0];
            if (!frame.type.equals("STATUS")) Log.d("PanoramaBluetooth", "RX " + frame.type);
            ui.post(() -> { if (!closed) listener.onFrame(frame); });
        }
    }
    public CompletableFuture<V2Client.Result> send(String type, String... fields) {
        V2Client active = client;
        if (!protocolReady || closed || active == null) return unavailable();
        return active.send(type, fields);
    }
    public CompletableFuture<V2Client.Result> emergencyStop(String id) {
        V2Client active = client;
        if (!protocolReady || closed || active == null) return unavailable();
        return active.emergencyStop(id);
    }
    private static CompletableFuture<V2Client.Result> unavailable() {
        CompletableFuture<V2Client.Result> result = new CompletableFuture<>();
        result.completeExceptionally(new IOException("Keine V2-Verbindung")); return result;
    }
    public CompletableFuture<V2Client.Result> stopJog(String token) {
        V2Client active=client;
        if(!protocolReady || closed || active==null) return unavailable();
        return active.stopJog(token);
    }
    public void fail(String reason) { failure = reason; close(); }
    @Override public void close() {
        BluetoothSocket current;
        V2Client active;
        synchronized (this) { closed = true; current = socket; socket = null; active = client; }
        ui.removeCallbacks(deadline);
        if (current != null) try { current.close(); } catch (IOException ignored) { }
        if (active != null) active.close();
        worker.shutdownNow();
    }
}
