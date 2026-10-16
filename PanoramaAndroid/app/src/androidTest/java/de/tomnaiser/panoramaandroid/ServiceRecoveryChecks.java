package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.os.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Real service and dialog with a fake wire: this runner cannot move or trigger hardware. */
final class ServiceRecoveryChecks {
    private int checks;
    private Instrumentation instrumentation;
    int run(Instrumentation runner,Activity activity) throws Exception {
        instrumentation=runner;
        PanoramaService service=(PanoramaService)field(MainActivity.class,"service").get(activity);
        if(service==null) {
            await(() -> { try { return field(MainActivity.class,"service").get(activity)!=null; } catch(Exception ex) { throw new RuntimeException(ex); } });
            service=(PanoramaService)field(MainActivity.class,"service").get(activity);
        }
        final PanoramaService target=service;
        check(!target.hasConnection(),"No real device connection in simulated checks");
        DeviceState original=target.state;
        long originalId=target.incident.id;
        String originalKey=target.incident.key, originalDetail=target.incident.detail;
        boolean originalActive=target.incident.active, originalAck=target.incident.acknowledged, originalStop=target.incident.requiresStop;
        FakeLink link=new FakeLink(target);
        try {
            main(() -> {
                try { field(PanoramaService.class,"session").set(target,link); }
                catch(Exception ex) { throw new RuntimeException(ex); }
                target.connected=true; target.state=state(0,"FAULT");
                target.incident.resolved(); target.incident.observe("SIMULATED_STALL","Simulierter Bewegungsfehler");
                target.support.append("TEST_MODE SIMULATED_RECOVERY_NO_HARDWARE"); render(activity);
                check(!target.navigationAllowed(),"Fault requires acknowledgement");
                AlertDialog dialog=dialog(activity); check(dialog!=null && dialog.isShowing(),"Acknowledgement dialog displayed");
                dialog.onBackPressed(); check(dialog.isShowing(),"Back cannot bypass acknowledgement");
                long id=target.incident.id;
                for(int i=0;i<100;i++) target.incident.observe("SIMULATED_STALL","Simulierter Bewegungsfehler");
                render(activity); check(dialog(activity)==dialog,"Repeated fault keeps one dialog");
                check(target.incident.id==id,"Stable incident identity");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            await(() -> !target.recovering && !target.uncertain && target.state.state.equals("IDLE"));
            main(() -> {
                check(target.navigationAllowed(),"Recovery permits new navigation");
                check(link.commands.equals(Arrays.asList("ESTOP","CLEAR_RUN")),"Empty run stopped and cleared, no movement");
                check(target.getSharedPreferences("incidents",0).getBoolean("acknowledged",false),"Acknowledgement persisted");
                check(dialog(activity)==null || !dialog(activity).isShowing(),"Dialog closes after acknowledgement");
                link.commands.clear(); target.state=state(123,"FAULT");
                target.state.completed=7; target.incident.resolved(); target.incident.observe("SIMULATED_RUN_STALL","Simulierter Auftragsfehler");
                target.acknowledgeIncident(target.incident.id);
            });
            await(() -> !target.recovering && !target.uncertain && target.state.state.equals("STOPPED"));
            main(() -> {
                check(target.state.runId==123 && target.state.completed==7,"Existing run and capture count retained");
                check(link.commands.equals(Collections.singletonList("ESTOP")),"Existing run never cleared");
                link.commands.clear(); target.state=state(123,"RUNNING"); target.state.completed=7;
                target.incident.observe("SIMULATED_CONNECTION","Verbindung wiederhergestellt"); target.incident.requiresStop=false;
                target.acknowledgeIncident(target.incident.id);
                check(!target.recovering && target.state.state.equals("RUNNING") && target.state.runId==123,"Connection-only resync preserves autonomous running job");
                check(link.commands.isEmpty(),"Connection-only resync sends neither stop nor start");
                link.commands.clear(); target.state=state(0,"IDLE");
                target.move(0,1); // preparation deliberately fails on the fake diagnostic ACK
            });
            await(() -> !target.busy);
            main(() -> {
                check(!target.uncertain && target.navigationAllowed(),"Missing diagnostic ACK does not poison readiness");
                check(link.commands.equals(Collections.singletonList("GET_DEBUG_LOG")),"Failed preparation sends no move");
                link.commands.clear(); link.sensorMissing=true; target.state=state(123,"FAULT");
                target.incident.observe("SIMULATED_SENSOR","Simulierter Sensorfehler");
                target.acknowledgeIncident(target.incident.id);
            });
            await(() -> !target.recovering);
            main(() -> {
                check(target.uncertain && !target.navigationAllowed(),"Persistent sensor fault cannot release motion");
                check(!target.incident.needsAcknowledgement(),"Persistent fault does not reopen acknowledged dialog");
                check(link.commands.equals(Collections.singletonList("ESTOP")),"Sensor fault never clears run or sends movement");
                link.sensorMissing=false;
                try { field(PanoramaService.class,"nextRecoveryAttemptMs").setLong(target,0); }
                catch(Exception ex) { throw new RuntimeException(ex); }
            });
            await(() -> !target.uncertain && !target.recovering);
            main(() -> {
                check(!target.incident.needsAcknowledgement() && target.navigationAllowed(),"Returning sensor restores readiness without another acknowledgement");
                check(link.commands.equals(Arrays.asList("ESTOP","ESTOP")),"Background recovery uses safe stops, no run clear or movement");
                link.commands.clear(); link.modernManual=true; target.state=state(0,"IDLE");
                target.state.accept(new P2Codec.Frame(1,"MANUAL_STATUS_CAPABILITIES",new String[]{"1"}),SystemClock.elapsedRealtime());
                target.state.accept(new P2Codec.Frame(2,"MANUAL_STATUS",new String[]{"0","NONE","IDLE","0","0","0","0"}),SystemClock.elapsedRealtime());
                target.moveToUi(0,1);
            });
            await(() -> !target.busy);
            main(() -> {
                check(link.commands.equals(Collections.singletonList("MANUAL_MOVE")),"Modern manual move never requests a diagnostic log");
                check(target.manualMoving,"Accepted move awaits matching firmware completion");
            });
        } finally {
            main(() -> {
                link.close(); target.disconnect(); target.state=original;
                target.incident.id=originalId; target.incident.key=originalKey; target.incident.detail=originalDetail;
                target.incident.active=originalActive; target.incident.acknowledged=originalAck;
                target.incident.requiresStop=originalStop;
                target.getSharedPreferences("incidents",0).edit().putLong("id",originalId).putString("key",originalKey)
                    .putString("detail",originalDetail).putBoolean("active",originalActive).putBoolean("acknowledged",originalAck).putBoolean("requiresStop",originalStop).commit();
                render(activity);
            });
        }
        return checks;
    }
    private static DeviceState state(long run,String name) {
        DeviceState s=new DeviceState(); s.state=name; s.runId=run; s.compatible=s.confirmed=s.limitsKnown=s.inactiveNavigation=true;
        s.panMin=-180000; s.panMax=180000; s.tiltMin=-45000; s.tiltMax=30000;
        s.lastStatusMs=SystemClock.elapsedRealtime(); return s;
    }
    private static Field field(Class<?> type,String name) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); return f; }
    private static AlertDialog dialog(Activity activity) {
        try { return (AlertDialog)field(MainActivity.class,"incidentDialog").get(activity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private static void render(Activity activity) {
        try { Method m=MainActivity.class.getDeclaredMethod("render"); m.setAccessible(true); m.invoke(activity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private void main(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        instrumentation.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier ready) throws Exception {
        for(int n=0;n<100;n++) {
            java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> result.set(ready.getAsBoolean())); if(result.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Simulated recovery did not finish within 10 seconds");
    }
    private void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    private static final class FakeLink implements DeviceLink {
        final List<String> commands=new ArrayList<>();
        final PanoramaService service;
        final Handler handler=new Handler(Looper.getMainLooper());
        boolean sensorMissing, closed, modernManual;
        FakeLink(PanoramaService service) { this.service=service; }
        public CompletableFuture<V2Client.Result> send(String type,String... fields) {
            commands.add(type);
            if(modernManual && type.equals("MANUAL_MOVE"))
                return CompletableFuture.completedFuture(new V2Client.Result(true,"MOVING",902));
            if(type.equals("GET_DEBUG_LOG")) {
                CompletableFuture<V2Client.Result> result=new CompletableFuture<>();
                result.completeExceptionally(new java.io.IOException("Simulated missing diagnostic ACK")); return result;
            }
            if(!type.equals("CLEAR_RUN")) throw new AssertionError("Forbidden command on fake recovery link: "+type);
            service.state.state="IDLE"; service.state.runId=0;
            return CompletableFuture.completedFuture(new V2Client.Result(true,"CLEARED"));
        }
        public CompletableFuture<V2Client.Result> emergencyStop(String run) {
            commands.add("ESTOP"); service.state.state="STOPPED";
            handler.removeCallbacksAndMessages(null); handler.postDelayed(new Runnable() {
                public void run() {
                    if(closed) return;
                    if(sensorMissing) { service.state.state="FAULT"; service.state.lastError="FAULT_PAN_SENSOR"; }
                    service.state.lastStatusMs=SystemClock.elapsedRealtime();
                    try { Field f=field(PanoramaService.class,"statusVersion"); f.setLong(service,f.getLong(service)+1); }
                    catch(Exception ex) { throw new RuntimeException(ex); }
                    handler.postDelayed(this,200);
                }
            },200);
            return CompletableFuture.completedFuture(new V2Client.Result(true,"STOPPED"));
        }
        public void fail(String reason) { throw new AssertionError("Unexpected fake disconnect: "+reason); }
        public void close() { closed=true; handler.removeCallbacksAndMessages(null); }
    }
}
