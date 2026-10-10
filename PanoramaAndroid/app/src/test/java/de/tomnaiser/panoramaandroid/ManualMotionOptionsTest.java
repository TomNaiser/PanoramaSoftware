package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ManualMotionOptionsTest {
    @Test public void olderFirmwareReceivesExistingFrameShape() {
        DeviceState state=new DeviceState();
        assertArrayEquals(new String[]{"1000","-2000"},ManualMotionOptions.fields(state,1000,-2000,500));
    }
    @Test public void negotiatedProfileTempoIsPassedForBothAxesWithoutChangingRun() {
        DeviceState state=new DeviceState(); state.state="PAUSED"; state.runId=42; state.completed=7;
        state.accept(P2Codec.decode(P2Codec.encode(1,"MOTION_CAPABILITIES","1")),1000);
        assertArrayEquals(new String[]{"1000","-2000","200","200"},ManualMotionOptions.fields(state,1000,-2000,200));
        assertEquals("PAUSED",state.state); assertEquals(42,state.runId); assertEquals(7,state.completed);
    }
    @Test(expected=IllegalArgumentException.class) public void unknownProfileVersionIsRejected() {
        new DeviceState().accept(P2Codec.decode(P2Codec.encode(1,"MOTION_CAPABILITIES","2")),1000);
    }
    @Test(expected=IllegalArgumentException.class) public void unsafeTempoIsRejectedBeforeSending() {
        DeviceState state=new DeviceState(); state.profiledMotion=true;
        ManualMotionOptions.fields(state,0,0,0);
    }
}
