package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RasterRecoveryTest {
    private PanoramaPlan original() {
        return PanoramaPlan.parse("94859692|1|0|5625|2373|65|4|2062|-3083|500|300|300|1700|1700|1|5000".split("\\|"));
    }
    @Test public void realFailedColumnStartsAtExactOriginalTarget() {
        PanoramaPlan original=original();RasterRecovery recovery=new RasterRecovery(original,24);
        PanoramaPlan rest=recovery.remaining(123,false);
        assertEquals(6,recovery.startColumn(false));assertEquals(59,rest.columns);
        assertEquals(17997,rest.startPan);assertEquals(2373,rest.startTilt);
        for(int i=0;i<rest.total();i++) {
            int[] a=original.target(24+i),b=rest.target(i);
            assertEquals(a[0],b[0]);assertEquals(a[1],b[1]);
        }
        assertEquals(1700,rest.panSpeed);assertEquals(300,rest.pulseMs);assertEquals(500,rest.settleMs);
    }
    @Test public void partialColumnAndRepeatedRecoveryPreserveTheGrid() {
        RasterRecovery recovery=new RasterRecovery(original(),27);
        PanoramaPlan rest=recovery.remaining(123,true);
        assertEquals(5,recovery.startColumn(true));assertEquals(original().target(20)[0],rest.startPan);
        PanoramaPlan again=new RasterRecovery(rest,6).remaining(124,false);
        assertEquals(original().target(24)[0],again.startPan);
        assertEquals(59,again.columns);
        assertEquals(0,new RasterRecovery(original(),0).startColumn(true));
    }
    @Test public void negativePanStepsAndSingleColumnRemainExact() {
        PanoramaPlan p=PanoramaPlan.parse("1|1|0|60000|2000|3|4|-2062|-3083|500|300|300|500|700|0|0".split("\\|"));
        PanoramaPlan rest=new RasterRecovery(p,11).remaining(2,false);
        assertEquals(1,rest.columns);assertEquals(55876,rest.startPan);assertEquals(700,rest.tiltSpeed);
    }
    @Test public void completedRunAndReusedIdentityCannotResume() {
        try { new RasterRecovery(original(),260);fail(); } catch(IllegalArgumentException expected) { }
        try { new RasterRecovery(original(),-1);fail(); } catch(IllegalArgumentException expected) { }
        try { new RasterRecovery(original(),24).remaining(original().runId,false);fail(); } catch(IllegalArgumentException expected) { }
        assertFalse(RasterRecovery.recoverable("FAULT_PAN_SENSOR"));
        assertFalse(RasterRecovery.recoverable("USER_STOP"));
    }
}
