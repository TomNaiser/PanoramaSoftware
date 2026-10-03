package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TiltDirectionTest {
    @Test public void upSendsIncreasingEncoderTarget() {
        // The physical test establishes: increasing measured tilt lifts the lens.
        assertEquals(1000,DeviceState.toTiltDevice(1));
        assertEquals(-1000,DeviceState.toTiltDevice(-1));
        assertEquals(-1000,DeviceState.toDevice(1)); // Pan keeps its established convention.
    }
    @Test public void graphicalPlanAndManualArrowUseTheSameDirection() {
        PanoramaPlan plan=PanoramaPlan.create(1,false,0,2,0,4,3,2,0,700,30,500,500,false);
        assertEquals(2000,plan.startTilt);
        assertEquals(4000,plan.target(1)[1]);
    }
    @Test public void calibrationReportsShiftedAbsoluteLimits() {
        DeviceState state=new DeviceState();
        state.accept(P2Codec.decode(P2Codec.encode(1,"TILT_CALIBRATION","5000","-55000","50000")),100);
        assertTrue(state.tiltCalibrationKnown);
        assertEquals(5000,state.tiltZeroOffset); assertEquals(-55000,state.tiltAbsoluteMin); assertEquals(50000,state.tiltAbsoluteMax);
    }
    @Test(expected=IllegalArgumentException.class) public void invalidCalibrationIsRejected() {
        new DeviceState().accept(P2Codec.decode(P2Codec.encode(1,"TILT_CALIBRATION","5000","50000","-55000")),100);
    }
}
