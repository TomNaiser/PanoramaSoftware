package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class JogHoldTest {
    @Test public void repeatedPressesNeverQueueTargets() {
        JogHold hold=new JogHold(); assertTrue(hold.begin(1,-1,100));
        for(int i=0;i<100;i++) assertFalse(hold.begin(1,-1,101+i));
        long token=hold.release(); assertEquals(100,token); assertEquals(0,hold.release());
        assertFalse(hold.begin(1,1,300)); hold.finish(token);
        assertTrue(hold.begin(1,1,301)); assertTrue(hold.token>token);
    }
    @Test public void staleStopCannotCancelNextGesture() {
        JogHold hold=new JogHold(); hold.begin(0,1,100); long old=hold.release(); hold.finish(old);
        hold.begin(1,-1,200); hold.finish(old);
        assertTrue(hold.matches(1,-1)); assertFalse(hold.matches(0,1));
    }
    @Test public void releasedStartAckCannotRearmGesture() {
        JogHold hold=new JogHold(); hold.begin(1,-1,100); hold.release();
        assertFalse(hold.held); assertTrue(hold.stopping);
    }
}
