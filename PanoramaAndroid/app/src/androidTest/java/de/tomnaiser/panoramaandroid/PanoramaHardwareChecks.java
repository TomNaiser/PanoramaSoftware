package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Explicit hardware/shutter opt-in: original 35-column user recipe. */
final class PanoramaHardwareChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private int checks;
    private boolean loaded;
    private long runId;

    int run(Instrumentation instrumentation,String expectedFirmware) throws Exception {
        runner=instrumentation;
        Context context=runner.getTargetContext();
        String address=context.getSharedPreferences("settings",0).getString("device",null);
        check(address!=null,"Saved controller required");
        runId=System.currentTimeMillis()/1000;
        PanoramaPlan plan=PanoramaPlan.parse(new String[]{""+runId,"1","0","38936","8020","35","8","2062","-3083","700","30","500","1000","1000","1","5000"});
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName name,IBinder binder) {service=((PanoramaService.LocalBinder)binder).service();}
            public void onServiceDisconnected(ComponentName name) {service=null;}
        };
        main(() -> check(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind real app service"));
        try {
            await(() -> service!=null,5000);
            main(() -> {
                check(!service.hasConnection(),"No pre-existing user connection");
                context.startForegroundService(new Intent(context,PanoramaService.class).putExtra("address",address));
            });
            await(() -> service.fresh() && service.state.manualStatusFresh(SystemClock.elapsedRealtime()),20000);
            main(() -> {
                check(service.state.firmware.equals(expectedFirmware),"Expected normal firmware");
                check(service.state.runId==0 && service.state.state.equals("IDLE") && service.state.positionSafe(),"Free controller inside stored limits");
                plan.validate(service.state);
                if(service.incident.needsAcknowledgement()) {
                    String key=service.incident.key;
                    check(key.endsWith("|DEVICE_RESTART") || key.endsWith("|MOTION_TARGET_MISSED"),"Only upload restart or archived target miss may be acknowledged");
                    service.acknowledgeIncident(service.incident.id);
                }
            });
            await(() -> service.navigationAllowed() && !service.busy,10000);
            // This fixed, independently validated recipe does not use or alter
            // user draft endpoints/reference-review state or saved settings.
            main(() -> {service.support.append("TEST_MODE REAL_MODEL_PANORAMA_35X8 run="+runId);loaded=true;service.command("LOAD_PLAN",plan.fields());});
            await(() -> !service.busy && service.state.state.equals("PLAN_READY"),10000);
            main(() -> service.command("GET_SUPPORT_BUNDLE"));
            await(() -> !service.busy && service.state.plan!=null && service.state.runId==runId,10000);
            main(() -> service.runCommand("START"));
            await(() -> !service.busy && service.state.state.equals("RUNNING"),10000);
            long deadline=SystemClock.elapsedRealtime()+25*60*1000;
            long[] progress={-1};boolean[] complete={false};
            while(SystemClock.elapsedRealtime()<deadline) {
                main(() -> {
                    check(service.fresh() && service.state.runId==runId,"Fresh original run identity");
                    check(!service.state.state.equals("FAULT") && !service.state.state.equals("STOPPED") && !service.incident.needsAcknowledgement(),"No device or app incident: "+service.message);
                    if(service.state.completed/8!=progress[0]) {
                        progress[0]=service.state.completed/8;
                        Bundle status=new Bundle();status.putString("stream","\nPanorama "+service.state.completed+"/280; "+service.state.state+"; run="+runId+"\n");runner.sendStatus(0,status);
                    }
                    complete[0]=service.state.state.equals("COMPLETED");
                });
                if(complete[0]) break;
                Thread.sleep(250);
            }
            main(() -> check(complete[0] && service.state.completed==280,"All 280 shutter completions through real app service"));
        } finally {
            try {
                if(loaded && service!=null && service.connected) {
                    main(() -> {check(service.state.runId==runId,"Cleanup owns only this test run");service.emergencyStop();});
                    await(() -> !service.busy && !service.uncertain && service.fresh() && service.state.state.equals("STOPPED"),10000);
                    main(() -> service.command("CLEAR_RUN"));
                    await(() -> !service.busy && service.fresh() && service.state.runId==0 && service.state.state.equals("IDLE"),10000);
                    main(() -> service.support.append("TEST_END REAL_MODEL_PANORAMA_35X8 stopped-and-cleared run="+runId));
                }
            } finally {main(() -> context.unbindService(binding));}
        }
        return checks;
    }
    private void main(Runnable action) {
        // Return assertions to the instrumentation worker so its finally block
        // can stop and clear this test's run even when a UI-thread check fails.
        AtomicReference<Throwable> failure=new AtomicReference<>();
        runner.runOnMainSync(() -> {try {action.run();} catch(Throwable ex) {failure.set(ex);}});
        Throwable ex=failure.get();
        if(ex instanceof Error) throw (Error)ex;
        if(ex instanceof RuntimeException) throw (RuntimeException)ex;
        if(ex!=null) throw new AssertionError(ex);
    }
    private void await(BooleanSupplier ready,int timeout) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<deadline) {
            AtomicBoolean result=new AtomicBoolean();main(() -> result.set(ready.getAsBoolean()));
            if(result.get()) return;Thread.sleep(100);
        }
        throw new AssertionError("Panorama check timed out: "+(service==null?"missing service":service.message));
    }
    private void check(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
}
