package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PanoramaPlanTest {
    @Test public void sphericalMatchesWindowsReference() {
        PanoramaPlan p = PanoramaPlan.parse(new String[]{"77","2","1","-60000","-45000","5","3","30000","30000","10","30","50","500","500","0","0","60000","45000"});
        assertEquals(13, p.total()); assertArrayEquals(new int[]{-60000,-45000,0,0}, p.target(0));
        assertArrayEquals(new int[]{60000,-45000,3,0}, p.target(3));
        assertArrayEquals(new int[]{-60000,0,0,1}, p.target(4));
        assertArrayEquals(new int[]{60000,45000,3,2}, p.target(12)); assertEquals(18, p.fields().length);
    }
    @Test public void fullCircleDoesNotRepeatEndpoint() {
        PanoramaPlan p = PanoramaPlan.create(78,true,180,0,-180,0,90,30,0,10,30,50,500,false);
        assertEquals(4, p.total()); assertEquals(90000, p.target(3)[0]);
    }
    @Test public void rasterMatchesDeviceCoordinateContract() {
        PanoramaPlan p = PanoramaPlan.create(5,false,0,0,10,-10,5,5,0,10,30,50,500,false);
        assertEquals(9,p.total()); assertArrayEquals(new int[]{-10000,-10000,2,2},p.target(8));
        assertEquals(-12500,DeviceState.toDevice(12.5));
        assertEquals(-1,DeviceState.toDevice(.0005)); assertEquals(1,DeviceState.toDevice(-.0005));
    }
    @Test public void positiveTiltRaisesLensAndIsPreservedInRecipeAndMoves() {
        assertEquals(50000,DeviceState.toTiltDevice(50));
        assertEquals(100,DeviceState.toTiltDevice(.1)); assertEquals(-100,DeviceState.toTiltDevice(-.1));
        PanoramaPlan p=PanoramaPlan.create(6,false,0,45,0,45,5,5,0,10,30,50,500,false);
        assertEquals(45000,p.target(0)[1]);
        DeviceState limits=new DeviceState(); limits.limitsKnown=limits.confirmed=true;
        limits.panMin=-180000; limits.panMax=180000; limits.tiltMin=-45000; limits.tiltMax=50000;
        p.validate(limits);
    }
    @Test(expected=IllegalArgumentException.class) public void unconfirmedLimitsRejectPlan() {
        PanoramaPlan.create(5,false,0,0,1,1,5,5,0,10,30,50,500,false).validate(new DeviceState());
    }
    @Test(expected=IllegalArgumentException.class) public void tooManyImagesAreRejected() {
        PanoramaPlan.create(5,false,0,0,360,180,1,1,0,10,30,50,500,false);
    }
    @Test public void planSurvivesInitialStatusAndFreshnessExpires() {
        DeviceState s = new DeviceState();
        PanoramaPlan p = PanoramaPlan.create(5,false,0,0,1,1,5,5,0,10,30,50,500,false);
        s.accept(P2Codec.decode(P2Codec.encode(1,"DEVICE_INFO","2.2","date","2","R4")),100);
        s.accept(P2Codec.decode(P2Codec.encode(2,"PLAN",p.fields())),100);
        s.accept(P2Codec.decode(P2Codec.encode(3,"STATUS","5","PLAN_READY","NONE","0","1","0","0","0","0","0","0","0","0","NONE","0","0","1","0")),100);
        assertNotNull(s.plan); assertTrue(s.fresh(101)); assertFalse(s.fresh(3000));
    }
    @Test public void actualTiltOutsideConfirmedLimitsBlocksMotion() {
        DeviceState s = new DeviceState();
        s.accept(P2Codec.decode(P2Codec.encode(1,"CAPABILITIES","-180000","180000","-45000","50000","1")),100);
        s.tilt = 82793; assertFalse(s.positionSafe());
        s.tilt = 50000; assertTrue(s.positionSafe());
        s.confirmed = false; assertFalse(s.positionSafe());
    }
}
