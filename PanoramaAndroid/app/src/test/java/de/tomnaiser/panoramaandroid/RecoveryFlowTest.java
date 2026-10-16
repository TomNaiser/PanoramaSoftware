package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RecoveryFlowTest {
    @Test public void emptyFaultNeedsStopAckAndNewStatusBeforeClear() {
        RecoveryFlow flow=new RecoveryFlow(0);
        assertEquals("ESTOP",flow.next("FAULT",0,10,true,0));
        assertNull(flow.next("STOPPED",0,11,true,10)); // no ACK yet
        flow.acknowledged(true,20);
        assertNull(flow.next("STOPPED",0,10,true,30)); // cached status
        assertNull(flow.next("STOPPED",0,11,false,30));
        assertEquals("CLEAR_RUN",flow.next("STOPPED",0,11,true,40));
        flow.acknowledged(true,50);
        assertNull(flow.next("IDLE",0,11,true,60));
        assertFalse(flow.done());
        flow.next("IDLE",0,12,true,100);
        flow.next("IDLE",0,13,true,600);
        flow.next("IDLE",0,14,true,1100);
        assertTrue(flow.done());
    }
    @Test public void nonemptyRunIsPreservedAndNeverCleared() {
        RecoveryFlow flow=new RecoveryFlow(0);
        assertEquals("ESTOP",flow.next("FAULT",123,4,true,0));
        flow.acknowledged(true,1);
        assertNull(flow.next("STOPPED",123,5,true,10));
        assertNull(flow.next("STOPPED",123,6,true,510));
        assertNull(flow.next("STOPPED",123,7,true,1010));
        assertTrue(flow.done());
    }
    @Test public void persistentSensorFaultCannotBeAcknowledgedIntoReadiness() {
        RecoveryFlow flow=new RecoveryFlow(0);
        flow.next("FAULT",23,1,true,0); flow.acknowledged(true,1);
        flow.next("STOPPED",23,2,true,100);
        flow.next("FAULT",23,3,true,500);
        flow.next("STOPPED",23,4,true,600);
        flow.next("STOPPED",23,5,true,1100);
        assertFalse(flow.done());
        flow.next("FAULT",23,6,true,5001);
        assertTrue(flow.failed());
    }
    @Test public void repeatedCachedStatusDoesNotProveRecovery() {
        RecoveryFlow flow=new RecoveryFlow(0);
        flow.next("FAULT",10,1,true,0); flow.acknowledged(true,1);
        flow.next("STOPPED",10,2,true,10);
        flow.next("STOPPED",10,2,true,1500);
        assertFalse(flow.done());
        flow.next("STOPPED",10,2,true,5001);
        assertTrue(flow.failed());
    }
    @Test public void missingAckAndLateAckRemainBounded() {
        RecoveryFlow flow=new RecoveryFlow(0);
        flow.next("FAULT",0,1,true,0);
        assertNull(flow.next("STOPPED",0,2,true,5000));
        assertTrue(flow.failed());
        flow.acknowledged(true,6000);
        assertNull(flow.next("IDLE",0,3,true,6001));
        assertFalse(flow.done());
    }
    @Test public void rejectionAndUnexpectedRunDoNotRelease() {
        RecoveryFlow rejected=new RecoveryFlow(0);
        rejected.next("FAULT",0,1,true,0); rejected.acknowledged(false,1);
        assertTrue(rejected.failed());
        RecoveryFlow changed=new RecoveryFlow(0);
        changed.next("FAULT",10,1,true,0); changed.acknowledged(true,1);
        changed.next("STOPPED",10,2,true,10);
        changed.next("STOPPED",11,3,true,510);
        assertTrue(changed.failed());
    }
}
