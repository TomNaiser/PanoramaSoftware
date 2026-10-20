package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class StatusLogPolicyTest {
    private String[] fields(String state,String phase,String error) {
        return ("1|"+state+"|"+phase+"|24|260|6|0|17997|2373|15667|-4219|0|260|"+error+"|24|0|1|24|0|0").split("\\|");
    }
    @Test public void faultImmediatelyAfterMovingStatusIsNotLostForFiveSeconds() {
        StatusLogPolicy p=new StatusLogPolicy();
        assertTrue(p.log(fields("RUNNING","MOVING","NONE"),1000));
        assertFalse(p.log(fields("RUNNING","MOVING","NONE"),1200));
        assertTrue(p.log(fields("FAULT","NONE","MOTION_STALLED"),1300));
        assertTrue(p.log(fields("FAULT","NONE","FAULT_PAN_SENSOR"),1400));
    }
    @Test public void movingSamplesAndPhaseChangesArePreserved() {
        StatusLogPolicy p=new StatusLogPolicy();
        assertTrue(p.log(fields("RUNNING","MOVING","NONE"),1000));
        assertTrue(p.log(fields("RUNNING","MOVING","NONE"),1500));
        assertTrue(p.log(fields("RUNNING","TRIGGER_ACTIVE","NONE"),1600));
        assertFalse(p.log(fields("RUNNING","TRIGGER_ACTIVE","NONE"),1700));
    }
}
