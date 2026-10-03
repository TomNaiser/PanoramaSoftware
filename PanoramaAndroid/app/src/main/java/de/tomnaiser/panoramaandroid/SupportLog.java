package de.tomnaiser.panoramaandroid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Bounded on-device log; persistence never performs Bluetooth work. */
final class SupportLog implements AutoCloseable {
    private final File file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private volatile String failure;
    SupportLog(File directory) { file = new File(directory, "support.log"); }
    void append(String line) {
        writer.execute(() -> {
            try {
                if (file.length() > 1024 * 1024) {
                    Files.move(file.toPath(), new File(file.getParentFile(), "support.previous.log").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                    out.write(java.time.Instant.now() + " " + line + "\n");
                }
            } catch (IOException ex) { failure = ex.getMessage(); }
        });
    }
    String snapshot() throws IOException {
        if (failure != null) throw new IOException("Diagnoselog: " + failure);
        return file.exists() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : "Noch keine Meldungen";
    }
    @Override public void close() { writer.shutdown(); }
}
