package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import android.view.MotionEvent;
import android.widget.*;

/** Explicit physical test: one 1-degree tilt step and return, no camera command. */
final class ManualHardwareChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private int checks;
    private boolean moveAttempted;
    private boolean uiMode,panPlanMode;
    private MainActivity activity;
    private long downTime;
    int run(Instrumentation instrumentation) throws Exception {
        return run(instrumentation,false);
    }
    int runJog(Instrumentation instrumentation) throws Exception { return run(instrumentation,true); }
    int runPanPlan(Instrumentation instrumentation) throws Exception { panPlanMode=true; return run(instrumentation,false); }
    int runUiJog(Instrumentation instrumentation) throws Exception { uiMode=true; return run(instrumentation,true); }
    private int run(Instrumentation instrumentation,boolean jogMode) throws Exception {
        runner=instrumentation;
        Context context=runner.getTargetContext();
        java.util.Map<String,?> preferencesBefore=(uiMode || panPlanMode)?new java.util.HashMap<>(context.getSharedPreferences("settings",0).getAll()):null;
        java.util.Map<String,String> originalDraft=new java.util.HashMap<>();
        String address=context.getSharedPreferences("settings",0).getString("device",null);
        if(address==null) throw new AssertionError("No saved controller");
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName name,IBinder binder) { service=((PanoramaService.LocalBinder)binder).service(); }
            public void onServiceDisconnected(ComponentName name) { service=null; }
        };
        main(() -> check(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind hardware service"));
        try {
            await(() -> service!=null,5000); main(() -> originalDraft.putAll(service.draftEndpoints));
            main(() -> {
                check(!service.hasConnection(),"No pre-existing user connection");
                context.startForegroundService(new Intent(context,PanoramaService.class).putExtra("address",address));
            });
            await(() -> service.fresh() && service.state.manualStatusSupported && service.state.manualStatusFresh(SystemClock.elapsedRealtime()),20000);
            main(() -> {
                check(service.state.firmware.equals("2.4.61"),"Expected firmware installed");
                check(service.state.runId==0 && service.state.state.equals("IDLE") && service.state.positionSafe(),"Idle without job inside confirmed limits");
                if(service.incident.needsAcknowledgement()) {
                    check(service.incident.key.endsWith("|DEVICE_RESTART"),"Only expected upload restart may be acknowledged automatically");
                    service.acknowledgeIncident(service.incident.id);
                }
            });
            await(() -> service.navigationAllowed() && !service.busy,10000);
            final int pan=service.state.pan,tilt=service.state.tilt;
            final int nextTilt=tilt+1000<=service.state.tiltMax?tilt+1000:tilt-1000;
            main(() -> check(nextTilt>=service.state.tiltMin && nextTilt<=service.state.tiltMax,"Bounded one-degree target"));
            if(jogMode) {
                if(uiMode) {
                    activity=(MainActivity)runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    main(() -> { activity.setShowWhenLocked(true); activity.setTurnScreenOn(true); activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); });
                    await(() -> activityService()==service && activity.hasWindowFocus(),5000);
                    main(() -> {
                        try { java.lang.reflect.Method select=MainActivity.class.getDeclaredMethod("selectPage",int.class); select.setAccessible(true); select.invoke(activity,2); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    });
                }
                main(() -> check(service.state.jogSupported && service.jogAllowed(),"Firmware supports current hold controls"));
                jogStep(0,-1); if(uiMode) rememberPosition("start");
                jogStep(0,1); jogStep(1,-1); jogStep(1,1); if(uiMode) rememberPosition("end");
                main(() -> { service.beginJog(1,-1); service.endJog(); });
                await(() -> !service.busy && !service.manualMoving,5000);
                main(() -> check(service.jogAllowed() && !service.jog.held,"Immediate release cannot leave a queued start"));
            } else if(panPlanMode) {
                int nextPan=pan+20000<=service.state.panMax?pan+20000:pan-20000;
                main(() -> {
                    check(nextPan>=service.state.panMin && nextPan<=service.state.panMax,"Pan-Plan test limited to 20 degrees inside limits");
                    service.setManualTempos(500,200);
                });
                move(nextPan,tilt); move(pan,tilt);
            } else { move(pan,nextTilt); move(pan,tilt); }
            main(() -> check(!service.manualMoving && !service.uncertain && service.navigationAllowed(),"Both physical completions release controls"));
        } finally {
            try {
                if(service!=null && service.connected && moveAttempted) {
                    main(() -> service.emergencyStop());
                    await(() -> !service.busy && !service.uncertain && service.fresh() && service.state.state.equals("STOPPED"),5000);
                    main(() -> {
                        check(service.state.runId==0,"Stop retains no unrelated job");
                        if(!service.incident.needsAcknowledgement()) service.command("CLEAR_RUN");
                    });
                    if(!service.incident.needsAcknowledgement())
                        await(() -> !service.busy && service.fresh() && service.state.state.equals("IDLE"),5000);
                }
            } finally {
                if(activity!=null) { main(activity::finish); runner.waitForIdleSync(); }
                main(() -> { if(service!=null) service.disconnect(); context.unbindService(binding); });
                main(() -> { if(service!=null) { service.draftEndpoints.clear(); service.draftEndpoints.putAll(originalDraft); } });
                if(preferencesBefore!=null) JogUiChecks.restoreSettings(context.getSharedPreferences("settings",0),preferencesBefore);
            }
        }
        return checks;
    }
    private void jogStep(int axis,int direction) throws Exception {
        final int start=axis==0?service.state.pan:service.state.tilt;
        main(() -> { moveAttempted=true; service.support.append(uiMode?"TEST_MODE REAL_PLAN_TOUCH_JOG_NO_SHUTTER":"TEST_MODE REAL_HOLD_JOG_NO_SHUTTER");
            if(uiMode) touch(axis,direction,MotionEvent.ACTION_DOWN); else service.beginJog(axis,direction); });
        await(() -> !service.busy && (service.jog.sequence>0 || service.state.state.equals("FAULT")),5000);
        main(() -> check(service.jog.held && !service.uncertain && !service.state.state.equals("FAULT"),"Hold start ACK accepted"));
        try {
            for(int i=0;i<16;i++) {
                Thread.sleep(100);
                java.util.concurrent.atomic.AtomicBoolean bound=new java.util.concurrent.atomic.AtomicBoolean();
                main(() -> bound.set(Math.abs((axis==0?service.state.pan:service.state.tilt)-start)>=1500 || service.state.state.equals("FAULT")));
                if(bound.get()) break;
            }
        } finally { main(() -> { if(uiMode) touch(axis,direction,MotionEvent.ACTION_UP); else service.endJog(); }); }
        await(() -> !service.busy && !service.manualMoving && !service.jog.stopping,5000);
        Thread.sleep(1200);
        main(() -> {
            check(!service.state.state.equals("FAULT") && service.jogAllowed(),"Release returns usable controls without fault");
            check(((axis==0?service.state.pan:service.state.tilt)-start)*direction>=200,"Real hold generates directed encoder progress");
            check(service.state.manualStatus.kind.equals("JOG") && service.state.manualStatus.result.equals("CANCELLED"),"Firmware confirms ordinary release");
            Bundle result=new Bundle(); result.putString("stream","\nJog axis "+axis+", direction "+direction+", start "+start+", stop "+(axis==0?service.state.pan:service.state.tilt)+" mdeg; controls released.\n"); runner.sendStatus(0,result);
        });
    }
    @SuppressWarnings("unchecked")
    private void rememberPosition(String prefix) throws Exception {
        await(() -> service.endpointCaptureAllowed(),8000);
        main(() -> {
            try {
                java.lang.reflect.Field gatesField=MainActivity.class.getDeclaredField("gates"); gatesField.setAccessible(true);
                java.util.Map<Button,?> gates=(java.util.Map<Button,?>)gatesField.get(activity);
                Button button=null; for(Button candidate:gates.keySet()) if(candidate.getText().toString().equals(prefix.equals("start")?"Start merken":"Ende merken")) button=candidate;
                check(button!=null && button.isEnabled(),"Endpoint capture available after physical arrow release"); button.performClick();
                java.lang.reflect.Field inputsField=MainActivity.class.getDeclaredField("inputs"); inputsField.setAccessible(true);
                java.util.Map<String,EditText> inputs=(java.util.Map<String,EditText>)inputsField.get(activity);
                check(Math.abs(Double.parseDouble(inputs.get(prefix+"Pan").getText().toString())+service.state.pan/1000.0)<0.001 &&
                      Math.abs(Double.parseDouble(inputs.get(prefix+"Tilt").getText().toString())-service.state.tilt/1000.0)<0.001,"Remember button saves actual measured coordinate point");
                if(prefix.equals("end")) {
                    android.view.View root=activity.getWindow().getDecorView();
                    android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                    root.draw(new android.graphics.Canvas(bitmap));
                    try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(runner.getTargetContext().getCacheDir(),"endpoint-plan.png"))) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output); }
                    bitmap.recycle();
                }
            } catch(Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private PanoramaService activityService() {
        try { java.lang.reflect.Field field=MainActivity.class.getDeclaredField("service"); field.setAccessible(true); return (PanoramaService)field.get(activity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private void touch(int axis,int direction,int action) {
        try {
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("navigationBar"); field.setAccessible(true);
            LinearLayout navigation=(LinearLayout)field.get(activity);
            Button arrow=(Button)navigation.getChildAt(axis==0?(direction>0?2:1):(direction>0?3:4));
            long now=SystemClock.uptimeMillis(); if(action==MotionEvent.ACTION_DOWN) downTime=now;
            MotionEvent event=MotionEvent.obtain(downTime,now,action,arrow.getWidth()/2f,arrow.getHeight()/2f,0);
            try { arrow.dispatchTouchEvent(event); } finally { event.recycle(); }
        } catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private void move(int pan,int tilt) throws Exception {
        main(() -> {
            moveAttempted=true;
            service.support.append((panPlanMode?"TEST_MODE REAL_PLAN_PAN_20DEG_NO_CAMERA target=":"TEST_MODE REAL_MANUAL_STATUS_1DEG_NO_CAMERA target=")+pan+","+tilt);
            service.moveToUi(-pan/1000.0,tilt/1000.0);
        });
        await(() -> !service.busy && (service.manualMoving || service.message.equals("Manuelles Ziel erreicht") || service.state.state.equals("FAULT") || service.incident.needsAcknowledgement()),5000);
        main(() -> check((service.manualMoving || service.message.equals("Manuelles Ziel erreicht")) && !service.uncertain,"Move ACK accepted and matched"));
        await(() -> !service.manualMoving,15000);
        main(() -> {
            check(service.state.manualStatus!=null && service.state.manualStatus.result.equals("REACHED") &&
                service.state.manualStatus.pan==pan && service.state.manualStatus.tilt==tilt,"Physical firmware completion reaches requested target");
            check(service.navigationAllowed() && !service.uncertain,"Controls released by status without debug query");
            Bundle result=new Bundle();
            result.putString("stream","\nZiel Pan/Tilt "+pan+"/"+tilt+" mdeg, Ist "+service.state.pan+"/"+service.state.tilt+" mdeg, Befehl "+service.state.manualStatus.commandSequence+
                ", Firmware-Dauer "+service.state.manualStatus.durationMs+" ms, Bedienung freigegeben.\n");
            runner.sendStatus(0,result);
        });
    }
    private void main(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier ready,int timeout) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<deadline) {
            java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> result.set(ready.getAsBoolean())); if(result.get()) return; Thread.sleep(100);
        }
        throw new AssertionError("Physical check timed out: "+(service==null?"service missing":service.message));
    }
    private void check(boolean value,String text) { checks++; if(!value) throw new AssertionError(text); }
}
