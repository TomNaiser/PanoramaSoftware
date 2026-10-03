package de.tomnaiser.panoramaandroid;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class P2CodecTest {
    @Test public void crcMatchesCcittFalseReference() {
        assertEquals(0x29B1, P2Codec.crc("123456789"));
    }

    @Test public void helloRoundTrip() {
        String wire = P2Codec.encode(42, "HELLO", "PanoramaAndroid");
        assertEquals("@P2|42|HELLO|PanoramaAndroid|1950\r\n", wire);
        P2Codec.Frame frame = P2Codec.decode(wire);
        assertEquals(42, frame.sequence);
        assertEquals("HELLO", frame.type);
        assertArrayEquals(new String[] {"PanoramaAndroid"}, frame.fields);
    }

    @Test(expected = IllegalArgumentException.class)
    public void damagedCommandDoesNotDecode() {
        P2Codec.decode(P2Codec.encode(42, "HELLO", "PanoramaAndroid").replace("HELLO", "START"));
    }
}
