package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** A stalled RFCOMM output must not take the Activity, reader or disconnect down with it. */
public final class BlockedTransportTest {
    @Test public void blockedWriteDoesNotBlockStatusOrDisconnect() throws Exception {
        ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor();
        ExecutorService caller = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        V2Client client = new V2Client(wire -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new java.io.IOException(ex); }
        }, writer, 1, 5000, 0);
        CompletableFuture<V2Client.Result> command = client.send("GET_STATUS");
        try {
            assertTrue("Writer must reach the stalled transport", entered.await(1, TimeUnit.SECONDS));
            Future<?> access = caller.submit(() -> {
                assertTrue(client.busy());
                client.receive(P2Codec.decode(P2Codec.encode(2, "ACK", "99", "OK")));
                client.close();
            });
            try { access.get(300, TimeUnit.MILLISECONDS); }
            catch (TimeoutException ex) { fail("UI/status/disconnect blocked behind Bluetooth write for >300 ms"); }
            assertTrue("Disconnect must resolve the pending command", command.isCompletedExceptionally());
        } finally {
            release.countDown(); writer.shutdownNow(); caller.shutdownNow(); client.close();
        }
    }

    @Test public void blockedWriteExpiresWithoutWaitingForWriterAndNeverReplays() throws Exception {
        ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), aborted = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
        V2Client client = new V2Client(new V2Client.Transport() {
            @Override public void write(String wire) throws java.io.IOException {
                writes.incrementAndGet(); entered.countDown();
                try { release.await(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new java.io.IOException(ex); }
            }
            @Override public void abort() { aborted.countDown(); }
        }, writer, 1, 50, 3);
        try {
            CompletableFuture<V2Client.Result> action = client.send("TRIGGER_ONCE", "30");
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            try { action.get(1, TimeUnit.SECONDS); fail("Blocked write must expire"); }
            catch (ExecutionException expected) { assertTrue(expected.getCause().getMessage().contains("Schreibzeit")); }
            assertTrue("Watchdog must abort this socket", aborted.await(1, TimeUnit.SECONDS));
            assertFalse(client.busy());
            assertTrue(client.send("START", "8").isCompletedExceptionally());
            assertTrue(client.emergencyStop("8").isCompletedExceptionally());
            release.countDown();
            writer.submit(() -> {}).get(1, TimeUnit.SECONDS);
            assertEquals("No retries or new actions after ambiguous write", 1, writes.get());
        } finally {
            release.countDown(); client.close(); writer.shutdownNow();
        }
    }

    @Test public void blockedHeartbeatAlsoAbortsTheSession() throws Exception {
        ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch release = new CountDownLatch(1), aborted = new CountDownLatch(1);
        V2Client client = new V2Client(new V2Client.Transport() {
            @Override public void write(String wire) throws java.io.IOException {
                try { release.await(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new java.io.IOException(ex); }
            }
            @Override public void abort() { aborted.countDown(); }
        }, writer, 1, 50, 3);
        try {
            client.heartbeat("0");
            assertTrue("Even unconfirmed heartbeats need an independent write deadline", aborted.await(1, TimeUnit.SECONDS));
            assertTrue(client.send("START", "8").isCompletedExceptionally());
        } finally { release.countDown(); client.close(); writer.shutdownNow(); }
    }

    @Test public void acknowledgementsAndEmergencyCancellationRemainResponsiveDuringBlockedWrite() throws Exception {
        ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        V2Client client = new V2Client(wire -> {
            entered.countDown();
            try { release.await(); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new java.io.IOException(ex); }
        }, writer, 1, 5000, 3);
        try {
            CompletableFuture<V2Client.Result> action = client.send("START", "8");
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            reader.submit(() -> client.receive(P2Codec.decode(P2Codec.encode(2,"ACK","1","STARTED"))))
                .get(300, TimeUnit.MILLISECONDS);
            assertTrue(action.get(300, TimeUnit.MILLISECONDS).accepted);
            CompletableFuture<V2Client.Result> next = client.send("PAUSE", "8");
            CompletableFuture<V2Client.Result> stop = reader.submit(() -> client.emergencyStop("8"))
                .get(300, TimeUnit.MILLISECONDS);
            assertTrue(next.isCompletedExceptionally());
            client.close(); assertTrue(stop.isCompletedExceptionally());
        } finally { release.countDown(); client.close(); writer.shutdownNow(); reader.shutdownNow(); }
    }
}
