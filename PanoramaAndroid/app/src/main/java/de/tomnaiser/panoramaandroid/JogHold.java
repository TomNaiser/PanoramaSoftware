package de.tomnaiser.panoramaandroid;

/** One current finger gesture, never a queue of destinations. */
final class JogHold {
    boolean held, stopping;
    int axis, direction, sequence;
    long token;
    boolean begin(int axis,int direction,long now) {
        if(held || stopping) return false;
        if((axis!=0 && axis!=1) || (direction!=1 && direction!=-1)) throw new IllegalArgumentException(UiText.MOVEMENT_DIRECTION.text());
        token=Math.max(token+1,now&0x7fffffffL);
        this.axis=axis; this.direction=direction; held=true; sequence=0; return true;
    }
    long release() {
        if(!held) return 0;
        held=false; stopping=true; return token;
    }
    void finish(long expected) { if(token==expected) { held=stopping=false; sequence=0; } }
    boolean matches(int axis,int direction) { return held && this.axis==axis && this.direction==direction; }
}
