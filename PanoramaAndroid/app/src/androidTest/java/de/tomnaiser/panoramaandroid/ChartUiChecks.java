package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** View-only checks: no motor, shutter, connection or run commands. */
final class ChartUiChecks {
    private Instrumentation runner;
    private MainActivity activity;
    private int checks;
    int run(Instrumentation instrumentation) throws Exception { return run(instrumentation,false); }
    int run(Instrumentation instrumentation,boolean landscape) throws Exception {
        runner=instrumentation; Context context=runner.getTargetContext();
        SharedPreferences preferences=context.getSharedPreferences("settings",0);
        Map<String,?> original=new HashMap<>(preferences.getAll());
        android.app.Application application=(android.app.Application)context.getApplicationContext();
        java.util.concurrent.atomic.AtomicReference<MainActivity> latest=new java.util.concurrent.atomic.AtomicReference<>();
        android.app.Application.ActivityLifecycleCallbacks rotations=new android.app.Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(android.app.Activity a,Bundle b) { if(a instanceof MainActivity) { latest.set((MainActivity)a); a.setShowWhenLocked(true); a.setTurnScreenOn(true); a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); } }
            public void onActivityStarted(android.app.Activity a) { }
            public void onActivityResumed(android.app.Activity a) { if(a instanceof MainActivity) latest.set((MainActivity)a); }
            public void onActivityPaused(android.app.Activity a) { }
            public void onActivityStopped(android.app.Activity a) { }
            public void onActivitySaveInstanceState(android.app.Activity a,Bundle b) { }
            public void onActivityDestroyed(android.app.Activity a) { }
        };
        main(() -> application.registerActivityLifecycleCallbacks(rotations));
        try {
            activity=(MainActivity)runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(() -> { activity.setShowWhenLocked(true); activity.setTurnScreenOn(true); activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); });
            if(landscape) {
                main(() -> activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                boolean ready=false;
                for(int attempt=0;attempt<50 && !ready;attempt++) {
                    Thread.sleep(100);
                    java.util.concurrent.atomic.AtomicBoolean measured=new java.util.concurrent.atomic.AtomicBoolean();
                    main(() -> {
                        MainActivity current=latest.get();
                        if(current!=null) activity=current;
                        View root=activity.getWindow().getDecorView();
                        measured.set(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE && root.getWidth()>root.getHeight() && activity.hasWindowFocus());
                    }); ready=measured.get();
                }
                if(!ready) throw new AssertionError("Real landscape window not available");
            }
            runner.waitForIdleSync();
            for(int page : new int[]{2,3}) {
                main(() -> invoke(activity,"selectPage",new Class[]{int.class},page)); runner.waitForIdleSync();
                PlanPreview plot=(PlanPreview)read(activity,page==2?"preview":"runPreview");
                LinearLayout[] pages=(LinearLayout[])read(activity,"pages"); ScrollView settings=(ScrollView)pages[page].getParent();
                main(() -> check(fullyVisible(plot),"Coordinate system immediately visible on page "+page+" without scrolling"));
                main(() -> settings.scrollTo(0,pages[page].getHeight())); runner.waitForIdleSync();
                main(() -> check(fullyVisible(plot),"Settings scrolling cannot move the coordinate system offscreen"));
                LinearLayout dock=(LinearLayout)plot.getParent();
                Button reset=(Button)dock.findViewWithTag("plotReset"); Button expand=(Button)dock.findViewWithTag("plotLarge");
                main(() -> {
                    check(reset!=null && fullyVisible(reset),"Pan standard reset always accessible");
                    plot.zoom(.25); plot.viewport.shift(80,5); reset.performClick();
                    check(plot.viewport.left==-180 && plot.viewport.right==180,"Pan reset restores exact 360-degree overview after zoom/drag");
                });
                long normalArea=(long)plot.getHeight()*plot.getWidth(); double[] bounds={plot.viewport.left,plot.viewport.right,plot.viewport.bottom,plot.viewport.top};
                main(expand::performClick); runner.waitForIdleSync();
                main(() -> {
                    check(settings.getVisibility()==View.GONE && (long)plot.getHeight()*plot.getWidth()>normalArea,"Large mode gives the settings area to the plot");
                    check(((View)read(activity,"headerBar")).getVisibility()==View.GONE,"Large mode removes header/menu chrome to maximize plotting area");
                    check(fullyVisible(plot) && fullyVisible(expand),"Large plot and return control remain fully visible");
                    check(plot.viewport.left==bounds[0] && plot.viewport.right==bounds[1] && plot.viewport.bottom==bounds[2] && plot.viewport.top==bounds[3],"Size toggle preserves coordinate range");
                    @SuppressWarnings("unchecked") Map<Button,?> gates=(Map<Button,?>)read(activity,"gates");
                    Button emergency=null; for(Button b:gates.keySet()) if(b.getText().toString().equals("NOT-STOP")) emergency=b;
                    check(emergency!=null && fullyVisible(emergency),"Emergency stop remains accessible in large mode");
                    activity.onBackPressed();
                }); runner.waitForIdleSync();
                main(() -> check(settings.getVisibility()==View.VISIBLE && fullyVisible(plot),"Back exits large mode while staying on the diagram page"));
                if(page==3) {
                    java.util.concurrent.atomic.AtomicBoolean selected=new java.util.concurrent.atomic.AtomicBoolean();
                    main(() -> {
                        RunPlotModel fixture=new RunPlotModel();fixture.captures=Collections.singletonList(new RunPlotModel.Capture("R-123-7",-1000,1000,-1500,1100,false,9,7));
                        plot.setLive(null,fixture,false);plot.viewport.fit(.5,2,0,2);
                        plot.setWarningListener(c -> selected.set(c.ordinal==7 && c.targetPan==-1000 && c.pan==-1500));
                        float density=plot.getResources().getDisplayMetrics().density;
                        float x=(float)(55*density+plot.viewport.x(1)*(plot.getWidth()-69*density));
                        float y=(float)(28*density+plot.viewport.y(1)*(plot.getHeight()-76*density));
                        long now=SystemClock.uptimeMillis();
                        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,x,y,0);
                        plot.dispatchTouchEvent(down);plot.dispatchTouchEvent(up);down.recycle();up.recycle();
                        check(selected.get(),"Warning marker selects its exact intended target without moving or triggering");
                        Object originalService=read(activity,"service");
                        PanoramaService fake=new PanoramaService();fake.connected=true;
                        fake.state.compatible=fake.state.confirmed=fake.state.limitsKnown=true;
                        fake.state.panMin=-180000;fake.state.panMax=180000;fake.state.tiltMin=-45000;fake.state.tiltMax=30000;
                        fake.state.lastStatusMs=SystemClock.elapsedRealtime();fake.state.state="RUNNING";
                        try {
                            Field field=MainActivity.class.getDeclaredField("service");field.setAccessible(true);field.set(activity,fake);
                            Method review=MainActivity.class.getDeclaredMethod("reviewCaptureWarning",RunPlotModel.Capture.class);review.setAccessible(true);
                            android.app.AlertDialog dialog=(android.app.AlertDialog)review.invoke(activity,fixture.captures.get(0));
                            check(!dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled(),"Warning inspection during panorama cannot start a manual move");dialog.dismiss();
                            fake.state.state="IDLE";
                            dialog=(android.app.AlertDialog)review.invoke(activity,fixture.captures.get(0));
                            check(dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled(),"Stopped panorama permits explicit revisiting of the intended target");dialog.dismiss();
                            fake.state.panMin=0;
                            dialog=(android.app.AlertDialog)review.invoke(activity,fixture.captures.get(0));
                            check(!dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled(),"Historical warning outside current limits cannot be driven");dialog.dismiss();
                        } catch(Exception ex) {throw new RuntimeException(ex);}
                        finally {
                            try {Field field=MainActivity.class.getDeclaredField("service");field.setAccessible(true);field.set(activity,originalService);}
                            catch(Exception ex) {throw new RuntimeException(ex);}
                        }
                        plot.invalidate();
                    });
                    runner.waitForIdleSync();screenshot(context,landscape?"warning-point-landscape.png":"warning-point.png");
                }
                screenshot(context,page==2?(landscape?"plan-normal-landscape.png":"plan-normal.png"):(landscape?"run-normal-landscape.png":"run-normal.png"));
                main(expand::performClick); runner.waitForIdleSync(); screenshot(context,page==2?(landscape?"plan-large-landscape.png":"plan-large.png"):(landscape?"run-large-landscape.png":"run-large.png"));
                main(expand::performClick); runner.waitForIdleSync();
            }
        } finally {
            main(() -> application.unregisterActivityLifecycleCallbacks(rotations));
            if(activity!=null) { main(activity::finish); runner.waitForIdleSync(); }
            JogUiChecks.restoreSettings(preferences,original);
        }
        return checks;
    }
    private void screenshot(Context context,String name) throws Exception {
        main(() -> {
            View root=activity.getWindow().getDecorView();
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
            root.draw(new android.graphics.Canvas(bitmap));
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(context.getCacheDir(),name))) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out); }
            catch(java.io.IOException ex) { throw new RuntimeException(ex); }
            bitmap.recycle();
        });
    }
    private boolean fullyVisible(View view) { Rect rect=new Rect(); return view.getHeight()>0 && view.getGlobalVisibleRect(rect) && rect.height()>=view.getHeight() && rect.width()>=view.getWidth(); }
    private void check(boolean condition,String message) { checks++; if(!condition) throw new AssertionError(message); }
    private void main(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private static Object read(Object object,String field) { try { Field f=object.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(object); } catch(Exception ex) { throw new RuntimeException(ex); } }
    private static void invoke(Object object,String method,Class[] types,Object... args) { try { Method m=object.getClass().getDeclaredMethod(method,types); m.setAccessible(true); m.invoke(object,args); } catch(Exception ex) { throw new RuntimeException(ex); } }
}
