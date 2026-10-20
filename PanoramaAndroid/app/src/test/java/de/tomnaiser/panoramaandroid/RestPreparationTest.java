package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RestPreparationTest {
    @Test public void acknowledgementsAndFreshMatchingRecipeAreRequiredAndNeverStart() {
        RestPreparation p=new RestPreparation(12,34);
        assertEquals("CLEAR_RUN",p.next("STOPPED",12,10,true,false));
        assertNull(p.next("IDLE",0,11,true,false));assertFalse(p.done());
        p.acknowledged(true);
        assertNull(p.next("IDLE",0,10,true,false));assertNull(p.next("IDLE",0,11,false,false));
        assertEquals("LOAD_PLAN",p.next("IDLE",0,11,true,false));
        assertNull(p.next("PLAN_READY",34,12,true,true));assertFalse(p.done());
        p.acknowledged(true);assertNull(p.next("PLAN_READY",34,12,true,true));assertTrue(p.done());
    }
    @Test public void wrongRunWrongRecipeRejectionOrFaultStayBlocked() {
        RestPreparation p=new RestPreparation(12,34);
        assertNull(p.next("FAULT",12,1,true,false));assertTrue(p.failed());
        p=new RestPreparation(12,34);p.next("STOPPED",12,1,true,false);p.acknowledged(false);assertTrue(p.failed());
        p=new RestPreparation(12,34);p.next("STOPPED",12,1,true,false);p.acknowledged(true);
        p.next("IDLE",0,2,true,false);p.acknowledged(true);p.next("PLAN_READY",34,3,true,false);assertTrue(p.failed());
        p=new RestPreparation(12,34);p.next("STOPPED",12,1,true,false);p.acknowledged(true);
        p.next("STOPPED",99,2,true,false);assertTrue(p.failed());
    }
    @Test public void interruptedTransferCanLoadFromConfirmedIdleButNeverStart() {
        RestPreparation p=new RestPreparation(12,34,true);
        assertEquals("LOAD_PLAN",p.next("IDLE",0,5,true,false));
        p.acknowledged(true);
        assertNull(p.next("PLAN_READY",34,5,true,true));assertFalse(p.done());
        assertNull(p.next("PLAN_READY",34,6,true,true));assertTrue(p.done());
    }
}
