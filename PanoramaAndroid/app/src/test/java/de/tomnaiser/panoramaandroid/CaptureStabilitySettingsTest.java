package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class CaptureStabilitySettingsTest {
    @Test public void parsesConfirmedDeviceValuesAndRevision() {
        CaptureStabilitySettings s=CaptureStabilitySettings.parse(new String[]{"250","300","400","15000","25"});
        assertEquals(250,s.spanMdeg);assertEquals(300,s.panDriftMdeg);assertEquals(400,s.tiltDriftMdeg);
        assertArrayEquals(new String[]{"250","300","400","15000","25"},s.fields());
    }
    @Test public void encodesUiDegreesAndSeconds() {
        assertArrayEquals(new String[]{"350","500","600","20000","26"},CaptureStabilitySettings.fromUi(.35,.5,.6,20,26).fields());
    }
    @Test public void rejectsInvalidValuesBeforeAnySend() {
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,.0439,1.001}) {
            try {CaptureStabilitySettings.fromUi(bad,.25,.4,15,25);fail("Invalid span accepted");}
            catch(IllegalArgumentException expected) { }
        }
        try {CaptureStabilitySettings.parse(new String[]{"250","250","1501","15000","25"});fail();}
        catch(IllegalArgumentException expected) { }
        try {CaptureStabilitySettings.fromUi(.25,.25,.4,.999,25);fail();}
        catch(IllegalArgumentException expected) { }
        try {CaptureStabilitySettings.parse(new String[]{"250","250","400","15000"});fail();}
        catch(IllegalArgumentException expected) { }
    }
    @Test public void deviceStateUsesOnlyValidatedSettings() {
        DeviceState s=new DeviceState();assertNull(s.captureStability);
        s.accept(new P2Codec.Frame(1,"CAPTURE_STABILITY_SETTINGS",new String[]{"250","250","400","15000","25"}),10);
        assertEquals(25,s.motionConfigRevision);assertEquals(250,s.captureStability.spanMdeg);
        try {s.accept(new P2Codec.Frame(2,"CAPTURE_STABILITY_SETTINGS",new String[]{"250","250","400","0","26"}),11);fail();}
        catch(IllegalArgumentException expected) { }
        assertEquals(25,s.motionConfigRevision);assertEquals(15000,s.captureStability.extraWaitMs);
    }
    @Test public void recoveryPauseNeverIncludesSafetyFaults() {
        DeviceState s=new DeviceState();s.recoveryPauseSupported=true;s.runId=1;s.state="PAUSED";
        for(String code:new String[]{"EXPOSURE_UNSTABLE","CAPTURE_NOT_STABLE","MOTION_STALLED","MOTION_SETTLE_TIMEOUT"}) {
            s.lastError=code;assertTrue(RunRecoveryPause.active(s));
        }
        for(String code:new String[]{"FAULT_PAN_SENSOR","MOTION_SENSOR","PAN_LIMIT","CONTROL_TIMEOUT","ESTOP"}) {
            s.lastError=code;assertFalse(RunRecoveryPause.active(s));
        }
        s.lastError="EXPOSURE_UNSTABLE";s.state="FAULT";assertFalse(RunRecoveryPause.active(s));
        s.state="PAUSED";s.recoveryPauseSupported=false;assertFalse(RunRecoveryPause.active(s));
    }
}
