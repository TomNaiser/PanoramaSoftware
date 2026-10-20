package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public final class SupportLogTest {
    @Test public void exportFlushesPendingFaultAndIncludesPreviousRotationsInOrder() throws Exception {
        Path directory=Files.createTempDirectory("panorama-support-test");
        try(SupportLog log=new SupportLog(directory.toFile(),1,3)) {
            log.append("first");log.append("second");log.append("fault");
            String snapshot=log.snapshot();
            assertTrue(snapshot.contains("first"));assertTrue(snapshot.contains("second"));assertTrue(snapshot.contains("fault"));
            assertTrue(snapshot.indexOf("first")<snapshot.indexOf("second"));
            assertTrue(snapshot.indexOf("second")<snapshot.indexOf("fault"));
            log.append("after");snapshot=log.snapshot();
            assertFalse(snapshot.contains("first"));assertTrue(snapshot.contains("fault"));assertTrue(snapshot.contains("after"));
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.list(directory)) {
                for(Path path:(Iterable<Path>)paths::iterator) Files.delete(path);
            }
            Files.delete(directory);
        }
    }
}
