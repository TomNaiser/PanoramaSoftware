package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PlotTest {
    @Test public void projectionPreservesSignsAndRoundTripsWithReversedEndpoints() {
        PlotViewport v=new PlotViewport(); v.fit(-54,-51,-6,-1);
        assertEquals(-53.749,v.pan(v.x(-53.749)),.000001);
        assertEquals(-5.605,v.tilt(v.y(-5.605)),.000001);
        assertTrue(v.y(-1)<v.y(-6)); assertTrue(v.x(180)<v.x(-180));
        v.bottom=-25; v.top=25;
        assertEquals(25,v.tilt(0),0); assertEquals(-25,v.tilt(1),0);
    }
    @Test public void singlePointAndFullCircleStayFiniteWhenZoomed() {
        PlotViewport v=new PlotViewport(); v.fit(3,3,4,4);
        assertEquals(.5,v.x(3),.000001); assertEquals(.5,v.y(4),.000001);
        for(int i=0;i<50;i++) v.zoom(.5);
        assertTrue(v.right>v.left && v.top>v.bottom);
        v.fit(-180,180,-90,90); assertTrue(v.x(-180)>v.x(180));
    }
    private P2Codec.Frame status(long id,int pan) {
        return P2Codec.decode(P2Codec.encode(1,"STATUS",""+id,"RUNNING","MOVING","1","4","0","0","51000","1500",""+pan,"1400","0","1","NONE","1","0","1","1","0","0").trim());
    }
    @Test public void telemetryBreaksAtReconnectGapAndRunChangeAndIsBounded() {
        RunPlotModel m=new RunPlotModel(); m.status(status(1,50000),100);
        m.status(status(1,50100),300); assertTrue(m.trace.getLast().joined);
        m.disconnected(); m.status(status(1,50200),500); assertFalse(m.trace.getLast().joined);
        m.status(status(1,50300),4000); assertFalse(m.trace.getLast().joined);
        for(int i=0;i<600;i++) m.status(status(1,50300),4200+i*200);
        assertEquals(512,m.trace.size());
        m.status(status(2,51000),200000); assertEquals(1,m.trace.size()); assertFalse(m.trace.getLast().joined);
    }
    @Test public void captureSeparatesMeasuredActualFromReconstructedTargetAndManual() {
        RunPlotModel.Capture c=RunPlotModel.Capture.parse("R-1-1","IMAGE_COMPLETED|20|1|1|0|0|0|0|50999|1494|50713|1296|0");
        assertEquals(50999,c.targetPan); assertEquals(50713,c.pan); assertFalse(c.reconstructed);
        RunPlotModel.Capture r=RunPlotModel.Capture.parse("R-1-2","IMAGE_COMPLETED|0|1|2|0|1|0|1|50999|5605|50999|5605|1");
        assertTrue(r.reconstructed); assertEquals(5605,r.targetTilt);
        RunPlotModel.Capture manual=RunPlotModel.Capture.parse("M-1","MANUAL_IMAGE_TRIGGERED|3|1|1|0|51086|1516|3251889|0");
        assertEquals(51086,manual.pan); assertEquals(1516,manual.tilt);
    }
}
