package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManualLimitRecoveryTest {
    private DeviceState state(int pan,int tilt) {
        DeviceState s=new DeviceState(); s.limitsKnown=s.confirmed=true;
        s.panMin=-180000; s.panMax=180000; s.tiltMin=-45000; s.tiltMax=50000; s.pan=pan; s.tilt=tilt; return s;
    }
    private void reject(DeviceState s,int pan,int tilt) {
        try { s.validateManualTarget(pan,tilt); fail("Outward or unrelated move accepted"); }
        catch(IllegalArgumentException expected) { }
    }
    @Test public void returnsFromBothTiltLimitsInSmallSteps() {
        DeviceState low=state(0,-47373); low.validateManualTarget(0,-47273); low.validateManualTarget(0,-44000);
        reject(low,0,-47473); reject(low,1000,-47273); reject(low,0,51000);
        DeviceState high=state(0,51000); high.validateManualTarget(0,50900); high.validateManualTarget(0,49000);
        reject(high,0,51100); reject(high,0,-46000);
    }
    @Test public void returnsFromPanAndBothAxesWithoutWideningLimits() {
        DeviceState s=state(181000,0); s.validateManualTarget(180900,0); reject(s,181100,0); reject(s,180900,100);
        s=state(181000,-46000); s.validateManualTarget(180900,-46000); s.validateManualTarget(181000,-45900); reject(s,181100,-45900);
        assertEquals(-45000,s.tiltMin); assertEquals(180000,s.panMax);
    }
    @Test public void stepsClampAtBoundaryAndKeepRecoverySteps() {
        assertEquals(50000,DeviceState.manualStep(49900,50900,-45000,50000));
        assertEquals(-45000,DeviceState.manualStep(-44900,-45900,-45000,50000));
        assertEquals(-47273,DeviceState.manualStep(-47373,-47273,-45000,50000));
    }
    @Test public void unconfirmedLimitsStillBlockRecovery() {
        DeviceState s=state(0,-46000); s.confirmed=false; reject(s,0,-45000);
    }
}
