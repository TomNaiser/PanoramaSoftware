package de.tomnaiser.panoramaandroid;

/** One durable acknowledgement per incident; repeated status frames never create dialog stacks. */
final class IncidentTracker {
    long id;
    String key="", detail="";
    boolean acknowledged=true, active;
    boolean requiresStop=true;
    void observe(String nextKey,String nextDetail) {
        if(!active || !key.equals(nextKey)) { id++; key=nextKey; acknowledged=false; requiresStop=true; }
        active=true; detail=nextDetail;
    }
    boolean acknowledge(long expected) {
        if(expected!=id || acknowledged) return false;
        acknowledged=true; return true;
    }
    void resolved() { active=false; }
    boolean needsAcknowledgement() { return !acknowledged; }
}
