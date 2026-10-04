package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class NavigationStateTest {
    private DeviceState state(String name,boolean extended) {
        DeviceState s=new DeviceState(); s.compatible=true; s.lastStatusMs=1000;
        s.state=name; s.inactiveNavigation=extended; return s;
    }
    @Test public void inactiveNavigationRequiresDeviceCapability() {
        for(String name:new String[]{"PLAN_READY","STOPPED","COMPLETED"}) {
            assertFalse(state(name,false).navigation(1100));
            assertTrue(state(name,true).navigation(1100));
        }
    }
    @Test public void recordingNeverAllowsArrowsEvenWithCapability() {
        for(String name:new String[]{"RUNNING","PAUSE_PENDING","TIMELAPSE","FAULT","UNKNOWN"})
            assertFalse(name,state(name,true).navigation(1100));
    }
    @Test public void pauseAndIdleWorkWithExistingFirmware() {
        assertTrue(state("IDLE",false).navigation(1100));
        assertTrue(state("PAUSED",false).navigation(1100));
        assertFalse(state("COMPLETED",true).manual(1100));
    }
    @Test public void staleDataCannotEnableNavigation() {
        assertFalse(state("PLAN_READY",true).navigation(3500));
        DeviceState s=state("IDLE",true); s.compatible=false; assertFalse(s.navigation(1100));
    }
    @Test public void capabilityIsStrictAndDoesNotAlterRun() {
        DeviceState s=state("PAUSED",false); s.runId=42; s.completed=7;
        s.accept(P2Codec.decode(P2Codec.encode(1,"NAVIGATION_CAPABILITIES","1")),1100);
        assertTrue(s.inactiveNavigation); assertEquals("PAUSED",s.state);
        assertEquals(42,s.runId); assertEquals(7,s.completed);
        s.accept(P2Codec.decode(P2Codec.encode(2,"NAVIGATION_CAPABILITIES","0")),1100);
        assertFalse(s.inactiveNavigation);
    }
    @Test(expected=IllegalArgumentException.class) public void malformedCapabilityRejected() {
        state("IDLE",false).accept(P2Codec.decode(P2Codec.encode(1,"NAVIGATION_CAPABILITIES","yes")),1100);
    }
}
