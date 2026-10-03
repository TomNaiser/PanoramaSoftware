package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FrameReaderTest {
    @Test public void partialNoiseAndOversizeResynchronize() {
        FrameReader reader = new FrameReader();
        String noise = "legacy\n@partial" + new String(new char[400]).replace('\0', 'x');
        for (char value : noise.toCharArray()) assertNull(reader.accept(value));
        String wire = P2Codec.encode(3, "STATUS", "0");
        P2Codec.Frame result = null;
        for (char value : wire.toCharArray()) {
            P2Codec.Frame frame = reader.accept(value); if (frame != null) result = frame;
        }
        assertNotNull(result); assertEquals("STATUS", result.type);
    }
    @Test public void corruptFrameCannotChangeState() {
        FrameReader reader = new FrameReader();
        for (char value : P2Codec.encode(3, "STATUS", "0").replace("STATUS", "FAULTS").toCharArray())
            assertNull(reader.accept(value));
    }
}
