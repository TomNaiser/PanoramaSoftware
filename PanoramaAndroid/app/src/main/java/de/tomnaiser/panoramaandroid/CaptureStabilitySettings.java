package de.tomnaiser.panoramaandroid;

/** Device-confirmed capture guards; independent of target warning tolerances. */
final class CaptureStabilitySettings {
    final int spanMdeg, panDriftMdeg, tiltDriftMdeg, extraWaitMs;
    final long revision;
    CaptureStabilitySettings(int span,int pan,int tilt,int wait,long revision) {
        if(span<44 || span>1000 || pan<44 || pan>1000 || tilt<44 || tilt>1500 ||
            wait<1000 || wait>120000 || revision<0 || revision>4294967295L)
            throw new IllegalArgumentException(UiText.STABILITY_VARIATION_PAN_0_044_1_TILT_0.text());
        spanMdeg=span;panDriftMdeg=pan;tiltDriftMdeg=tilt;extraWaitMs=wait;this.revision=revision;
    }
    static CaptureStabilitySettings parse(String[] fields) {
        if(fields.length!=5) throw new IllegalArgumentException(UiText.INCOMPLETE_STABILITY_LIMITS.text());
        return new CaptureStabilitySettings(Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),
            Integer.parseInt(fields[2]),Integer.parseInt(fields[3]),DeviceState.unsigned(fields[4]));
    }
    static CaptureStabilitySettings fromUi(double span,double pan,double tilt,double seconds,long revision) {
        if(!Double.isFinite(span) || !Double.isFinite(pan) || !Double.isFinite(tilt) || !Double.isFinite(seconds) ||
            span<.044 || span>1 || pan<.044 || pan>1 || tilt<.044 || tilt>1.5 || seconds<1 || seconds>120)
            throw new IllegalArgumentException(UiText.ANGLE_OUTSIDE_THE_ALLOWED_RANGE_OR_WAIT_TIME.text());
        return new CaptureStabilitySettings((int)Math.round(span*1000),(int)Math.round(pan*1000),
            (int)Math.round(tilt*1000),(int)Math.round(seconds*1000),revision);
    }
    String[] fields() {
        if(revision==4294967295L) throw new IllegalArgumentException(UiText.DEVICE_CONFIGURATION_REVISION_EXHAUSTED.text());
        return new String[]{""+spanMdeg,""+panDriftMdeg,""+tiltDriftMdeg,""+extraWaitMs,""+revision};
    }
    boolean sameValues(CaptureStabilitySettings other) {
        return other!=null && spanMdeg==other.spanMdeg && panDriftMdeg==other.panDriftMdeg &&
            tiltDriftMdeg==other.tiltDriftMdeg && extraWaitMs==other.extraWaitMs;
    }
}
