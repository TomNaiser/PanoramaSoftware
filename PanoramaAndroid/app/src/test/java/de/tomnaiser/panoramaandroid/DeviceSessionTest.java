package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DeviceSessionTest {
    @Test public void reconnectKeepsDraftButPowerCycleClearsIt() {
        assertFalse(new DeviceSession(42).accept(42));
        assertTrue(new DeviceSession(42).accept(43));
        DeviceSession live=new DeviceSession(42);
        assertFalse(live.accept(42));
        assertFalse(live.accept(42));
        assertTrue(live.accept(43));
        assertFalse(live.accept(43));
    }
    @Test public void firstConnectionAndWrapAreHandled() {
        assertFalse(new DeviceSession(-1).accept(1));
        assertTrue(new DeviceSession(4294967295L).accept(1));
    }
    @Test public void unsupportedIdentityClearsOncePerConnection() {
        DeviceSession live=new DeviceSession(0);
        assertTrue(live.accept(0));
        assertFalse(live.accept(0));
    }
}
