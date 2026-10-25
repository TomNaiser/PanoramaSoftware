package de.tomnaiser.panoramaandroid;

/** Additive CAPTURE_WARNING event; identity is run + capture ordinal, not STATUS. */
final class CaptureWarning {
    final long runId, ordinal;
    final int flags;
    private CaptureWarning(long run,long capture,int flags) {runId=run;ordinal=capture;this.flags=flags;}
    static CaptureWarning parse(String[] fields) {
        if(fields.length!=3) throw new IllegalArgumentException(UiText.INCOMPLETE_CAPTURE_WARNING.text());
        long run=DeviceState.unsigned(fields[0]),ordinal=DeviceState.unsigned(fields[1]);
        int flags=Integer.parseInt(fields[2]);
        if(run==0 || ordinal==0 || flags<1 || flags>15) throw new IllegalArgumentException(UiText.INVALID_CAPTURE_WARNING.text());
        return new CaptureWarning(run,ordinal,flags);
    }
    static String describe(int flags) {
        java.util.List<String> reasons=new java.util.ArrayList<>();
        if((flags&1)!=0) reasons.add(UiText.PAN_DEVIATION_EXCEEDS_TOLERANCE.text());
        if((flags&2)!=0) reasons.add(UiText.TILT_DEVIATION_EXCEEDS_TOLERANCE.text());
        if((flags&4)!=0) reasons.add(UiText.MOVEMENT_TIME_LIMIT_REACHED.text());
        if((flags&8)!=0) reasons.add(UiText.TARGET_NOT_REACHED_PRECISELY.text());
        return String.join(" · ",reasons);
    }
}
