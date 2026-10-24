package de.tomnaiser.panoramaandroid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** A single background writer. The database is authoritative; startup repairs unpublished commits. */
final class LivePapywizard implements AutoCloseable {
    interface Source extends AutoCloseable {
        List<String> roots();
        Snapshot snapshot(String key);
        @Override void close();
    }
    static final class Snapshot {
        final String filename, xml;
        Snapshot(String filename,String xml) { this.filename=filename;this.xml=xml; }
    }
    interface Publisher { void publish(File directory,Snapshot snapshot) throws IOException; }
    interface Listener { void changed(String status); }
    private final Source source;
    private final File directory;
    private final Publisher publisher;
    private final Listener listener;
    // Serialize across Service recreation too: an old writer must never replace a newer file.
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r -> {
        Thread thread=new Thread(r,"Papywizard-export");thread.setDaemon(true);return thread;
    });
    private final ExecutorService worker=WORKER;
    private final Set<String> pending=new LinkedHashSet<>();
    private final Map<String,String> failures=new LinkedHashMap<>(); // writer thread only
    private boolean scheduled,closed;
    private volatile String status="Automatische Papywizard-Sicherung wird vorbereitet.";

    LivePapywizard(Source source,File directory,Listener listener) {
        this(source,directory,LivePapywizard::atomicPublish,listener);
    }
    LivePapywizard(Source source,File directory,Publisher publisher,Listener listener) {
        this.source=source;this.directory=directory;this.publisher=publisher;this.listener=listener;
    }
    String status() { return status+"\n"+directory.getAbsolutePath(); }
    /** Rebuild every logical panorama, including empty plans and recovered partial runs. */
    synchronized void rebuild() {
        if(closed) return;
        worker.execute(() -> {
            try {
                List<String> roots=source.roots();failures.remove("startup");
                synchronized(this) { pending.addAll(roots);scheduled=true; }
                drain();
            }
            catch(RuntimeException ex) { failed("startup",ex); }
        });
    }
    synchronized void request(String key) {
        if(closed) return;
        pending.add(key);
        if(!scheduled) { scheduled=true;worker.execute(this::drain); }
    }
    private void drain() {
        while(true) {
            String key;
            synchronized(this) {
                if(pending.isEmpty()) { scheduled=false;return; }
                Iterator<String> iterator=pending.iterator();key=iterator.next();iterator.remove();
            }
            String failureKey=key;
            try {
                Snapshot snapshot=source.snapshot(key);
                failureKey=snapshot.filename;
                publisher.publish(directory,snapshot);
                failures.remove(key);failures.remove(snapshot.filename);
                status=failures.isEmpty()?"Papywizard gesichert: "+snapshot.filename:failureSummary();
                listener.changed(status());
            } catch(IOException | RuntimeException ex) { failed(failureKey,ex); }
        }
    }
    private String failureSummary() {
        return "Papywizard-Sicherung fehlgeschlagen ("+failures.size()+"): "+failures.values().iterator().next()+
            ". Aufnahmedaten bleiben in der Datenbank. Unter Extras erneut sichern.";
    }
    private void failed(String key,Exception ex) {
        failures.put(key,ex.getMessage());status=failureSummary();
        listener.changed(status());
    }
    static void atomicPublish(File directory,Snapshot snapshot) throws IOException {
        atomicPublish(directory,snapshot,() -> { });
    }
    interface BeforeReplace { void run() throws IOException; }
    static void atomicPublish(File directory,Snapshot snapshot,BeforeReplace beforeReplace) throws IOException {
        Files.createDirectories(directory.toPath());
        Path target=new File(directory,snapshot.filename).toPath();
        Path temporary=new File(directory,snapshot.filename+".tmp").toPath();
        try {
            try(FileOutputStream stream=new FileOutputStream(temporary.toFile())) {
                stream.write(snapshot.xml.getBytes(StandardCharsets.UTF_8));
                stream.getFD().sync();
            }
            beforeReplace.run();
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    /** Tests may wait off the UI thread for a quiet queue. Production never waits for file IO. */
    void awaitIdle(long timeoutMs) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while(true) {
            worker.submit(() -> { }).get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
            synchronized(this) { if(!scheduled && pending.isEmpty()) return; }
            if(System.nanoTime()>=deadline) throw new TimeoutException("Papywizard writer busy");
        }
    }
    @Override public synchronized void close() {
        if(closed) return;
        closed=true;
        // The already queued drain finishes before its private database connection is closed.
        worker.execute(source::close);
    }
}
