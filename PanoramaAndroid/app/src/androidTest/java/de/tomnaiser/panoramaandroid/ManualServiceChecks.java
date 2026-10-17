package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Bound-service checks run with a locked screen and never open a hardware transport. */
final class ManualServiceChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private int checks;
    int run(Instrumentation instrumentation) throws Exception {
        runner=instrumentation;
        Context context=runner.getTargetContext();
        String originalManualPanSpeed=context.getSharedPreferences("settings",0).getString("manualPanSpeed",null);
        String originalManualSpeed=context.getSharedPreferences("settings",0).getString("manualSpeed",null);
        String originalPanJogSpeed=context.getSharedPreferences("settings",0).getString("panJogSpeed",null);
        String originalJogSpeed=context.getSharedPreferences("settings",0).getString("jogSpeed",null);
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName name,IBinder binder) { service=((PanoramaService.LocalBinder)binder).service(); }
            public void onServiceDisconnected(ComponentName name) { service=null; }
        };
        main(() -> check(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind service"));
        try {
            await(() -> service!=null);
            main(() -> check(!service.hasConnection(),"No real controller session"));
            IncidentTracker preserved=new IncidentTracker();
            main(() -> copyIncident(service.incident,preserved));
            DeviceState original=service.state;
            FakeLink fake=new FakeLink();
            try {
                main(() -> {
                    setSession(fake); service.connected=true; service.state=new DeviceState();
                    service.incident.active=false; service.incident.acknowledged=true; service.incident.requiresStop=false;
                    service.state.compatible=service.state.confirmed=service.state.limitsKnown=service.state.profiledMotion=true;
                    service.state.state="IDLE"; service.state.lastStatusMs=SystemClock.elapsedRealtime();
                    service.state.panMin=-180000; service.state.panMax=180000; service.state.tiltMin=-45000; service.state.tiltMax=30000;
                    service.support.append("TEST_MODE SIMULATED_MANUAL_STATUS_NO_HARDWARE");
                    service.state.accept(new P2Codec.Frame(1,"MANUAL_STATUS_CAPABILITIES",new String[]{"1"}),SystemClock.elapsedRealtime());
                    status(0,"NONE","IDLE",0,0);
                    service.setManualTempos(500,200); service.moveToUi(0,1);
                });
                await(() -> !service.busy);
                main(() -> {
                    check(fake.commands.equals(Collections.singletonList("MANUAL_MOVE")),"Modern move must start without GET_DEBUG_LOG");
                    check(Arrays.equals(fake.lastMoveFields,new String[]{"0","1000","500","200"}),"Plan target sends tested Pan 500 and preserved Tilt 200 separately");
                    boolean invalid=false; try { service.setManualTempos(500,0); } catch(IllegalArgumentException expected) { invalid=true; }
                    check(invalid && context.getSharedPreferences("settings",0).getString("manualSpeed","").equals("200"),"Invalid tempo pair does not partly overwrite saved settings");
                    check(service.manualMoving,"ACK alone cannot complete a move");
                    status(901,"MOVE","REACHED",0,1000);
                });
                Thread.sleep(600);
                main(() -> {
                    check(service.manualMoving,"Old command cannot finish current move");
                    status(902,"MOVE","REACHED",0,2000);
                });
                Thread.sleep(600);
                main(() -> {
                    check(service.manualMoving,"Wrong target cannot finish current move");
                    status(902,"MOVE","REACHED",0,1000);
                });
                await(() -> !service.manualMoving);
                main(() -> {
                    check(!service.uncertain && service.navigationAllowed(),"Replayed completion releases service without diagnostic event");
                    check(fake.commands.equals(Collections.singletonList("MANUAL_MOVE")),"No diagnostic requests while moving or completing");
                    fake.rejectMove=true; service.moveToUi(0,2);
                });
                await(() -> !service.busy);
                main(() -> {
                    check(!service.manualMoving && !service.uncertain && service.navigationAllowed(),"Rejected move leaves service usable");
                    fake.rejectMove=false; service.trigger(30);
                });
                await(() -> !service.busy);
                main(() -> {
                    check(service.manualTrigger,"Trigger ACK is not a completed pulse");
                    status(904,"TRIGGER","TRIGGERED",0,1000);
                });
                await(() -> !service.manualTrigger);
                main(() -> {
                    check(!service.uncertain && service.manualAllowed(),"Lost image event does not keep trigger controls blocked");
                    fake.delayAck=true; service.moveToUi(0,3);
                    status(905,"MOVE","REACHED",0,3000);
                    fake.delayedMove.complete(new V2Client.Result(true,"MOVING",905));
                });
                await(() -> !service.busy);
                main(() -> {
                    check(service.manualMoving,"Status predating ACK cannot complete a reused command sequence");
                    status(905,"MOVE","REACHED",0,3000);
                });
                await(() -> !service.manualMoving);
                main(() -> check(service.navigationAllowed(),"Post-ACK replay completes a fast move or delayed ACK safely"));
                main(() -> {
                    service.state.jogSupported=true; fake.commands.clear(); fake.delayAck=true;
                    service.beginJog(1,-1);
                    check(service.jog.held && service.busy,"Finger gesture owns exactly one pending start");
                    service.endJog();
                });
                await(() -> !service.busy);
                main(() -> {
                    check(!service.jog.held && !service.jog.stopping && !service.manualMoving && !service.uncertain,
                        "Release before ACK cancels start without blocking recovery");
                    check(fake.commands.equals(Arrays.asList("MANUAL_JOG_START","MANUAL_JOG_STOP")),"Release never sends incremental targets");
                    fake.delayAck=false; fake.commands.clear(); service.beginJog(0,1);
                });
                await(() -> !service.busy && service.jog.sequence>0);
                Thread.sleep(450);
                main(() -> {
                    check(Collections.frequency(fake.commands,"MANUAL_JOG_START")==1 &&
                        Collections.frequency(fake.commands,"MANUAL_JOG_KEEPALIVE")>=1,"Held finger renews lease without new starts or targets");
                    service.endJog();
                });
                await(() -> !service.busy);
                long afterStop=fake.commands.stream().filter(c -> c.startsWith("MANUAL_JOG") || c.equals("MANUAL_MOVE")).count(); Thread.sleep(400);
                main(() -> {
                    status(905,"MOVE","REACHED",0,3000);
                    check(fake.commands.stream().filter(c -> c.startsWith("MANUAL_JOG") || c.equals("MANUAL_MOVE")).count()==afterStop,
                        "No queued moves or keepalives after release");
                    check(service.jogAllowed(),"Fresh terminal status keeps next gesture usable");
                    service.beginJog(1,-1);
                });
                await(() -> !service.busy && service.jog.sequence>0);
                main(() -> {
                    service.state.accept(new P2Codec.Frame(3,"MANUAL_STATUS",new String[]{""+service.jog.sequence,"JOG","CANCELLED","0","1000","750","750"}),SystemClock.elapsedRealtime());
                });
                await(() -> !service.jog.held && !service.manualMoving);
                main(() -> {
                    check(service.jogAllowed(),"Firmware lease expiry releases controls without fault acknowledgement");
                    check(!service.endpointCaptureAllowed(),"Lease expiry also waits for post-stop position feedback before endpoint capture");
                });
                main(() -> {
                    service.state.accept(new P2Codec.Frame(4,"MANUAL_JOG_VELOCITY_CAPABILITIES",new String[]{"5000"}),SystemClock.elapsedRealtime());
                    fake.commands.clear(); service.setJogSpeed(5); service.beginJog(1,1);
                });
                await(() -> !service.busy && service.jog.sequence>0);
                main(() -> {
                    check(fake.commands.get(0).equals("MANUAL_JOG_START_VELOCITY") && fake.lastStartSpeed.equals("5000"),"App requests 5 degrees/s independently of legacy step tempo");
                    service.endJog();
                });
                await(() -> !service.busy);
                main(() -> {
                    check(!service.jog.held && !service.manualMoving && !service.uncertain,"Absolute velocity gesture releases without blocked controls");
                    boolean rejected=false; try { service.setJogSpeed(6); } catch(IllegalArgumentException expected) { rejected=true; }
                    check(rejected && !service.jog.held && !service.uncertain,"Excess speed is rejected before any motion without blocking app");
                });
                main(() -> {
                    service.state.accept(new P2Codec.Frame(5,"MANUAL_JOG_PAN_VELOCITY_CAPABILITIES",new String[]{"10000"}),SystemClock.elapsedRealtime());
                    fake.commands.clear(); service.setJogSpeed(0,10); service.beginJog(0,-1);
                });
                await(() -> !service.busy && service.jog.sequence>0);
                main(() -> {
                    check(fake.commands.get(0).equals("MANUAL_JOG_START_VELOCITY") && fake.lastStartSpeed.equals("10000"),"Pan requests 10 degrees/s independently of Tilt 5 degrees/s");
                    service.endJog();
                });
                await(() -> !service.busy);
                main(() -> {
                    fake.delayAck=false;service.moveToUi(0,2);
                });
                await(() -> !service.busy);
                main(() -> status(fake.sequence,"MOVE","TARGET_MISSED",0,2000));
                await(() -> !service.manualMoving);
                main(() -> check(!service.uncertain && !service.incident.needsAcknowledgement() && service.navigationAllowed(),"Target miss releases controls as warning without acknowledgement or ESTOP"));
            } finally {
                main(() -> {
                    service.disconnect(); service.state=original; copyIncident(preserved,service.incident);
                    try { java.lang.reflect.Method save=PanoramaService.class.getDeclaredMethod("saveIncident"); save.setAccessible(true); save.invoke(service); }
                    catch(Exception ex) { throw new RuntimeException(ex); }
                    SharedPreferences.Editor preferences=context.getSharedPreferences("settings",0).edit();
                    if(originalManualPanSpeed==null) preferences.remove("manualPanSpeed"); else preferences.putString("manualPanSpeed",originalManualPanSpeed);
                    if(originalManualSpeed==null) preferences.remove("manualSpeed"); else preferences.putString("manualSpeed",originalManualSpeed);
                    if(originalJogSpeed==null) preferences.remove("jogSpeed"); else preferences.putString("jogSpeed",originalJogSpeed);
                    if(originalPanJogSpeed==null) preferences.remove("panJogSpeed"); else preferences.putString("panJogSpeed",originalPanJogSpeed);
                    preferences.commit();
                });
            }
        } finally { main(() -> context.unbindService(binding)); }
        return checks;
    }
    private void status(int command,String kind,String result,int pan,int tilt) {
        service.state.lastStatusMs=SystemClock.elapsedRealtime();
        service.state.accept(new P2Codec.Frame(3,"MANUAL_STATUS",new String[]{""+command,kind,result,""+pan,""+tilt,"0",command==0?"0":kind.equals("TRIGGER")?"30":"120000"}),SystemClock.elapsedRealtime());
    }
    private static void copyIncident(IncidentTracker from,IncidentTracker to) {
        to.id=from.id; to.key=from.key; to.detail=from.detail; to.active=from.active;
        to.acknowledged=from.acknowledged; to.requiresStop=from.requiresStop;
    }
    private void setSession(DeviceLink link) {
        try { Field field=PanoramaService.class.getDeclaredField("session"); field.setAccessible(true); field.set(service,link); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private void main(Runnable action) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier ready) throws Exception {
        for(int i=0;i<50;i++) {
            java.util.concurrent.atomic.AtomicBoolean result=new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> result.set(ready.getAsBoolean())); if(result.get()) return; Thread.sleep(100);
        }
        throw new AssertionError("Manual service condition not reached within five seconds");
    }
    private void check(boolean value,String text) { checks++; if(!value) throw new AssertionError(text); }
    private static final class FakeLink implements DeviceLink {
        final List<String> commands=new ArrayList<>();
        int sequence=901;
        boolean rejectMove,delayAck;
        CompletableFuture<V2Client.Result> delayedMove;
        String lastStartSpeed;
        String[] lastMoveFields;
        public CompletableFuture<V2Client.Result> send(String type,String... fields) {
            commands.add(type);
            if(type.equals("MANUAL_MOVE")) lastMoveFields=fields.clone();
            sequence++;
            if(type.equals("MANUAL_JOG_KEEPALIVE") || type.equals("GET_STATUS"))
                return CompletableFuture.completedFuture(new V2Client.Result(true,"OK",sequence));
            if(type.equals("MANUAL_MOVE") || type.equals("MANUAL_JOG_START") || type.equals("MANUAL_JOG_START_VELOCITY")) {
                if(type.startsWith("MANUAL_JOG_START")) lastStartSpeed=fields[3];
                if(delayAck) { delayedMove=new CompletableFuture<>(); return delayedMove; }
                return CompletableFuture.completedFuture(new V2Client.Result(!rejectMove,rejectMove?"TARGET_REJECTED":"MOVING",sequence));
            }
            if(type.equals("TRIGGER_ONCE")) return CompletableFuture.completedFuture(new V2Client.Result(true,"TRIGGERED",sequence));
            CompletableFuture<V2Client.Result> result=new CompletableFuture<>();
            result.completeExceptionally(new java.io.IOException("No diagnostic data in this simulated transport")); return result;
        }
        public CompletableFuture<V2Client.Result> stopJog(String token) {
            commands.add("MANUAL_JOG_STOP");
            if(delayedMove!=null && !delayedMove.isDone()) delayedMove.completeExceptionally(new java.io.IOException("Released"));
            return CompletableFuture.completedFuture(new V2Client.Result(true,"JOG_STOPPED",++sequence));
        }
        public CompletableFuture<V2Client.Result> emergencyStop(String run) { throw new AssertionError("Unexpected stop"); }
        public void fail(String reason) { throw new AssertionError(reason); }
        public void close() { }
    }
}
