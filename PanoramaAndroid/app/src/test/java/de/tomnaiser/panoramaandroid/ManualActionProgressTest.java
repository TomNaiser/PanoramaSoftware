package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ManualActionProgressTest {
    private ManualActionStatus status(int sequence,String kind,String result,int pan,int tilt) {
        return ManualActionStatus.parse(new String[]{""+sequence,kind,result,""+pan,""+tilt,"200","120000"});
    }
    @Test public void repeatedTerminalStatusCompletesWithoutAnyDebugEventOrActiveStatus() {
        ManualActionProgress progress=new ManualActionProgress(42,"MOVE",1000,2000,10);
        assertEquals(ManualActionProgress.Outcome.SUCCEEDED,progress.accept(status(42,"MOVE","REACHED",1000,2000),11));
    }
    @Test public void oldCommandCachedStatusWrongTargetAndActiveStatusCannotComplete() {
        ManualActionProgress progress=new ManualActionProgress(42,"MOVE",1000,2000,10);
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(41,"MOVE","REACHED",1000,2000),11));
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(42,"MOVE","REACHED",1000,2000),10));
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(42,"MOVE","REACHED",1000,2100),11));
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(42,"MOVE","ACTIVE",1000,2000),11));
    }
    @Test public void interruptedActionDoesNotPretendToReachTheTarget() {
        ManualActionProgress progress=new ManualActionProgress(42,"MOVE",1000,2000,10);
        for(String result:new String[]{"CANCELLED","FAULT","TIMED_OUT"})
            assertEquals(ManualActionProgress.Outcome.INTERRUPTED,progress.accept(status(42,"MOVE",result,1000,2000),11));
    }
    @Test public void sequenceWrapUsesExactAckIdentity() {
        ManualActionProgress progress=new ManualActionProgress(1,"MOVE",-1000,-2000,10);
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(65535,"MOVE","REACHED",-1000,-2000),11));
        assertEquals(ManualActionProgress.Outcome.SUCCEEDED,progress.accept(status(1,"MOVE","REACHED",-1000,-2000),12));
    }
    @Test public void triggerCompletionDoesNotRequireImageEventOrMatchingCoordinates() {
        ManualActionProgress progress=new ManualActionProgress(42,"TRIGGER",0,0,10);
        ManualActionStatus completed=ManualActionStatus.parse(new String[]{"42","TRIGGER","TRIGGERED","3000","4000","30","30"});
        assertEquals(ManualActionProgress.Outcome.SUCCEEDED,progress.accept(completed,11));
        assertEquals(ManualActionProgress.Outcome.WAITING,progress.accept(status(42,"MOVE","REACHED",0,0),12));
    }
    @Test public void invalidStatusCannotOverwritePreviouslyValidatedState() {
        DeviceState device=new DeviceState();
        device.accept(new P2Codec.Frame(1,"MANUAL_STATUS",new String[]{"42","MOVE","REACHED","1000","2000","200","120000"}),1000);
        for(String[] fields:new String[][]{
            {"42","MOVE","TRIGGERED","1000","2000","200","120000"},
            {"0","MOVE","REACHED","1000","2000","200","120000"},
            {"42","TRIGGER","REACHED","1000","2000","200","30"},
            {"42","MOVE","REACHED","1000","2000","-1","120000"},
            {"42","MOVE","REACHED","1000","2000","200","120001"}}) {
            try { device.accept(new P2Codec.Frame(2,"MANUAL_STATUS",fields),1100); fail("Invalid frame accepted"); }
            catch(IllegalArgumentException expected) { }
            assertEquals("REACHED",device.manualStatus.result); assertEquals(1,device.manualStatusVersion); assertEquals(1000,device.lastManualStatusMs);
        }
    }
    @Test public void capabilityAndStatusFreshnessAreSeparateFromOrdinaryStatus() {
        DeviceState device=new DeviceState();
        device.accept(new P2Codec.Frame(1,"MANUAL_STATUS_CAPABILITIES",new String[]{"1"}),1000);
        assertTrue(device.manualStatusSupported); assertFalse(device.manualStatusFresh(1000));
        device.accept(new P2Codec.Frame(2,"MANUAL_STATUS",new String[]{"0","NONE","IDLE","0","0","0","0"}),1100);
        assertTrue(device.manualStatusFresh(3599)); assertFalse(device.manualStatusFresh(3600));
    }
}
