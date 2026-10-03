package de.tomnaiser.panoramaandroid;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import java.util.Locale;

/** Shared projection for recipe, telemetry and touch; never sends device commands. */
final class PlanPreview extends View {
    interface EndpointListener { void changed(boolean start,double pan,double tilt); }
    private PanoramaPlan plan;
    private int[][] points=new int[0][];
    private DeviceState state;
    private RunPlotModel run;
    private boolean fresh;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    final PlotViewport viewport=new PlotViewport();
    private double sx,sy,ex,ey;
    private boolean startKnown,endKnown;
    private int editMode;
    private EndpointListener listener;
    private Runnable proposalChanged=() -> { };
    final EndpointSelection selection=new EndpointSelection();
    private float lastX,lastY;
    private boolean multiTouch;
    private final ScaleGestureDetector scale;
    PlanPreview(Context context) {
        super(context); setContentDescription("Pan-Tilt-Diagramm in Grad");
        scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) { zoom(1/d.getScaleFactor()); return true; }
        });
    }
    void setListener(EndpointListener value) { listener=value; }
    void setProposalListener(Runnable value) { proposalChanged=value; }
    void setEditMode(int mode) { editMode=mode; discardSelection(); }
    int editMode() { return editMode; }
    void discardSelection() { selection.clear(); proposalChanged.run(); invalidate(); }
    void commitSelection() {
        if(!selection.pending || editMode==0 || editMode==3) return;
        double x=selection.pan,y=selection.tilt;
        selection.clear(); listener.changed(editMode==1,x,y); proposalChanged.run(); invalidate();
    }
    void fineTilt(double delta) {
        if(editMode==0) return;
        double x=selection.pending?selection.pan:basePan();
        double y=(selection.pending?selection.tilt:baseTilt())+delta;
        propose(x,y);
    }
    void finePan(double delta) { if(editMode!=0) propose((selection.pending?selection.pan:basePan())+delta,selection.pending?selection.tilt:baseTilt()); }
    private double basePan() { return editMode==3 && run!=null && run.positionKnown?-run.pan/1000.0:editMode==1?sx:ex; }
    private double baseTilt() { return editMode==3 && run!=null && run.positionKnown?run.tilt/1000.0:editMode==1?sy:ey; }
    void setEndpoints(double a,double b,double c,double d) {
        if(sx!=a || sy!=b || ex!=c || ey!=d) selection.clear();
        sx=a; sy=b; ex=c; ey=d; startKnown=endKnown=true; invalidate();
    }
    void setEndpointAvailability(boolean start,boolean end) { startKnown=start; endKnown=end; invalidate(); }
    private void propose(double pan,double tilt) {
        selection.propose(pan,tilt,baseTilt(),
            state!=null && state.limitsKnown?-state.panMax/1000.0:-180,
            state!=null && state.limitsKnown?-state.panMin/1000.0:180,
            state!=null && state.limitsKnown?state.tiltMin/1000.0:-90,
            state!=null && state.limitsKnown?state.tiltMax/1000.0:90);
        proposalChanged.run(); invalidate();
    }
    void setPlan(PanoramaPlan value) {
        if(plan==value) return;
        plan=value; points=new int[value==null?0:value.total()][];
        for(int i=0;i<points.length;i++) points[i]=value.target(i);
        invalidate();
    }
    void setLive(DeviceState value,RunPlotModel model,boolean current) { state=value; run=model; fresh=current; invalidate(); }
    void zoom(double factor) { viewport.zoom(factor); invalidate(); }
    int alignmentHeight() {
        android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
        float dpi=Float.isFinite(metrics.ydpi) && metrics.ydpi>0?metrics.ydpi:metrics.densityDpi;
        // Reserve 25 mm for the actual plot, plus its axis labels and margins.
        return (int)Math.ceil(25*dpi/25.4+76*metrics.density);
    }
    void wholeRange() {
        viewport.left=state!=null && state.limitsKnown?-state.panMax/1000.0:-180;
        viewport.right=state!=null && state.limitsKnown?-state.panMin/1000.0:180;
        viewport.bottom=state!=null && state.limitsKnown?state.tiltMin/1000.0:-90;
        viewport.top=state!=null && state.limitsKnown?state.tiltMax/1000.0:90;
        invalidate();
    }
    void alignmentRange(double tiltExtent) {
        wholeRange();
        if(tiltExtent>0) {
            double bottom=Math.max(viewport.bottom,-tiltExtent),top=Math.min(viewport.top,tiltExtent);
            if(top>bottom) { viewport.bottom=bottom; viewport.top=top; }
        }
        discardSelection();
    }
    void fit() {
        double x0=Double.POSITIVE_INFINITY,x1=Double.NEGATIVE_INFINITY,y0=Double.POSITIVE_INFINITY,y1=Double.NEGATIVE_INFINITY;
        if(listener!=null && startKnown) { x0=x1=sx; y0=y1=sy; }
        if(listener!=null && endKnown) { x0=Math.min(x0,ex); x1=Math.max(x1,ex); y0=Math.min(y0,ey); y1=Math.max(y1,ey); }
        for(int[] p:points) {
            double x=-p[0]/1000.0,y=p[1]/1000.0;
            x0=Math.min(x0,x); x1=Math.max(x1,x); y0=Math.min(y0,y); y1=Math.max(y1,y);
        }
        if(run!=null && run.positionKnown) { x0=Math.min(x0,-run.pan/1000.0); x1=Math.max(x1,-run.pan/1000.0); y0=Math.min(y0,run.tilt/1000.0); y1=Math.max(y1,run.tilt/1000.0); }
        if(run!=null && listener==null) for(RunPlotModel.Capture capture:run.captures) {
            double x=-(capture.reconstructed?capture.targetPan:capture.pan)/1000.0,y=(capture.reconstructed?capture.targetTilt:capture.tilt)/1000.0;
            x0=Math.min(x0,x); x1=Math.max(x1,x); y0=Math.min(y0,y); y1=Math.max(y1,y);
        }
        if(Double.isFinite(x0)) viewport.fit(x0,x1,y0,y1); else viewport.fit(-180,180,-90,90);
        invalidate();
    }
    void save(Bundle b,String key) { b.putDoubleArray(key,new double[]{viewport.left,viewport.right,viewport.bottom,viewport.top,editMode}); }
    void restore(Bundle b,String key) {
        double[] v=b==null?null:b.getDoubleArray(key);
        if(v!=null && v.length==5) { viewport.left=v[0]; viewport.right=v[1]; viewport.bottom=v[2]; viewport.top=v[3]; editMode=(int)v[4]; }
    }
    private float density() { return getResources().getDisplayMetrics().density; }
    private float l() { return 55*density(); }
    private float t() { return 28*density(); }
    private float w() { return Math.max(1,getWidth()-l()-14*density()); }
    private float h() { return Math.max(1,getHeight()-t()-48*density()); }
    private float px(double x) { return (float)(l()+viewport.x(x)*w()); }
    private float py(double y) { return (float)(t()+viewport.y(y)*h()); }
    private void dot(Canvas c,int x,int y,int color,float radius) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); c.drawCircle(px(-x/1000.0),py(y/1000.0),radius*density(),paint); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(Color.rgb(244,247,249)); paint.setTextSize(11*density()); paint.setStrokeWidth(density());
        for(int i=0;i<=4;i++) {
            float x=l()+i*w()/4,y=t()+i*h()/4; paint.setColor(Color.LTGRAY);
            c.drawLine(x,t(),x,t()+h(),paint); c.drawLine(l(),y,l()+w(),y,paint); paint.setColor(Color.DKGRAY);
            String tick=String.format(Locale.GERMANY,"%.2f",viewport.pan(i/4.0));
            c.drawText(tick,Math.max(2*density(),Math.min(getWidth()-paint.measureText(tick)-8*density(),x-paint.measureText(tick)/2)),t()+h()+16*density(),paint);
            c.drawText(String.format(Locale.GERMANY,"%.2f",viewport.tilt(i/4.0)),2*density(),y+4*density(),paint);
        }
        c.drawText("Tilt ° ↑",4*density(),17*density(),paint); c.drawText("← Pan °",getWidth()-65*density(),getHeight()-8*density(),paint);
        c.save(); c.clipRect(l(),t(),l()+w(),t()+h());
        if(state!=null && state.limitsKnown) {
            paint.setColor(state.confirmed?Color.rgb(0,120,80):Color.rgb(210,130,0)); paint.setStyle(Paint.Style.STROKE);
            float x0=px(-state.panMax/1000.0),x1=px(-state.panMin/1000.0),y0=py(state.tiltMin/1000.0),y1=py(state.tiltMax/1000.0);
            c.drawRect(Math.min(x0,x1),Math.min(y0,y1),Math.max(x0,x1),Math.max(y0,y1),paint); paint.setStyle(Paint.Style.FILL);
        }
        for(int[] p:points) dot(c,p[0],p[1],Color.rgb(90,130,185),2.5f);
        if(run!=null) {
            if(listener==null) {
            RunPlotModel.Sample previous=null; paint.setColor(Color.GRAY);
            for(RunPlotModel.Sample sample:run.trace) {
                if(previous!=null && sample.joined) c.drawLine(px(-previous.pan/1000.0),py(previous.tilt/1000.0),px(-sample.pan/1000.0),py(sample.tilt/1000.0),paint);
                previous=sample;
            }
            for(RunPlotModel.Capture capture:run.captures) {
                if(capture.reconstructed) {
                    float x=px(-capture.targetPan/1000.0),y=py(capture.targetTilt/1000.0); paint.setColor(Color.rgb(210,130,0));
                    c.drawLine(x-4*density(),y-4*density(),x+4*density(),y+4*density(),paint); c.drawLine(x-4*density(),y+4*density(),x+4*density(),y-4*density(),paint);
                } else dot(c,capture.pan,capture.tilt,Color.rgb(0,130,75),4);
            }
            }
            if(run.positionKnown) { if(listener==null && plan!=null && fresh) dot(c,run.targetPan,run.targetTilt,Color.rgb(190,60,180),6); dot(c,run.pan,run.tilt,fresh?Color.rgb(220,70,25):Color.GRAY,5); }
        }
        if(listener!=null) {
            paint.setTextSize(12*density());
            if(startKnown) { paint.setColor(Color.rgb(40,70,160)); c.drawCircle(px(sx),py(sy),7*density(),paint); c.drawText("S",px(sx)+9*density(),py(sy),paint); }
            if(endKnown) { paint.setColor(Color.rgb(190,35,40)); c.drawCircle(px(ex),py(ey),7*density(),paint); c.drawText("E",px(ex)+9*density(),py(ey),paint); }
            if(selection.pending) {
                paint.setColor(Color.rgb(200,125,0)); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2*density());
                c.drawCircle(px(selection.pan),py(selection.tilt),10*density(),paint); paint.setStyle(Paint.Style.FILL); paint.setStrokeWidth(density());
            }
        }
        c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX(),y=e.getY(); int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            if(x<l() || x>l()+w() || y<t() || y>t()+h()) return false;
            getParent().requestDisallowInterceptTouchEvent(editMode==0); lastX=x; lastY=y; multiTouch=false;
        }
        if(e.getPointerCount()>1) { multiTouch=true; getParent().requestDisallowInterceptTouchEvent(true); }
        scale.onTouchEvent(e);
        if(!multiTouch && (action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP)) {
            if(listener!=null && editMode!=0) {
                if(action==MotionEvent.ACTION_UP) {
                    double pan=viewport.pan(Math.max(0,Math.min(1,(x-l())/w()))),tilt=viewport.tilt(Math.max(0,Math.min(1,(y-t())/h())));
                    propose(pan,tilt);
                }
            } else if(action==MotionEvent.ACTION_MOVE) { viewport.shift((x-lastX)/w()*(viewport.right-viewport.left),(y-lastY)/h()*(viewport.top-viewport.bottom)); invalidate(); }
            lastX=x; lastY=y;
        }
        if(action==MotionEvent.ACTION_CANCEL) discardSelection();
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) { getParent().requestDisallowInterceptTouchEvent(false); if(action==MotionEvent.ACTION_UP) performClick(); }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
