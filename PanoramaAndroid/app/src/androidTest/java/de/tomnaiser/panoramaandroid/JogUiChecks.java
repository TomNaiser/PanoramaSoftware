package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Actual Plan-page touch events and status renders, with no hardware transport. */
final class JogUiChecks {
    private Instrumentation runner;
    private MainActivity activity;
    private PanoramaService service;
    private int checks;
    private long downTime;
    private String phase="bind";
    private FakeLink link;
    int run(Instrumentation instrumentation) throws Exception {
        runner=instrumentation; Context context=runner.getTargetContext();
        SharedPreferences preferences=context.getSharedPreferences("settings",0);
        Map<String,?> originalSettings=new HashMap<>(preferences.getAll());
        IncidentTracker savedIncident=new IncidentTracker(); DeviceState originalState=null;
        Map<String,String> originalDraft=new HashMap<>();
        FakeLink fake=new FakeLink();
        link=fake;
        try {
            activity=(MainActivity)runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            // Exercise this app's views while the phone remains locked; no
            // credentials, real transport or other app content are accessed.
            main(() -> { activity.setShowWhenLocked(true); activity.setTurnScreenOn(true);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); });
            await(() -> read(activity,"service")!=null);
            service=(PanoramaService)read(activity,"service"); originalState=service.state; originalDraft.putAll(service.draftEndpoints);
            main(() -> {
                check(!service.hasConnection(),"UI test has no hardware connection");
                copyIncident(service.incident,savedIncident);
                write(service,"session",fake); service.connected=true; service.state=new DeviceState();
                service.incident.active=false; service.incident.acknowledged=true;
                service.state.compatible=service.state.confirmed=service.state.limitsKnown=service.state.profiledMotion=true;
                service.state.state="IDLE"; service.state.panMin=-180000; service.state.panMax=180000;
                service.state.tiltMin=-45000; service.state.tiltMax=30000;
                service.state.accept(new P2Codec.Frame(1,"MANUAL_STATUS_CAPABILITIES",new String[]{"1"}),SystemClock.elapsedRealtime());
                service.state.accept(new P2Codec.Frame(2,"MANUAL_JOG_CAPABILITIES",new String[]{"1"}),SystemClock.elapsedRealtime());
                service.state.accept(new P2Codec.Frame(3,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{"5000"}),SystemClock.elapsedRealtime());
                service.state.accept(new P2Codec.Frame(4,"MANUAL_JOG_PAN_VELOCITY_CAPABILITIES",new String[]{"10000"}),SystemClock.elapsedRealtime());
                status("IDLE"); invoke(activity,"selectPage",new Class[]{int.class},2);
            });
            runner.waitForIdleSync();
            LinearLayout navigation=(LinearLayout)read(activity,"navigationBar");
            Button arrow=(Button)navigation.getChildAt(4);
            TextView hint=(TextView)read(activity,"alignmentHint");
            main(() -> {
                P2Codec.Frame position=new P2Codec.Frame(10,"STATUS",new String[]{"0","IDLE","NONE","0","0","0","0","0","0","120000","5000","0","1","NONE","0","0","0","0","0","0"});
                service.state.accept(position,SystemClock.elapsedRealtime()); service.plot.status(position,SystemClock.elapsedRealtime());
                PlanPreview preview=(PlanPreview)read(activity,"preview");
                preview.setLive(service.state,service.plot,true);
                ((PlotViewport)read(preview,"viewport")).fit(-2,2,-2,2);
            });
            main(() -> { check(arrow.isEnabled(),"Plan-page down arrow enabled"); touch(arrow,MotionEvent.ACTION_DOWN); });
            phase="first start";
            await(() -> service.jog.held && service.jog.sequence>0 && !service.busy);
            main(() -> {
                PlanPreview preview=(PlanPreview)read(activity,"preview");
                PlotViewport viewport=(PlotViewport)read(preview,"viewport");
                check(viewport.x(-service.state.pan/1000.0)>=0.1 && viewport.x(-service.state.pan/1000.0)<=0.9,
                    "Arrow brings the current physical position into the visible coordinate system");
            });
            Thread.sleep(350); runner.waitForIdleSync();
            main(() -> { PlanPreview preview=(PlanPreview)read(activity,"preview"); android.graphics.Rect visible=new android.graphics.Rect();
                check(preview.getGlobalVisibleRect(visible) && visible.height()>=preview.getHeight(),"Arrow hold reveals the complete position diagram above the fixed controls"); });
            final String[] heldHint={null}; main(() -> heldHint[0]=hint.getText().toString());
            final int[] iPosition={0};
            for(int i=0;i<20;i++) {
                iPosition[0]=i;
                Thread.sleep(100);
                main(() -> {
                    status("ACTIVE");
                    P2Codec.Frame measured=new P2Codec.Frame(10,"STATUS",new String[]{"0","IDLE","NONE","0","0","0","0","0","0","120000",""+(5000-iPosition[0]*250),"0","1","NONE","0","0","0","0","0","0"});
                    service.state.accept(measured,SystemClock.elapsedRealtime()); service.plot.status(measured,SystemClock.elapsedRealtime());
                    invoke(activity,"render",new Class[0]);
                    PlanPreview preview=(PlanPreview)read(activity,"preview"); PlotViewport viewport=(PlotViewport)read(preview,"viewport");
                    check(viewport.y(service.state.tilt/1000.0)>=0.1 && viewport.y(service.state.tilt/1000.0)<=0.9,"Measured point stays visible while held arrows move the camera");
                    check(service.jog.held && arrow.isEnabled() && arrow.isPressed(),"Status update preserves pressed Plan arrow and gesture");
                    check(hint.getText().toString().equals(heldHint[0]),"Held hint does not flicker with keepalive ACK state");
                    if(fake.keepalive!=null && !fake.keepalive.isDone()) fake.keepalive.complete(new V2Client.Result(true,"JOG_ALIVE",fake.sequence));
                    touch(arrow,MotionEvent.ACTION_MOVE);
                });
            }
            main(() -> {
                check(fake.starts==1 && fake.stops==0 && fake.keeps>=3,"One sustained finger hold sends one start and renewals, no stop or steps");
                touch(arrow,MotionEvent.ACTION_UP);
            });
            await(() -> !service.jog.held && !service.manualMoving && !service.busy);
            main(() -> check(!service.endpointCaptureAllowed(),"A release ACK cannot capture a possibly coasting position"));
            await(() -> { status("CANCELLED"); invoke(activity,"render",new Class[0]); return service.endpointCaptureAllowed(); });
            main(() -> {
                check(fake.stops==1 && !arrow.isPressed(),"Finger release sends one priority stop");
                status("CANCELLED"); invoke(activity,"render",new Class[0]);
                check(arrow.isEnabled(),"Release leaves the next Plan gesture usable");
                @SuppressWarnings("unchecked") Map<String,EditText> inputs=(Map<String,EditText>)read(activity,"inputs");
                @SuppressWarnings("unchecked") Map<Button,?> gates=(Map<Button,?>)read(activity,"gates");
                Button remember=null; for(Button candidate:gates.keySet()) if(candidate.getText().toString().equals("Start merken")) remember=candidate;
                android.graphics.Rect visible=new android.graphics.Rect();
                check(remember!=null && remember.getGlobalVisibleRect(visible) && visible.height()>=remember.getHeight(),"Remember Start stays fully visible beside the fixed arrow controls");
                invoke(activity,"takePosition",new Class[]{String.class},"start");
                check(Double.parseDouble(inputs.get("startPan").getText().toString())==-120 && Double.parseDouble(inputs.get("startTilt").getText().toString())==service.state.tilt/1000.0,"Start stores the displayed actual position after arrow release");
                P2Codec.Frame moved=new P2Codec.Frame(11,"STATUS",new String[]{"0","IDLE","NONE","0","0","0","0","0","0","121250","-8000","0","1","NONE","0","0","0","0","0","0"});
                service.state.accept(moved,SystemClock.elapsedRealtime()); service.plot.status(moved,SystemClock.elapsedRealtime()); invoke(activity,"render",new Class[0]);
                invoke(activity,"takePosition",new Class[]{String.class},"end");
                check(Double.parseDouble(inputs.get("endPan").getText().toString())==-121.25 && Double.parseDouble(inputs.get("endTilt").getText().toString())==-8 && Double.parseDouble(inputs.get("startPan").getText().toString())==-120,"End stores the next measured position without changing Start");
                touch(arrow,MotionEvent.ACTION_DOWN);
            });
            phase="second start";
            await(() -> service.jog.held && !service.busy);
            main(() -> activity.onWindowFocusChanged(false));
            phase="focus release";
            await(() -> !service.jog.held && !service.busy);
            main(() -> check(fake.stops==2,"Window focus loss releases held gesture"));
        } finally {
            DeviceState restore=originalState;
            if(service!=null && restore!=null) main(() -> {
                service.disconnect(); service.state=restore; copyIncident(savedIncident,service.incident);
                invoke(service,"saveIncident",new Class[0]);
            });
            if(activity!=null) { main(activity::finish); runner.waitForIdleSync(); }
            if(service!=null) main(() -> { service.draftEndpoints.clear(); service.draftEndpoints.putAll(originalDraft); });
            restoreSettings(preferences,originalSettings);
        }
        return checks;
    }
    @SuppressWarnings("unchecked")
    static void restoreSettings(SharedPreferences preferences,Map<String,?> originalSettings) {
            SharedPreferences.Editor editor=preferences.edit().clear();
            for(Map.Entry<String,?> entry:originalSettings.entrySet()) {
                Object value=entry.getValue(); String key=entry.getKey();
                if(value instanceof String) editor.putString(key,(String)value);
                else if(value instanceof Boolean) editor.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer) editor.putInt(key,(Integer)value);
                else if(value instanceof Long) editor.putLong(key,(Long)value);
                else if(value instanceof Float) editor.putFloat(key,(Float)value);
                else if(value instanceof Set) editor.putStringSet(key,(Set<String>)value);
            }
            editor.commit();
    }
    private void status(String result) {
        long now=SystemClock.elapsedRealtime(); service.state.lastStatusMs=now;
        int sequence=result.equals("IDLE")?0:service.jog.sequence>0?service.jog.sequence:link.startSequence;
        service.state.accept(new P2Codec.Frame(3,"MANUAL_STATUS",new String[]{""+sequence,
            sequence==0?"NONE":"JOG",result,"0","0","0",sequence==0?"0":"750"}),now);
    }
    private void touch(Button button,int action) {
        long now=SystemClock.uptimeMillis(); if(action==MotionEvent.ACTION_DOWN) downTime=now;
        MotionEvent event=MotionEvent.obtain(downTime,now,action,button.getWidth()/2f,button.getHeight()/2f,0);
        try { button.dispatchTouchEvent(event); } finally { event.recycle(); }
    }
    private void main(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        for(int i=0;i<60;i++) { java.util.concurrent.atomic.AtomicBoolean ok=new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> ok.set(condition.getAsBoolean())); if(ok.get()) return; Thread.sleep(100); }
        throw new AssertionError("UI hold condition timed out: "+phase+" held="+(service!=null && service.jog.held)+" busy="+(service!=null && service.busy)+" message="+(service==null?"unbound":service.message));
    }
    private void check(boolean value,String text) { checks++; if(!value) throw new AssertionError(text); }
    private static Object read(Object owner,String name) { try { Field f=owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner); } catch(Exception ex) { throw new RuntimeException(ex); } }
    private static void write(Object owner,String name,Object value) { try { Field f=owner.getClass().getDeclaredField(name); f.setAccessible(true); f.set(owner,value); } catch(Exception ex) { throw new RuntimeException(ex); } }
    private static void invoke(Object owner,String name,Class[] types,Object... args) { try { Method m=owner.getClass().getDeclaredMethod(name,types); m.setAccessible(true); m.invoke(owner,args); } catch(Exception ex) { throw new RuntimeException(ex); } }
    private static void copyIncident(IncidentTracker from,IncidentTracker to) { to.id=from.id; to.key=from.key; to.detail=from.detail; to.active=from.active; to.acknowledged=from.acknowledged; to.requiresStop=from.requiresStop; }
    private static final class FakeLink implements DeviceLink {
        int sequence=1000,startSequence,starts,stops,keeps; CompletableFuture<V2Client.Result> keepalive;
        public CompletableFuture<V2Client.Result> send(String type,String[] fields) {
            ++sequence;
            if(type.equals("MANUAL_JOG_START") || type.equals("MANUAL_JOG_START_VELOCITY")) { starts++; startSequence=sequence; return CompletableFuture.completedFuture(new V2Client.Result(true,"JOGGING",sequence)); }
            if(type.equals("MANUAL_JOG_KEEPALIVE")) { keeps++; keepalive=new CompletableFuture<>(); return keepalive; }
            return CompletableFuture.completedFuture(new V2Client.Result(true,"OK",sequence));
        }
        public CompletableFuture<V2Client.Result> stopJog(String token) { stops++; return CompletableFuture.completedFuture(new V2Client.Result(true,"JOG_STOPPED",++sequence)); }
        public CompletableFuture<V2Client.Result> emergencyStop(String run) { return CompletableFuture.completedFuture(new V2Client.Result(true,"STOPPED",++sequence)); }
        public boolean uncertain() { return false; } public void close() {} public void fail(String message) { throw new AssertionError(message); }
    }
}
