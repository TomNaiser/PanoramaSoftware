package de.tomnaiser.panoramaandroid;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** One pending request. Emergency stop replaces it before any further retry can be sent. */
public final class V2Client implements AutoCloseable {
    public interface Transport { void write(String frame) throws IOException; }
    public static final class Result {
        public final boolean accepted;
        public final String code;
        Result(boolean accepted, String code) { this.accepted = accepted; this.code = code; }
    }
    private final Transport transport;
    private final ScheduledExecutorService worker;
    private final long timeoutMs;
    private final int retries;
    private int sequence;
    private Pending pending;
    private boolean closed, uncertain;

    public V2Client(Transport transport, ScheduledExecutorService worker, int firstSequence,
                    long timeoutMs, int retries) {
        if (firstSequence < 1 || firstSequence > 65535 || timeoutMs < 1 || retries < 0)
            throw new IllegalArgumentException("V2-Konfiguration");
        this.transport = transport; this.worker = worker; this.sequence = firstSequence;
        this.timeoutMs = timeoutMs; this.retries = retries;
    }

    public synchronized CompletableFuture<Result> send(String type, String... fields) {
        if (closed || uncertain) return failed("Sitzung geschlossen oder Befehlswirkung unklar; erneut verbinden");
        if (pending != null) return failed("Ein Befehl wartet noch auf Bestätigung");
        return begin(type, fields);
    }

    public synchronized CompletableFuture<Result> emergencyStop(String runId) {
        if (closed) return failed("Sitzung geschlossen");
        cancelPending("Durch Not-Stop verworfen");
        uncertain = false;
        return begin("ESTOP", new String[] { runId, "ANDROID_ESTOP" });
    }

    private CompletableFuture<Result> begin(String type, String[] fields) {
        int id = nextSequence();
        String wire = P2Codec.encode(id, type, fields);
        if (wire.length() > 164) throw new IllegalArgumentException("Befehl überschreitet Firmware-Rahmenlimit");
        Pending request = new Pending(id, wire, type);
        pending = request;
        worker.execute(() -> transmit(request));
        return request.completion;
    }

    private synchronized void transmit(Pending request) {
        if (closed || pending != request) return;
        if (request.attempts > retries) {
            uncertain = true;
            pending = null;
            request.completion.completeExceptionally(new IOException(
                "Keine Bestätigung für " + request.type + "; Wirkung unklar. Neu verbinden."));
            return;
        }
        request.attempts++;
        try {
            transport.write(request.wire);
            worker.schedule(() -> transmit(request), timeoutMs, TimeUnit.MILLISECONDS);
        } catch (IOException ex) {
            uncertain = true; pending = null;
            request.completion.completeExceptionally(ex);
        }
    }

    public synchronized void receive(P2Codec.Frame frame) {
        if (closed || pending == null || (!frame.type.equals("ACK") && !frame.type.equals("NACK"))
                || frame.fields.length != 2) return;
        int reference;
        try { reference = Integer.parseInt(frame.fields[0]); } catch (NumberFormatException ex) { return; }
        if (reference != pending.sequence) return;
        Pending completed = pending;
        pending = null;
        completed.completion.complete(new Result(frame.type.equals("ACK"), frame.fields[1]));
    }

    public synchronized void heartbeat(String runId) {
        if (closed) return;
        String wire = P2Codec.encode(nextSequence(), "HEARTBEAT", runId);
        worker.execute(() -> {
            synchronized (V2Client.this) {
                if (closed) return;
                try { transport.write(wire); } catch (IOException ex) { close(); }
            }
        });
    }

    public synchronized boolean busy() { return pending != null; }
    private int nextSequence() { int id = sequence; sequence = sequence == 65535 ? 1 : sequence + 1; return id; }
    private static CompletableFuture<Result> failed(String message) {
        CompletableFuture<Result> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException(message)); return future;
    }
    private void cancelPending(String reason) {
        Pending old = pending; pending = null;
        if (old != null) old.completion.completeExceptionally(new IOException(reason));
    }
    @Override public synchronized void close() { closed = true; cancelPending("Verbindung beendet; Befehl nicht wiederholen"); }
    private static final class Pending {
        final int sequence; final String wire, type;
        final CompletableFuture<Result> completion = new CompletableFuture<>();
        int attempts;
        Pending(int sequence, String wire, String type) { this.sequence = sequence; this.wire = wire; this.type = type; }
    }
}
