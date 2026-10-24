package de.tomnaiser.panoramaandroid;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.xml.parsers.DocumentBuilderFactory;

public class LivePapywizardTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private static String read(File dir) throws IOException {
        return new String(Files.readAllBytes(new File(dir,"test.xml").toPath()),java.nio.charset.StandardCharsets.UTF_8);
    }
    private static String xml(int count) {
        StringBuilder out=new StringBuilder("<?xml version=\"1.0\"?><papywizard version=\"c\"><header/><shoot>");
        for(int i=0;i<count;i++) out.append("<pict id=\"").append(i).append("\"/>");
        return out.append("</shoot></papywizard>").toString();
    }
    @Test public void thousandReplacementsAreAlwaysCompleteAndOrdered() throws Exception {
        File dir=temporary.newFolder();
        for(int n=0;n<=1000;n++) {
            LivePapywizard.atomicPublish(dir,new LivePapywizard.Snapshot("test.xml",xml(n)));
            org.w3c.dom.NodeList images=DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(dir,"test.xml")).getElementsByTagName("pict");
            assertEquals(n,images.getLength());
            if(n>0) assertEquals(""+(n-1),images.item(n-1).getAttributes().getNamedItem("id").getNodeValue());
        }
        assertArrayEquals(new String[]{"test.xml"},dir.list());
    }
    @Test public void interruptedReplacementPreservesPreviousFileAndCanBeRetried() throws Exception {
        File dir=temporary.newFolder();
        LivePapywizard.atomicPublish(dir,new LivePapywizard.Snapshot("test.xml",xml(3)));
        try {
            LivePapywizard.atomicPublish(dir,new LivePapywizard.Snapshot("test.xml",xml(4)),() -> { throw new IOException("disk error"); });
            fail();
        } catch(IOException expected) { assertEquals("disk error",expected.getMessage()); }
        assertEquals(xml(3),read(dir));
        LivePapywizard.atomicPublish(dir,new LivePapywizard.Snapshot("test.xml",xml(4)));
        assertEquals(xml(4),read(dir));
        assertArrayEquals(new String[]{"test.xml"},dir.list());
    }
    private static final class Source implements LivePapywizard.Source {
        volatile int count;
        volatile boolean closed;
        public List<String> roots() { return Collections.singletonList("root"); }
        public LivePapywizard.Snapshot snapshot(String key) { return new LivePapywizard.Snapshot("test.xml",xml(count)); }
        public void close() { closed=true; }
    }
    @Test public void slowDiskDoesNotBlockCaptureRequestsAndLatestStateWins() throws Exception {
        File dir=temporary.newFolder();Source source=new Source();
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AtomicInteger writes=new AtomicInteger();
        try(LivePapywizard live=new LivePapywizard(source,dir,(d,s) -> {
            if(writes.incrementAndGet()==1) { entered.countDown();try { release.await(5,TimeUnit.SECONDS); } catch(InterruptedException ex) { throw new IOException(ex); } }
            LivePapywizard.atomicPublish(d,s);
        },status -> { })) {
            live.request("root");assertTrue(entered.await(5,TimeUnit.SECONDS));
            // Would deadlock if request waited for a file write. Coalesce requests under a stalled writer.
            for(int i=1;i<=1000;i++) { source.count=i;live.request("root"); }
            release.countDown();live.awaitIdle(10000);
            assertEquals(xml(1000),read(dir));assertEquals(2,writes.get());
        } finally { release.countDown(); }
    }
    @Test public void restartRebuildsCommittedButUnpublishedCaptureAndReportsDiskFailure() throws Exception {
        File dir=temporary.newFolder();Source source=new Source();source.count=7;
        AtomicBoolean fail=new AtomicBoolean(true);
        try(LivePapywizard live=new LivePapywizard(source,dir,(d,s) -> {
            if(fail.get()) throw new IOException("full");LivePapywizard.atomicPublish(d,s);
        },status -> { })) {
            live.rebuild();live.awaitIdle(5000);assertTrue(live.status().contains("fehlgeschlagen"));
            fail.set(false);live.rebuild();live.awaitIdle(5000);
            assertEquals(xml(7),read(dir));
        }
        source.count=8; // committed after the previous worker stopped: no notification survived
        try(LivePapywizard restarted=new LivePapywizard(source,dir,status -> { })) {
            restarted.rebuild();restarted.awaitIdle(5000);
            assertEquals(xml(8),read(dir));
        }
    }
    @Test public void serviceRecreationDrainsOldWritesBeforeNewSnapshotAndClosesItsReader() throws Exception {
        File dir=temporary.newFolder();Source oldSource=new Source();oldSource.count=1;
        Source newSource=new Source();newSource.count=2;
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        LivePapywizard old=new LivePapywizard(oldSource,dir,(d,s) -> {
            entered.countDown();try { release.await(5,TimeUnit.SECONDS); } catch(InterruptedException ex) { throw new IOException(ex); }
            LivePapywizard.atomicPublish(d,s);
        },status -> { });
        try {
            old.request("root");assertTrue(entered.await(5,TimeUnit.SECONDS));old.close();
            try(LivePapywizard replacement=new LivePapywizard(newSource,dir,status -> { })) {
                replacement.rebuild();release.countDown();replacement.awaitIdle(10000);
                assertTrue(oldSource.closed);assertEquals(xml(2),read(dir));
            }
        } finally { release.countDown();old.close(); }
    }
}
