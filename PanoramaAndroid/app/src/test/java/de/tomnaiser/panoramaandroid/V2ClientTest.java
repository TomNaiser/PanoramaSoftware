package de.tomnaiser.panoramaandroid;

import org.junit.After;
import org.junit.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public final class V2ClientTest {
    private final ManualScheduler worker = new ManualScheduler();
    private final BlockingQueue<String> writes = new LinkedBlockingQueue<>();
    private final V2Client client = new V2Client(writes::add, worker, 65535, 20, 2);
    @After public void cleanup() { client.close(); worker.shutdownNow(); }
    private String next() throws Exception {
        String wire = writes.poll(1, TimeUnit.SECONDS); assertNotNull(wire); return wire;
    }
    private void reply(String type, int id, String code) {
        client.receive(P2Codec.decode(P2Codec.encode(7, type, Integer.toString(id), code)));
    }
    @Test public void lostAckRepeatsExactlyTheSameFrame() throws Exception {
        CompletableFuture<V2Client.Result> result = client.send("TRIGGER_ONCE", "30");
        String first = next(); worker.advance(); assertEquals(first, next());
        reply("ACK", 65535, "TRIGGERED");
        assertTrue(result.get(1, TimeUnit.SECONDS).accepted);
    }
    @Test public void foreignAckAndMalformedAckDoNotCompleteRequest() throws Exception {
        CompletableFuture<V2Client.Result> result = client.send("GET_STATUS"); next();
        reply("ACK", 1, "OK");
        client.receive(P2Codec.decode(P2Codec.encode(2, "ACK", "65535")));
        assertFalse(result.isDone()); reply("NACK", 65535, "INVALID_STATE");
        assertFalse(result.get(1, TimeUnit.SECONDS).accepted);
    }
    @Test public void parallelRequestIsRejectedAndSequenceWraps() throws Exception {
        CompletableFuture<V2Client.Result> first = client.send("GET_STATUS"); next();
        assertTrue(client.send("START", "8").isCompletedExceptionally());
        reply("ACK", 65535, "OK"); first.get();
        client.send("GET_STATUS"); assertEquals(1, P2Codec.decode(next()).sequence);
    }
    @Test public void closeCancelsRequestAndNoNewCommandIsSent() throws Exception {
        CompletableFuture<V2Client.Result> result = client.send("START", "8"); next();
        client.close(); assertTrue(result.isCompletedExceptionally());
        assertTrue(client.send("START", "9").isCompletedExceptionally());
        worker.advance();
        assertTrue(writes.isEmpty());
    }
    @Test public void emergencyStopSupersedesCommandAndLateAck() throws Exception {
        CompletableFuture<V2Client.Result> old = client.send("MANUAL_MOVE", "1000", "0"); next();
        CompletableFuture<V2Client.Result> stop = client.emergencyStop("0");
        assertEquals("ESTOP", P2Codec.decode(next()).type);
        assertTrue(old.isCompletedExceptionally()); reply("ACK", 65535, "MOVING");
        assertFalse(stop.isDone()); reply("ACK", 1, "STOPPED");
        assertEquals("STOPPED", stop.get(1, TimeUnit.SECONDS).code);
        worker.advance(); assertTrue(writes.isEmpty());
    }
    @Test public void exhaustedRetryBudgetBlocksNewActionsUntilResync() throws Exception {
        CompletableFuture<V2Client.Result> result = client.send("TRIGGER_ONCE", "30");
        String wire = next(); worker.advance(); assertEquals(wire, next()); worker.advance(); assertEquals(wire, next()); worker.advance();
        try { result.get(1, TimeUnit.SECONDS); fail(); } catch (ExecutionException expected) { }
        assertTrue(client.send("TRIGGER_ONCE", "30").isCompletedExceptionally());
    }
    @Test public void heartbeatDoesNotReplacePendingCommand() throws Exception {
        CompletableFuture<V2Client.Result> result = client.send("PAUSE", "5"); next();
        client.heartbeat("5"); assertEquals("HEARTBEAT", P2Codec.decode(next()).type);
        reply("ACK", 65535, "PAUSED"); assertTrue(result.get(1, TimeUnit.SECONDS).accepted);
    }
    private static final class ManualScheduler extends ScheduledThreadPoolExecutor {
        private final java.util.Queue<Scheduled> tasks = new java.util.ArrayDeque<>();
        ManualScheduler() { super(1); }
        @Override public void execute(Runnable task) { task.run(); }
        @Override public ScheduledFuture<?> schedule(Runnable task,long delay,TimeUnit unit) {
            Scheduled scheduled = new Scheduled(task); tasks.add(scheduled); return scheduled;
        }
        void advance() { Scheduled task = tasks.poll(); if (task != null) task.run(); }
        private static final class Scheduled extends FutureTask<Void> implements ScheduledFuture<Void> {
            Scheduled(Runnable task) { super(task,null); }
            @Override public long getDelay(TimeUnit unit) { return 0; }
            @Override public int compareTo(Delayed other) { return 0; }
        }
    }
}
