package de.tomnaiser.panoramaandroid;

/** Last accepted manual action in this firmware boot; replayed by GET_STATUS. */
final class ManualActionStatus {
    final int commandSequence, pan, tilt;
    final String kind, result;
    final long durationMs, maximumMs;
    private ManualActionStatus(int sequence,String kind,String result,int pan,int tilt,long duration,long maximum) {
        commandSequence=sequence; this.kind=kind; this.result=result;
        this.pan=pan; this.tilt=tilt; durationMs=duration; maximumMs=maximum;
    }
    static ManualActionStatus parse(String[] f) {
        if(f.length!=7) throw new IllegalArgumentException("Manueller Status unvollständig");
        int sequence=Integer.parseInt(f[0]),pan=Integer.parseInt(f[3]),tilt=Integer.parseInt(f[4]);
        long duration=DeviceState.unsigned(f[5]),maximum=DeviceState.unsigned(f[6]);
        String kind=f[1],result=f[2];
        boolean valid;
        if(kind.equals("NONE")) valid=sequence==0 && result.equals("IDLE") && maximum==0 && duration==0;
        else {
            valid=sequence>=1 && sequence<=65535 && (kind.equals("MOVE") || kind.equals("TRIGGER") || kind.equals("JOG")) && maximum>0 &&
                maximum<=(kind.equals("MOVE")?120000:kind.equals("JOG")?1500:60000) &&
                (result.equals("ACTIVE") || result.equals("CANCELLED") || result.equals("FAULT") ||
                    kind.equals("MOVE") && (result.equals("REACHED") || result.equals("TIMED_OUT") || result.equals("TARGET_MISSED")) ||
                    kind.equals("JOG") && result.equals("REACHED") ||
                    kind.equals("TRIGGER") && result.equals("TRIGGERED"));
        }
        if(!valid) throw new IllegalArgumentException("Manueller Status ungültig");
        return new ManualActionStatus(sequence,kind,result,pan,tilt,duration,maximum);
    }
    boolean active() { return result.equals("ACTIVE"); }
    boolean succeeded() { return result.equals("REACHED") || result.equals("TRIGGERED"); }
}
