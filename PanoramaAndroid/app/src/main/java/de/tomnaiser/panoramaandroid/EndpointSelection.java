package de.tomnaiser.panoramaandroid;

/** A graphical proposal changes no endpoint and sends no movement until explicitly used. */
final class EndpointSelection {
    double baseTilt,pan,tilt;
    boolean pending,limited;
    void propose(double x,double y,double committedTilt,double minPan,double maxPan,double minTilt,double maxTilt) {
        baseTilt=committedTilt;
        pan=round(Math.max(minPan,Math.min(maxPan,x)));
        tilt=round(Math.max(minTilt,Math.min(maxTilt,y)));
        limited=Math.abs(pan-x)>.001 || Math.abs(tilt-y)>.001; pending=true;
    }
    private static double round(double value) { return Math.rint(value*1000)/1000; }
    void clear() { pending=false; limited=false; }
}
