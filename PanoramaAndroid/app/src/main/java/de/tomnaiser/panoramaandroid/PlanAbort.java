package de.tomnaiser.panoramaandroid;

/** Stop acknowledgement and fresh stopped status must precede clearing a running plan. */
final class PlanAbort {
    private String stage="START";
    private final boolean stopFirst;
    PlanAbort(String state) {
        if(!allowed(state)) throw new IllegalArgumentException("Kein abbrechbarer Panoramaauftrag");
        stopFirst=!(state.equals("STOPPED") || state.equals("COMPLETED") || state.equals("FAULT"));
    }
    static boolean allowed(String state) {
        return state.equals("PLAN_READY") || state.equals("RUNNING") || state.equals("PAUSE_PENDING")
            || state.equals("PAUSED") || state.equals("STOPPED") || state.equals("COMPLETED") || state.equals("FAULT");
    }
    String next(String state,boolean fresh) {
        if(!fresh) return null;
        if(stage.equals("START")) { stage=stopFirst?"STOP_ACK":"CLEAR_ACK"; return stopFirst?"STOP":"CLEAR_RUN"; }
        if(stage.equals("STOP_STATUS") && state.equals("STOPPED")) { stage="CLEAR_ACK"; return "CLEAR_RUN"; }
        if(stage.equals("IDLE_STATUS") && state.equals("IDLE")) stage="DONE";
        return null;
    }
    void acknowledged(boolean accepted) {
        if(!accepted) { stage="FAILED"; return; }
        if(stage.equals("STOP_ACK")) stage="STOP_STATUS";
        else if(stage.equals("CLEAR_ACK")) stage="IDLE_STATUS";
    }
    boolean done() { return stage.equals("DONE"); }
    boolean failed() { return stage.equals("FAILED"); }
}
