package de.tomnaiser.panoramaandroid;

/** ACK plus fresh state confirmations for CLEAR -> LOAD. Never returns START or movement commands. */
final class RestPreparation {
    private String phase="START";
    private long baseline;
    private final long sourceRun,newRun;
    private final boolean clearedAlready;
    RestPreparation(long sourceRun,long newRun) { this(sourceRun,newRun,false); }
    RestPreparation(long sourceRun,long newRun,boolean clearedAlready) { this.sourceRun=sourceRun;this.newRun=newRun;this.clearedAlready=clearedAlready; }
    String next(String state,long run,long version,boolean fresh,boolean recipeMatches) {
        if(done() || failed() || !fresh) return null;
        if(phase.equals("START")) {
            if(clearedAlready && state.equals("IDLE") && run==0) { baseline=version;phase="LOAD_ACK";return "LOAD_PLAN"; }
            if(!state.equals("STOPPED") || run!=sourceRun) { phase="FAILED";return null; }
            baseline=version;phase="CLEAR_ACK";return "CLEAR_RUN";
        }
        if(version<=baseline) return null;
        if(phase.equals("IDLE_STATUS")) {
            if(state.equals("IDLE") && run==0) { baseline=version;phase="LOAD_ACK";return "LOAD_PLAN"; }
            if(run!=sourceRun || !state.equals("STOPPED")) phase="FAILED";
        } else if(phase.equals("PLAN_STATUS")) {
            if(state.equals("PLAN_READY") && run==newRun && recipeMatches) phase="DONE";
            else if(!state.equals("IDLE") || run!=0) phase="FAILED";
        }
        return null;
    }
    void acknowledged(boolean accepted) {
        if(done() || failed()) return;
        if(!accepted) { phase="FAILED";return; }
        if(phase.equals("CLEAR_ACK")) phase="IDLE_STATUS";
        else if(phase.equals("LOAD_ACK")) phase="PLAN_STATUS";
    }
    boolean done() { return phase.equals("DONE"); }
    boolean failed() { return phase.equals("FAILED"); }
}
