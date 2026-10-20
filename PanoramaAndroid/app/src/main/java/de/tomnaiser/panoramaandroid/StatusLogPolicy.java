package de.tomnaiser.panoramaandroid;

/** Preserve every run/phase/error transition and 500 ms moving samples; throttle idle telemetry. */
final class StatusLogPolicy {
    private String last="";
    private long logged=-5000;
    boolean log(String[] f,long now) {
        String key=f[0]+"|"+f[1]+"|"+f[2]+"|"+f[13];
        long interval=f[2].equals("MOVING") || f[2].equals("SETTLING") ? 500 : 5000;
        boolean result=!key.equals(last) || now-logged>=interval;
        if(result) { logged=now;last=key; }
        return result;
    }
}
