package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit hardware check: only HELLO/status/configuration; never a motion or camera job. */
final class CaptureStabilityHardwareChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private int checks;
    private void check(boolean ok,String text) { checks++;if(!ok) throw new AssertionError(text); }
    private void main(Runnable action) {
        AtomicReference<Throwable> error=new AtomicReference<>();
        runner.runOnMainSync(() -> {try {action.run();} catch(Throwable ex) {error.set(ex);} });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        for(int i=0;i<200;i++) {
            java.util.concurrent.atomic.AtomicBoolean done=new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> done.set(condition.getAsBoolean()));if(done.get()) return;Thread.sleep(100);
        }
        throw new AssertionError("Hardware settings timeout: "+(service==null?"no service":service.message));
    }
    int run(Instrumentation instrumentation,String expectedFirmware) throws Exception {
        runner=instrumentation;Context context=runner.getTargetContext();
        check(context.getPackageName().endsWith(".validation"),"Isolated application required");
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName name,IBinder binder) {service=((PanoramaService.LocalBinder)binder).service();}
            public void onServiceDisconnected(ComponentName name) {service=null;}
        };
        main(() -> check(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind real service"));
        CaptureStabilitySettings[] original={null};boolean[] restore={false};
        try {
            await(() -> service!=null);
            main(() -> {
                check(!service.hasConnection(),"No pre-existing connection");
                context.startForegroundService(new Intent(context,PanoramaService.class).putExtra("address","00:06:66:EE:18:E4"));
            });
            await(() -> service.fresh() && service.state.captureStability!=null && service.state.manualStatusFresh(SystemClock.elapsedRealtime()));
            main(() -> {
                check(service.state.firmware.equals(expectedFirmware),"Expected normal firmware");
                check(service.state.runId==0 && service.state.state.equals("IDLE") && service.state.positionSafe(),"Idle head, no loaded user job");
                if(service.incident.needsAcknowledgement()) {
                    check(service.incident.key.endsWith("|DEVICE_RESTART"),"Only expected firmware restart acknowledgement");
                    service.acknowledgeIncident(service.incident.id);
                }
            });
            await(() -> service.canConfigureStability());
            main(() -> {
                original[0]=service.state.captureStability;
                check(service.state.recoveryPauseSupported,"Recoverable pause capability advertised");
                restore[0]=true;
                CaptureStabilitySettings s=original[0];int wait=s.extraWaitMs==120000?s.extraWaitMs-1000:s.extraWaitMs+1000;
                service.saveCaptureStability(s.spanMdeg/1000.0,s.panDriftMdeg/1000.0,s.tiltDriftMdeg/1000.0,wait/1000.0);
            });
            await(() -> !service.busy && service.state.captureStability.revision==original[0].revision+1);
            main(() -> {
                CaptureStabilitySettings s=original[0],actual=service.state.captureStability;
                check(actual.spanMdeg==s.spanMdeg && actual.panDriftMdeg==s.panDriftMdeg && actual.tiltDriftMdeg==s.tiltDriftMdeg,"Angles remain unchanged");
                check(actual.extraWaitMs!=s.extraWaitMs,"New wait confirmed by real Nano");
                check(service.message.contains("zurückgelesen"),"Save success includes readback");
                service.readCaptureStability();
            });
            await(() -> !service.busy);
            main(() -> check(service.state.captureStability.revision==original[0].revision+1,"Separate read returns stored revision"));
        } finally {
            try {
                if(restore[0] && service!=null && service.fresh()) {
                    await(() -> service.canConfigureStability());
                    main(() -> {
                        CaptureStabilitySettings s=original[0];
                        service.saveCaptureStability(s.spanMdeg/1000.0,s.panDriftMdeg/1000.0,s.tiltDriftMdeg/1000.0,s.extraWaitMs/1000.0);
                    });
                    await(() -> !service.busy && original[0].sameValues(service.state.captureStability));
                    main(() -> check(original[0].sameValues(service.state.captureStability),"Original settings restored and read back"));
                } else if(restore[0]) throw new AssertionError("Cannot confirm original settings restored; read device before use");
            } finally {
                main(() -> {if(service!=null) service.disconnect();context.unbindService(binding);});
            }
        }
        return checks;
    }
}
