package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class EndpointSelectionTest {
    @Test public void bothAxesRemainFreelySelectableAcrossTheDeviceRange() {
        EndpointSelection p=new EndpointSelection(); p.propose(24,44,-6,-180,180,-50,45);
        assertEquals(44,p.tilt,0); assertEquals(24,p.pan,0); assertTrue(p.pending);
    }
    @Test public void touchIsOnlyAProposalAndCanBeCancelled() {
        EndpointSelection p=new EndpointSelection();
        p.propose(24,-40,-6,-180,180,-50,45);
        assertEquals(-40,p.tilt,0); assertEquals(-6,p.baseTilt,0);
        p.clear(); assertFalse(p.pending);
    }
    @Test public void measuredDeviceLimitsAndCancellationStillApply() {
        EndpointSelection p=new EndpointSelection(); p.propose(300,50,44.5,-180,180,-50,45);
        assertEquals(180,p.pan,0); assertEquals(45,p.tilt,0);
        p.clear(); assertFalse(p.pending);
    }
    @Test public void fullPanOverviewStillClampsAndMarksNarrowerDeviceLimits() {
        EndpointSelection p=new EndpointSelection(); p.propose(170,0,0,-150,150,-45,45);
        assertEquals(150,p.pan,0); assertTrue(p.limited);
    }
}
