package de.tomnaiser.panoramaandroid;

import java.util.*;

/** Bounded telemetry; a gap is never drawn as a measured movement. */
final class RunPlotModel {
    static final class Capture {
        final String key; final int targetPan,targetTilt,pan,tilt; final boolean reconstructed;
        Capture(String key,int tx,int ty,int x,int y,boolean inferred) {
            this.key=key; targetPan=tx; targetTilt=ty; pan=x; tilt=y; reconstructed=inferred;
        }
        static Capture parse(String key,String payload) {
            String[] f=payload.split("\\|",-1);
            boolean manual=f[0].equals("MANUAL_IMAGE_TRIGGERED");
            int x=Integer.parseInt(f[manual?5:10]), y=Integer.parseInt(f[manual?6:11]);
            return new Capture(key,manual?x:Integer.parseInt(f[8]),manual?y:Integer.parseInt(f[9]),x,y,f[manual?8:12].equals("1"));
        }
    }
    static final class Sample {
        final int pan,tilt; final boolean joined;
        Sample(int x,int y,boolean joined) { pan=x; tilt=y; this.joined=joined; }
    }
    long runId=-1, lastMs;
    int targetPan,targetTilt,pan,tilt;
    boolean positionKnown, joinNext;
    List<Capture> captures=Collections.emptyList();
    final ArrayDeque<Sample> trace=new ArrayDeque<>();
    void status(P2Codec.Frame frame,long now) {
        String[] f=frame.fields; long id=DeviceState.unsigned(f[0]);
        if(runId!=id) { runId=id; trace.clear(); captures=Collections.emptyList(); joinNext=false; }
        targetPan=Integer.parseInt(f[7]); targetTilt=Integer.parseInt(f[8]);
        pan=Integer.parseInt(f[9]); tilt=Integer.parseInt(f[10]); positionKnown=true;
        trace.addLast(new Sample(pan,tilt,joinNext && now-lastMs<2500));
        while(trace.size()>512) trace.removeFirst();
        lastMs=now; joinNext=true;
    }
    void disconnected() { joinNext=false; positionKnown=false; }
}
