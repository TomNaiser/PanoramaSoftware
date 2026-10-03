package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class CameraProfileTest {
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
