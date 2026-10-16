package de.tomnaiser.panoramaandroid;

/** Bounded legacy recovery: stop, fresh status, clear only an empty run, confirm quiet status. */
final class RecoveryFlow {
    private String phase="START";
    private long deadline, baseline, healthySince=-1;
    private int healthyStatuses;
    private long lastHealthyVersion=-1;
    private long expectedRun;
    RecoveryFlow(long now) { deadline=now+5000; }
    String next(String state,long run,long version,boolean fresh,long now) {
        if(done() || failed()) return null;
        if(now>=deadline) { phase="FAILED"; return null; }
        if(phase.equals("START")) { baseline=version; phase="STOP_ACK"; return "ESTOP"; }
        if(!fresh || version<=baseline) return null;
        if(phase.equals("STOP_STATUS")) {
            if(!(state.equals("STOPPED") || state.equals("TIMELAPSE_STOPPED"))) return null;
            expectedRun=run;
            if(run==0) { baseline=version; phase="CLEAR_ACK"; return "CLEAR_RUN"; }
            phase="VERIFY";
        }
        if(phase.equals("IDLE_STATUS") && state.equals("IDLE") && run==0) phase="VERIFY";
        if(phase.equals("VERIFY")) {
            if(run!=expectedRun) { phase="FAILED"; return null; }
            if(!(state.equals("IDLE") || state.equals("STOPPED") || state.equals("TIMELAPSE_STOPPED"))) {
                healthySince=-1; healthyStatuses=0; return null;
            }
            if(version!=lastHealthyVersion) {
                lastHealthyVersion=version; healthyStatuses++;
                if(healthySince<0) healthySince=now;
                if(healthyStatuses>=3 && now-healthySince>=1000) phase="DONE";
            }
        }
        return null;
    }
    void acknowledged(boolean accepted,long now) {
        if(done() || failed()) return;
        if(!accepted) { phase="FAILED"; return; }
        if(phase.equals("STOP_ACK")) phase="STOP_STATUS";
        else if(phase.equals("CLEAR_ACK")) phase="IDLE_STATUS";
        deadline=now+5000;
    }
    boolean done() { return phase.equals("DONE"); }
    boolean failed() { return phase.equals("FAILED"); }
}
