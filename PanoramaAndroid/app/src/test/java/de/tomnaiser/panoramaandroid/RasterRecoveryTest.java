package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RasterRecoveryTest {
    @Test public void encoderInterruptionRetainsRecoveryAfterDeviceReadinessIsConfirmed() {
        assertTrue("Tilt sensor interruption must retain the recovery checkpoint",RasterRecovery.recoverable("FAULT_TILT_SENSOR"));
        assertTrue("Pan sensor interruption must retain the recovery checkpoint",RasterRecovery.recoverable("FAULT_PAN_SENSOR"));
        assertTrue("Common motion sensor interruption must retain the recovery checkpoint",RasterRecovery.recoverable("MOTION_SENSOR"));
    }
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
        assertFalse(RasterRecovery.recoverable("TARGET_CALCULATION"));
        assertFalse(RasterRecovery.recoverable("INVALID_PHASE"));
    }
    @Test public void sphericalRecoveryRepeatsCompleteBandsWithoutChangingAnyOriginalTarget() {
        for(double[] tilt:new double[][]{{-45,45},{45,-45},{-42.489,27.5},{27.5,-42.489}})
        for(boolean circle:new boolean[]{false,true}) {
            PanoramaPlan p=PanoramaPlan.create(77,true,circle?180:60,tilt[0],circle?-180:-60,tilt[1],30,13,20,500,30,300,1700,true);
            for(int index:new int[]{0,p.total()/2,p.total()-1}) for(boolean previous:new boolean[]{false,true}) {
                RasterRecovery recovery=new RasterRecovery(p,index);
                int start=recovery.startColumn(previous),offset=0;
                assertTrue(start<=Math.max(0,p.target(index)[3]-(previous?1:0)));
                for(int band=0;band<start;band++) offset+=p.shots(band);
                PanoramaPlan rest=recovery.remaining(78,previous);
                assertEquals(1,rest.mode);assertEquals(p.total()-offset,rest.total());
                for(int i=0;i<rest.total();i++) {
                    assertEquals(p.target(offset+i)[0],rest.target(i)[0]);
                    assertEquals(p.target(offset+i)[1],rest.target(i)[1]);
                }
            }
        }
        PanoramaPlan p=PanoramaPlan.parse("77|2|1|-60000|-45000|5|3|30000|30000|10|30|50|500|500|0|0|60000|45000".split("\\|"));
        assertEquals(2,new RasterRecovery(p,6).remaining(78,false).rows);
    }
    @Test public void explicitRecoveryRetainsOtherStoppedAcquisitionsButNotInternalRecipeFaults() {
        for(String reason:new String[]{"FAULT_TILT_LIMIT","CONTROL_TIMEOUT","TX_OVERFLOW","CONNECTION_TIMEOUT","ANDROID_ESTOP","USER_STOP","TARGET_REJECTED"})
            assertTrue(reason,RasterRecovery.recoverable(reason));
        for(String reason:new String[]{"TARGET_CALCULATION","INVALID_PHASE","UNKNOWN"})
            assertFalse(reason,RasterRecovery.recoverable(reason));
    }
    @Test public void asynchronousInterruptionsDoNotPromiseCameraFileOrder() {
        for(String reason:new String[]{"EXPOSURE_UNSTABLE","FAULT_TILT_SENSOR","MOTION_SENSOR","CONTROL_TIMEOUT","TX_OVERFLOW","CONNECTION_TIMEOUT","ANDROID_ESTOP","USER_STOP"})
            assertTrue(reason,RasterRecovery.uncertainExposure(reason));
        for(String reason:new String[]{"CAPTURE_NOT_STABLE","MOTION_STALLED","MOTION_NOT_QUIET","TARGET_REJECTED"})
            assertFalse(reason,RasterRecovery.uncertainExposure(reason));
    }
}
