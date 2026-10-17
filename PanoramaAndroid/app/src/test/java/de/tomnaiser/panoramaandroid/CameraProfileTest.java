package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class CameraProfileTest {
    @Test public void actual35mmRecipeScalesBothAxesAndKeepsLargeTiltStepOnWire() {
        CameraProfile wide=new CameraProfile("35",35,24,35.9,700,500);
        PanoramaPlan p=PanoramaPlan.create(1579773610L,false,-35.112,22.786,-179.714,-29.487,wide.fovX(),wide.fovY(),40,700,30,500,1000,true);
        assertEquals(22709,p.stepPan);assertEquals(-32582,p.stepTilt);assertEquals(7,p.columns);assertEquals(3,p.rows);
        assertArrayEquals(new int[]{35112,-9796,0,1},p.target(1));
        PanoramaPlan received=PanoramaPlan.parse(p.fields());assertEquals(-32582,received.stepTilt);assertArrayEquals(p.target(2),received.target(2));
    }
    @Test public void profileGeometryMatchesRoundedWindowsFov() {
        CameraProfile tele = new CameraProfile("400",400,24,35.9,700,500);
        assertEquals(3.437,tele.fovX(),.00001);
        assertEquals(5.139,tele.fovY(),.00001);
        CameraProfile wide = new CameraProfile("35",35,24,35.9,700,500);
        PanoramaPlan a=PanoramaPlan.create(1,true,0,-30,90,30,tele.fovX(),tele.fovY(),20,700,30,500,500,false);
        PanoramaPlan b=PanoramaPlan.create(2,true,0,-30,90,30,wide.fovX(),wide.fovY(),20,700,30,500,500,false);
        assertTrue(b.total()<a.total()/4);
    }
    @Test public void polarBandHasSingleShotAndNoDuplicateCircleEndpoint() {
        assertEquals(1,PanoramaPlan.shotsInBand(-180000,180000,90000,30000));
        assertEquals(1,PanoramaPlan.shotsInBand(-180000,180000,-90000,30000));
    }
    @Test(expected=IllegalArgumentException.class) public void invalidOpticsAreRejected() {
        new CameraProfile("NaN",Double.NaN,24,35.9,700,500);
    }
}
