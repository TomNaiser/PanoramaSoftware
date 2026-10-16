package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class IncidentTrackerTest {
    @Test public void repeatedFaultAfterAcknowledgementDoesNotReopenDialog() {
        IncidentTracker tracker=new IncidentTracker();
        tracker.observe("boot|MOTION_STALLED","Fahrt unterbrochen");
        long id=tracker.id; assertTrue(tracker.needsAcknowledgement());
        assertTrue(tracker.acknowledge(id));
        for(int n=0;n<100;n++) tracker.observe("boot|MOTION_STALLED","Fahrt unterbrochen");
        assertEquals(id,tracker.id); assertFalse(tracker.needsAcknowledgement());
        tracker.resolved(); tracker.observe("boot|MOTION_STALLED","Neue Fahrt unterbrochen");
        assertTrue(tracker.needsAcknowledgement()); assertNotEquals(id,tracker.id);
        assertFalse(tracker.acknowledge(id));
    }
    @Test public void restoredAcknowledgementAndNewCauseAreDistinct() {
        IncidentTracker restored=new IncidentTracker();
        restored.id=42; restored.key="sensor"; restored.active=true; restored.acknowledged=true;
        restored.observe("sensor","Noch keine Werte");
        assertFalse(restored.needsAcknowledgement()); assertEquals(42,restored.id);
        restored.observe("other-fault","Neuer Fehler");
        assertTrue(restored.needsAcknowledgement()); assertEquals(43,restored.id);
    }
}
