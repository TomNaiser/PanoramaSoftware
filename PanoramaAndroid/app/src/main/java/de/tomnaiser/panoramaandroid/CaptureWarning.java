package de.tomnaiser.panoramaandroid;

/** Additive CAPTURE_WARNING event; identity is run + capture ordinal, not STATUS. */
final class CaptureWarning {
    final long runId, ordinal;
    final int flags;
    private CaptureWarning(long run,long capture,int flags) {runId=run;ordinal=capture;this.flags=flags;}
    static CaptureWarning parse(String[] fields) {
        if(fields.length!=3) throw new IllegalArgumentException("Aufnahmewarnung unvollständig");
        long run=DeviceState.unsigned(fields[0]),ordinal=DeviceState.unsigned(fields[1]);
        int flags=Integer.parseInt(fields[2]);
        if(run==0 || ordinal==0 || flags<1 || flags>15) throw new IllegalArgumentException("Aufnahmewarnung ungültig");
        return new CaptureWarning(run,ordinal,flags);
    }
    static String describe(int flags) {
        java.util.List<String> reasons=new java.util.ArrayList<>();
        if((flags&1)!=0) reasons.add("Pan-Abweichung über Toleranz");
        if((flags&2)!=0) reasons.add("Tilt-Abweichung über Toleranz");
        if((flags&4)!=0) reasons.add("Fahrzeitgrenze erreicht");
        if((flags&8)!=0) reasons.add("Ziel nicht genau erreicht");
        return String.join(" · ",reasons);
    }
}
