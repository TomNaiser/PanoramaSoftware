package de.tomnaiser.panoramaandroid;

/** Positive UI pan points left; positive UI tilt points up. */
final class PlotViewport {
    double left=-180, right=180, bottom=-90, top=90;
    double pan(double fraction) { return right-fraction*(right-left); }
    double tilt(double fraction) { return top-fraction*(top-bottom); }
    double x(double pan) { return (right-pan)/(right-left); }
    double y(double tilt) { return (top-tilt)/(top-bottom); }
    void fit(double minPan,double maxPan,double minTilt,double maxTilt) {
        double px=Math.max(.5,(maxPan-minPan)*.1), py=Math.max(.5,(maxTilt-minTilt)*.1);
        left=minPan-px; right=maxPan+px; bottom=minTilt-py; top=maxTilt+py;
    }
    void zoom(double factor) {
        double cx=(left+right)/2, cy=(bottom+top)/2;
        double w=Math.max(.02,Math.min(720,(right-left)*factor));
        double h=Math.max(.02,Math.min(360,(top-bottom)*factor));
        left=cx-w/2; right=cx+w/2; bottom=cy-h/2; top=cy+h/2;
    }
    void shift(double dx,double dy) { left+=dx; right+=dx; bottom+=dy; top+=dy; }
}
