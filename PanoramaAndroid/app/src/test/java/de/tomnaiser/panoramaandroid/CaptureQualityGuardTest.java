package de.tomnaiser.panoramaandroid;
import org.junit.Test;
import static org.junit.Assert.*;

public final class CaptureQualityGuardTest {
    private String[] fields(int ordinal,int actualTilt) {
        return ("1|852334391|"+ordinal+"|0|26|6|2|28104|-4013|28000|"+actualTilt+"|0").split("\\|");
    }
    @Test public void realFourPointNineDegreeErrorRequestsOneQualityPause() {
        CaptureQualityGuard guard=new CaptureQualityGuard();
        CaptureQualityGuard.Issue issue=guard.accept(fields(27,879));
        assertNotNull(issue);assertEquals(4892,issue.tiltError);assertEquals(26,issue.index);
        assertNull(guard.accept(fields(27,879)));assertNull(guard.accept(fields(26,879)));
        assertNotNull(guard.accept(fields(28,879)));
    }
    @Test public void ordinaryAndReconstructedCapturesDoNotPause() {
        CaptureQualityGuard guard=new CaptureQualityGuard();
        assertNull(guard.accept(fields(1,-3013)));
        String[] reconstructed=fields(2,879);reconstructed[11]="1";assertNull(guard.accept(reconstructed));
    }
}
