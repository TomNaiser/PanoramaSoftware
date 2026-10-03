package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ManualMoveProgressTest {
    private String[] event(int seq, int code, int pan, int tilt) {
        return new String[]{""+seq,"3841766",""+code,"0","0","IDLE","NONE",""+pan,""+tilt,"22700","200","-700","0","0"};
    }
    @Test public void stoppedMotorWithZeroStableSamplesCompletesFromFirmwareEvent() {
        ManualMoveProgress progress = new ManualMoveProgress(40,22028,264);
        assertFalse(progress.accept(event(41,13,22000,200)));
        assertTrue(progress.accept(event(42,14,22000,200)));
    }
    @Test public void historicalOrUnpairedCompletionDoesNotCompleteNewMove() {
        ManualMoveProgress progress = new ManualMoveProgress(40,22028,264);
        assertFalse(progress.accept(event(39,13,22000,200)));
        assertFalse(progress.accept(event(40,14,22000,200)));
        assertFalse(progress.accept(event(41,14,22000,200)));
        assertFalse(progress.accept(event(42,13,23000,200)));
        assertFalse(progress.accept(event(43,14,22000,200)));
    }
    @Test public void sequenceWrapAndNegativeTargetRoundingAreSupported() {
        ManualMoveProgress progress = new ManualMoveProgress(65535,-22028,-264);
        assertFalse(progress.accept(event(1,13,-21900,-200)));
        assertTrue(progress.accept(event(2,14,-21900,-200)));
    }
}
