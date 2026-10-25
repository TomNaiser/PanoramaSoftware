package de.tomnaiser.panoramaandroid;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** One pending request. Emergency stop replaces it before any further retry can be sent. */
public final class V2Client implements AutoCloseable {
    public interface Transport {
        void write(String frame) throws IOException;
        /** Must release a blocked write by closing this transport, never a replacement session. */
        default void abort() { }
    }
    public static final class Result {
        public final boolean accepted;
        public final String code;
        public final int commandSequence;
        Result(boolean accepted, String code) { this(accepted,code,0); }
        Result(boolean accepted, String code,int commandSequence) {
            this.accepted = accepted; this.code = code; this.commandSequence=commandSequence;
        }
    }
    private final Transport transport;
    private final ScheduledExecutorService worker;
    private final long timeoutMs;
    private final int retries;
    // A blocked RFCOMM writer cannot run its own deadline on the serial worker.
    private final ScheduledThreadPoolExecutor watchdog = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "Panorama-WriteDeadline"); thread.setDaemon(true); return thread;
    });
    private Object activeWrite;
    private int sequence;
    private Pending pending;
    private boolean closed, uncertain;

    public V2Client(Transport transport, ScheduledExecutorService worker, int firstSequence,
                    long timeoutMs, int retries) {
        if (firstSequence < 1 || firstSequence > 65535 || timeoutMs < 1 || retries < 0)
            throw new IllegalArgumentException("V2-Konfiguration");
        this.transport = transport; this.worker = worker; this.sequence = firstSequence;
        this.timeoutMs = timeoutMs; this.retries = retries;
        watchdog.setRemoveOnCancelPolicy(true);
    }

    public synchronized CompletableFuture<Result> send(String type, String... fields) {
        if (closed || uncertain) return failed(UiText.SESSION_CLOSED_OR_COMMAND_EFFECT_UNCERTAIN_RECONNECT.text());
        if (pending != null) return failed(UiText.A_COMMAND_IS_STILL_AWAITING_ACKNOWLEDGEMENT.text());
        return begin(type, fields);
    }

    public synchronized CompletableFuture<Result> emergencyStop(String runId) {
        if (closed) return failed(UiText.SESSION_CLOSED.text());
        cancelPending(UiText.DISCARDED_BY_EMERGENCY_STOP.text());
        uncertain = false;
        return begin("ESTOP", new String[] { runId, "ANDROID_ESTOP" });
    }
    public synchronized CompletableFuture<Result> stopJog(String token) {
        if(closed) return failed(UiText.SESSION_CLOSED.text());
        cancelPending(UiText.DISCARDED_ON_RELEASE.text()); uncertain=false;
        return begin("MANUAL_JOG_STOP",new String[]{token});
    }

    private CompletableFuture<Result> begin(String type, String[] fields) {
        int id = nextSequence();
        String wire = P2Codec.encode(id, type, fields);
        if (wire.length() > 164) throw new IllegalArgumentException(UiText.COMMAND_EXCEEDS_FIRMWARE_FRAME_LIMIT.text());
        Pending request = new Pending(id, wire, type);
        pending = request;
        worker.execute(() -> transmit(request));
        return request.completion;
    }

    private void transmit(Pending request) {
        synchronized (this) {
            if (closed || pending != request) return;
            if (request.attempts > retries) {
                uncertain = !CommandEffects.readOnly(request.type);
                pending = null;
                request.completion.completeExceptionally(new IOException(
                    UiText.NO_ACKNOWLEDGEMENT_FOR.text() + request.type + (uncertain?UiText.EFFECT_UNCERTAIN_CHECK_DEVICE_STATE.text():UiText.QUERY_INCOMPLETE.text())));
                return;
            }
            request.attempts++;
        }
        if (write(request.wire, request)) {
            synchronized (this) {
                if (!closed && pending == request)
                    worker.schedule(() -> transmit(request), timeoutMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    private boolean write(String wire, Pending request) {
        Object token = new Object();
        ScheduledFuture<?> deadline;
        synchronized (this) {
            if (closed || (request != null && pending != request)) return false;
            activeWrite = token;
            deadline = watchdog.schedule(() -> failWrite(token,
                new IOException(UiText.BLUETOOTH_WRITE_TIMED_OUT_RECONNECT.text())), timeoutMs, TimeUnit.MILLISECONDS);
        }
        try {
            // Never hold the client monitor while invoking blocking platform I/O.
            transport.write(wire);
            synchronized (this) { return !closed && activeWrite == token; }
        } catch (IOException ex) {
            failWrite(token, ex); return false;
        } finally {
            synchronized (this) { if (activeWrite == token) activeWrite = null; }
            deadline.cancel(false);
        }
    }

    private void failWrite(Object token, IOException error) {
        Pending old;
        synchronized (this) {
            if (closed || activeWrite != token) return;
            closed = true; activeWrite = null;
            old = pending; pending = null;
        }
        watchdog.shutdown();
        if (old != null) old.completion.completeExceptionally(error);
        transport.abort();
    }

    public synchronized void receive(P2Codec.Frame frame) {
        if (closed || pending == null || (!frame.type.equals("ACK") && !frame.type.equals("NACK"))
                || frame.fields.length != 2) return;
        int reference;
        try { reference = Integer.parseInt(frame.fields[0]); } catch (NumberFormatException ex) { return; }
        if (reference != pending.sequence) return;
        Pending completed = pending;
        pending = null;
        completed.completion.complete(new Result(frame.type.equals("ACK"), frame.fields[1],completed.sequence));
    }

    public synchronized void heartbeat(String runId) {
        if (closed) return;
        String wire = P2Codec.encode(nextSequence(), "HEARTBEAT", runId);
        worker.execute(() -> write(wire, null));
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
    @Override public synchronized void close() {
        closed = true; activeWrite = null;
        watchdog.shutdownNow();
        cancelPending(UiText.CONNECTION_ENDED_DO_NOT_REPEAT_COMMAND.text());
    }
    private static final class Pending {
        final int sequence; final String wire, type;
        final CompletableFuture<Result> completion = new CompletableFuture<>();
        int attempts;
        Pending(int sequence, String wire, String type) { this.sequence = sequence; this.wire = wire; this.type = type; }
    }
}
