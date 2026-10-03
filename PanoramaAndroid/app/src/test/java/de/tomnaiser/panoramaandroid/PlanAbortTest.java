package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PlanAbortTest {
    @Test public void loadedPlanIsStoppedBeforeClearingAndRequiresIdleStatus() {
        PlanAbort p=new PlanAbort("PLAN_READY");
        assertEquals("STOP",p.next("PLAN_READY",true));
        assertNull(p.next("STOPPED",true));
        p.acknowledged(true); assertNull(p.next("PLAN_READY",true));
        assertEquals("CLEAR_RUN",p.next("STOPPED",true));
        assertNull(p.next("IDLE",true)); assertFalse(p.done());
        p.acknowledged(true); p.next("STOPPED",true); assertFalse(p.done());
        p.next("IDLE",true); assertTrue(p.done());
    }
    @Test public void rejectionNeverSendsClear() {
        PlanAbort p=new PlanAbort("RUNNING"); p.next("RUNNING",true); p.acknowledged(false);
        assertNull(p.next("STOPPED",true)); assertTrue(p.failed());
    }
    @Test public void staleStatusCannotClearAndCompletedNeedsNoStop() {
        PlanAbort p=new PlanAbort("PAUSED"); p.next("PAUSED",true); p.acknowledged(true);
        assertNull(p.next("STOPPED",false)); assertEquals("CLEAR_RUN",p.next("STOPPED",true));
        assertEquals("CLEAR_RUN",new PlanAbort("COMPLETED").next("COMPLETED",true));
    }
    @Test public void idleAndTimelapseAreExcluded() {
        assertFalse(PlanAbort.allowed("IDLE")); assertFalse(PlanAbort.allowed("TIMELAPSE"));
    }
}
