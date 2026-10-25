package de.tomnaiser.panoramaandroid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Bounded on-device log; persistence never performs Bluetooth work. */
final class SupportLog implements AutoCloseable {
    private final File file;
    private final long maximumBytes;
    private final int retainedFiles;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private volatile String failure;
    SupportLog(File directory) { this(directory,1024*1024,8); }
    SupportLog(File directory,long maximumBytes,int retainedFiles) {
        if(maximumBytes<1 || retainedFiles<2) throw new IllegalArgumentException(UiText.INVALID_LOG_SIZE.text());
        file=new File(directory,"support.log");this.maximumBytes=maximumBytes;this.retainedFiles=retainedFiles;
    }
    private File part(int index) {
        return index==0?file:new File(file.getParentFile(),index==1?"support.previous.log":"support."+index+".log");
    }
    void append(String line) {
        writer.execute(() -> {
            try {
                if (file.length() > maximumBytes) {
                    for(int i=retainedFiles-1;i>0;i--) if(part(i-1).exists())
                        Files.move(part(i-1).toPath(),part(i).toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                try (Writer out = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                    out.write(java.time.Instant.now() + " " + line + "\n");
                }
            } catch (IOException ex) { failure = ex.getMessage(); }
        });
    }
    String snapshot() throws IOException {
        try {
            // Queue the read behind pending writes and rotations, so exports include the fault just received.
            return writer.submit(() -> {
                if(failure!=null) throw new IOException(UiText.DIAGNOSTIC_LOG.text()+failure);
                StringBuilder out=new StringBuilder();
                for(int i=retainedFiles-1;i>=0;i--) if(part(i).exists()) {
                    out.append("=== ").append(part(i).getName()).append(" ===\n");
                    out.append(new String(Files.readAllBytes(part(i).toPath()),StandardCharsets.UTF_8));
                }
                return out.length()==0?UiText.NO_MESSAGES_YET.text():out.toString();
            }).get(5,TimeUnit.SECONDS);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();throw new IOException(UiText.DIAGNOSTIC_EXPORT_INTERRUPTED.text(),ex);
        } catch(java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new IOException(UiText.DIAGNOSTIC_EXPORT_NOT_COMPLETED.text(),ex);
        }
    }
    @Override public void close() { writer.shutdown(); }
}
