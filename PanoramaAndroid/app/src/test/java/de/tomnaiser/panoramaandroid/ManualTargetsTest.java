package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ManualTargetsTest {
    @Test public void actualPositionDoesNotEraseAcceptedSteps() {
        assertEquals(2000,ManualTargets.step(1000,100,1000));
        assertEquals(-2000,ManualTargets.step(-1000,-100,-1000));
    }
    @Test public void overshootStillMovesAWholeStep() {
        assertEquals(2100,ManualTargets.step(1000,1100,1000));
        assertEquals(-2100,ManualTargets.step(-1000,-1100,-1000));
        assertEquals(1000,ManualTargets.step(1000,1100,0));
    }
    @Test(expected=ArithmeticException.class) public void overflowIsRejected() {
        ManualTargets.step(Integer.MAX_VALUE,0,1000);
    }
}
