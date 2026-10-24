package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmptyPlanTest {
    @Test public void stoppedHeadWithoutRunClearsLoadedPlan() {
        DeviceState state=new DeviceState();
        state.plan=PanoramaPlan.create(1,false,0,0,0,0,3,3,20,10,30,50,500,false);
        state.accept(P2Codec.decode(P2Codec.encode(1,"PLAN",
            "0","1","0","0","0","0","0","0","0","0","30","0","1000","1000","0","0")),1);
        assertNull(state.plan);
    }
    @Test(expected=IllegalArgumentException.class) public void malformedEmptyPlanIsStillRejected() {
        new DeviceState().accept(P2Codec.decode(P2Codec.encode(1,"PLAN","0","garbage")),1);
    }
}
