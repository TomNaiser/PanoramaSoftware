package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.os.*;
import android.widget.Button;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Runs only in the isolated validation app: real SQLite, service and dialog with a fake transport. */
final class RestRecoveryChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private int checks;
    private String stage="service binding";
    private final Handler ui=new Handler(Looper.getMainLooper());
    int run(Instrumentation instrumentation) throws Exception {
        runner=instrumentation;Context context=runner.getTargetContext();
        check(context.getPackageName().endsWith(".validation"),"Must use isolated validation application, never the user's capture store");
        String name="rest-checks-"+UUID.randomUUID()+".db";
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName n,IBinder binder) { service=((PanoramaService.LocalBinder)binder).service(); }
            public void onServiceDisconnected(ComponentName n) { service=null; }
        };
        main(() -> check(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind real service"));
        await(() -> service!=null);
        CaptureStore original=service.captures;
        CaptureStore store=new CaptureStore(context,name);
        Activity activity=null;
        try {
            PanoramaPlan plan=PanoramaPlan.parse("94859692|1|0|5625|2373|65|4|2062|-3083|500|300|300|1700|1700|1|5000".split("\\|"));
            FakeLink fake=new FakeLink();
            main(() -> {
                check(!service.hasConnection(),"No hardware session before injection");
                service.captures=store;
                set("device",context.getSystemService(BluetoothManager.class).getAdapter().getRemoteDevice("00:06:66:EE:18:E4"));
                set("session",fake);set("sessionIdentityReceived",true);
                context.getSharedPreferences("settings",0).edit().putLong("session.00:06:66:EE:18:E4",133).commit();
                service.connected=true;service.uncertain=false;service.draftNeedsReview=false;service.incident.resolved();
                service.state=faultState(plan,"MOTION_STALLED",24);
                fake.heartbeat();
                try {
                    store.accept("00:06:66:EE:18:E4",service.state,new P2Codec.Frame(1,"PLAN",plan.fields()));
                    for(int i=0;i<24;i++) capture(store,plan,i);
                    store.accept("00:06:66:EE:18:E4",service.state,new P2Codec.Frame(2,"STATUS",statusFields(service.state)));
                } catch(Exception ex) { throw new RuntimeException(ex); }
                check(store.recordFault("00:06:66:EE:18:E4",133,service.state,statusFields(service.state)),"First failure checkpoint recorded");
                check(!store.recordFault("00:06:66:EE:18:E4",133,service.state,statusFields(service.state)),"Repeated fault status is deduplicated");
                service.state.state="STOPPED";fresh();
                check(service.canPrepareRest(),"Acknowledged same-boot stopped run offers recovery");
                service.state.tiltZeroOffset=1000;check(!service.canPrepareRest(),"Changed tilt reference blocks original coordinates even within the same boot");service.state.tiltZeroOffset=0;
                context.getSharedPreferences("settings",0).edit().putLong("session.00:06:66:EE:18:E4",134).commit();
                check(!service.canPrepareRest(),"Changed boot blocks old coordinates");
                context.getSharedPreferences("settings",0).edit().putLong("session.00:06:66:EE:18:E4",133).commit();
                service.state.panMax=10000;
                try { service.prepareRest(false,plan.runId,24);throw new AssertionError("Limits ignored"); }
                catch(IllegalArgumentException expected) { check(fake.commands.isEmpty(),"Limits checked before clearing source"); }
                service.state.panMax=180000;
                fake.rejectClear=true;service.prepareRest(false,plan.runId,24);
            });
            await(() -> !service.preparingRest && !service.busy);
            main(() -> {
                check(fake.commands.equals(Collections.singletonList("CLEAR_RUN")),"Rejected clear never sends LOAD or START");
                check(service.state.state.equals("STOPPED") && service.state.runId==plan.runId,"Rejected clear preserves source");
                fake.commands.clear();fake.rejectClear=false;fake.rejectLoad=true;
                service.prepareRest(false,plan.runId,24);
            });
            await(() -> !service.preparingRest && !service.busy);
            main(() -> {
                check(fake.commands.equals(Arrays.asList("CLEAR_RUN","LOAD_PLAN")),"Rejected load is never retried or started automatically");
                check(service.state.state.equals("IDLE") && service.canPrepareRest(),"Can prepare again after interrupted CLEAR/LOAD");
                CaptureStore reopened=new CaptureStore(context,name);
                check(reopened.pendingCheckpoint("00:06:66:EE:18:E4")!=null,"Interrupted transfer survives database reopen");reopened.close();
                fake.commands.clear();fake.rejectLoad=false;fresh();
            });
            activity=runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity screen=activity;
            stage="Activity service binding";
            main(() -> { screen.setShowWhenLocked(true);screen.setTurnScreenOn(true); });
            await(() -> activityService(screen)==service);
            main(() -> {
                render(screen);
                try { Method m=MainActivity.class.getDeclaredMethod("selectPage",int.class);m.setAccessible(true);m.invoke(screen,3); }
                catch(Exception ex) { throw new RuntimeException(ex); }
                Button button=(Button)screen.findViewById(android.R.id.content).findViewWithTag("restRecovery");
                check(button!=null && button.isEnabled() && button.getVisibility()==android.view.View.VISIBLE,"Recovery action shown after acknowledgement");
                button.performClick();
            });
            runner.waitForIdleSync();
            main(() -> {
                AlertDialog dialog=latestDialog();
                check(dialog!=null,"Rest selection dialog opens");
                check(dialog.getListView().getAdapter().getCount()==2,"Both column choices available");
                dialog.getListView().performItemClick(dialog.getListView().getChildAt(1),1,1);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            stage="rest loading after UI selection";
            await(() -> !service.preparingRest && !service.busy && service.state.state.equals("PLAN_READY"));
            main(() -> {
                check(fake.commands.equals(Arrays.asList("LOAD_PLAN","GET_SUPPORT_BUNDLE")),"Retry from confirmed IDLE loads only; never START");
                PanoramaPlan rest=service.state.plan;
                check(rest.startPan==15935 && rest.columns==60,"UI selects exact previous original column");
                check(service.state.completed==0,"Loading alone counts no pictures");
                CaptureStore.Checkpoint source=store.checkpoint("00:06:66:EE:18:E4",plan.runId);
                check(source!=null,"Original checkpoint retained");
                try { for(int i=0;i<4;i++) capture(store,rest,i); } catch(Exception ex) { throw new RuntimeException(ex); }
                String csv=store.exportLinked(source.key,false),xml=store.exportLinked(source.key,true);
                check(csv.split("\n").length==29,"Combined CSV includes 24 original and 4 repeated photographs");
                check(xml.split("<pict ").length-1==28,"XML retains duplicate photographs in camera order");
                check(csv.contains(";false;true"),"Repeated target marked without deleting it");
                check(csv.contains("parent_run_key"),"Run ancestry exported");
                DeviceState fault=faultState(rest,"EXPOSURE_UNSTABLE",4);
                check(store.recordFault("00:06:66:EE:18:E4",133,fault,statusFields(fault)),"Possible uncounted exposure stored");
                check(store.hasUncertainExposure(source.key),"Exposure uncertainty applies to complete linked panorama");
                check(store.exportLinked(source.key,false).contains(";true;true"),"Camera order uncertainty marked in combined manifest");
                check(store.exportInterruptions(source.key).contains("EXPOSURE_UNSTABLE;4;1;0;4;true"),"Failure audit includes exact failed index and possible additional camera file");
                CaptureStore reopened=new CaptureStore(context,name);
                check(reopened.exportLinked(source.key,true).equals(xml),"Linked export survives database reopen");reopened.close();
                check(!fake.commands.contains("START") && !fake.commands.contains("MANUAL_MOVE"),"Whole check never starts drive or camera");
                fake.commands.clear();service.state.state="RUNNING";fresh();
                String[] measured=("1|"+rest.runId+"|27|0|26|6|2|28104|-4013|28000|879|0").split("\\|");
                set("pendingQuality",new CaptureQualityGuard().accept(measured));
                invoke("pauseForQuality");
            });
            stage="quality pause";await(() -> !service.busy && service.incident.needsAcknowledgement());
            main(() -> {
                check(fake.commands.equals(Collections.singletonList("PAUSE")),"Large measured error requests PAUSE, never abort or clear");
                check(service.state.state.equals("PAUSED") && !service.incident.requiresStop,"Quality pause retains resumable job");
                service.acknowledgeIncident(service.incident.id);
                check(service.state.state.equals("PAUSED") && !service.incident.active && !service.recovering,"Quality acknowledgement does not ESTOP paused job");
                check(fake.commands.equals(Collections.singletonList("PAUSE")),"Acknowledgement neither starts nor clears job");
            });
            stage="recoverable acquisition pause";
            main(() -> {
                fake.commands.clear();service.state.recoveryPauseSupported=true;service.state.lastError="EXPOSURE_UNSTABLE";
                service.state.nextImageIndex=7;service.state.completed=7;fresh();
                service.observeRecoveryPause(statusFields(service.state));
                check(service.incident.needsAcknowledgement() && !service.incident.requiresStop,"Firmware pause requests acknowledgement without ESTOP");
                long incident=service.incident.id;service.observeRecoveryPause(statusFields(service.state));
                check(service.incident.id==incident,"Repeated paused status does not stack dialogs");
                service.acknowledgeIncident(incident);
                check(service.state.state.equals("PAUSED") && !service.incident.active && fake.commands.isEmpty(),"Exposure-pause acknowledgement preserves loaded job and sends no motion");
                check(service.canRetryColumn(),"Acknowledged loaded raster offers column retry");
                service.retryColumn(true,service.state.runId,7);
            });
            await(() -> !service.busy);
            main(() -> {
                check(fake.commands.equals(Collections.singletonList("RETRY_COLUMN")),"Column retry sends preparation only, never RESUME or START");
                check(service.state.state.equals("PAUSED") && service.state.nextImageIndex==0 && service.state.completed==7,"Previous column retains capture count and paused state");
                check(store.hasUncertainExposure(store.checkpoint("00:06:66:EE:18:E4",plan.runId).key),"Paused exposure remains marked as uncertain in linked exports");
                check(service.state.runId!=0 && service.state.plan!=null,"Stability editing starts with the paused job still loaded");
                service.state.captureStability=new CaptureStabilitySettings(250,250,400,15000,25);service.state.motionConfigRevision=25;fresh();
                fake.commands.clear();
                try {service.saveCaptureStability(.01,.25,.4,15);throw new AssertionError("Invalid span accepted");}
                catch(IllegalArgumentException expected) {check(fake.commands.isEmpty(),"Invalid stability value rejected before transport");}
                service.saveCaptureStability(.35,.45,.55,20);
            });
            stage="stability settings";await(() -> !service.busy);
            main(() -> {
                check(fake.commands.equals(Collections.singletonList("SET_CAPTURE_STABILITY")),"Only atomic stability setting command sent");
                check(service.state.captureStability.spanMdeg==350 && service.state.captureStability.panDriftMdeg==450 && service.state.captureStability.extraWaitMs==20000,"Device-confirmed settings replace active values");
                check(service.message.contains("zurückgelesen"),"Settings success requires confirmed readback");
                check(service.state.state.equals("PAUSED") && service.state.completed==7,"Stability save preserves paused job and capture count");
                render(currentActivity);
                try {
                    Field f=field(MainActivity.class,"inputs");
                    @SuppressWarnings("unchecked") Map<String,android.widget.EditText> inputs=(Map<String,android.widget.EditText>)f.get(currentActivity);
                    check(inputs.get("captureSpan").getText().toString().equals("0.35"),"UI shows actual device span");
                    check(inputs.get("captureExtraWait").getText().toString().equals("20.0"),"UI shows additional wait in seconds");
                } catch(Exception ex) {throw new RuntimeException(ex);}
                service.state.runId=plan.runId;service.state.plan=plan;service.state.state="PLAN_READY";fresh();
                int count=fake.commands.size();
                try {service.saveCaptureStability(.3,.3,.4,15);throw new AssertionError("Loaded plan ignored");}
                catch(IllegalArgumentException expected) {check(fake.commands.size()==count,"Loaded job prevents changing capture guards");}
            });
            migration(context,name+"-v2");
        } finally {
            Activity screen=activity;
            main(() -> { if(screen!=null) screen.finish();service.disconnect();service.captures=original; });
            store.close();context.deleteDatabase(name);main(() -> context.unbindService(binding));
        }
        return checks;
    }
    private AlertDialog latestDialog() {
        // The UI deliberately keeps this transient dialog as a field for lifecycle cleanup and deterministic checks.
        try { return (AlertDialog)field(MainActivity.class,"restDialog").get(currentActivity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private Activity currentActivity;
    private void migration(Context context,String name) throws Exception {
        try {
            CaptureStore old=new CaptureStore(context,name);
            PanoramaPlan p=PanoramaPlan.parse("11|1|0|0|0|2|2|1000|-1000|500|30|300|500|500|0|0".split("\\|"));
            capture(old,p,0);String key=old.runs().get(0).key;String before=old.export(key,true);
            android.database.sqlite.SQLiteDatabase db=old.getWritableDatabase();
            db.execSQL("DROP TABLE run_links");db.execSQL("DROP TABLE run_faults");db.setVersion(2);old.close();
            try(CaptureStore upgraded=new CaptureStore(context,name)) {
                check(upgraded.getReadableDatabase().getVersion()==4,"Real v2-to-v4 SQLite upgrade");
                check(upgraded.export(key,true).equals(before),"Upgrade preserves existing capture identities, timestamps and coordinates");
            }
        } finally { context.deleteDatabase(name); }
    }
    private void invoke(String name) {
        try { Method m=PanoramaService.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(service); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private PanoramaService activityService(Activity activity) {
        try { return (PanoramaService)field(MainActivity.class,"service").get(activity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private void render(Activity activity) {
        currentActivity=activity;
        try { Method m=MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private DeviceState faultState(PanoramaPlan p,String error,int index) {
        DeviceState s=new DeviceState();s.compatible=s.confirmed=s.limitsKnown=s.inactiveNavigation=s.tiltCalibrationKnown=true;s.runMaximumTempo=2000;
        s.panMin=-180000;s.panMax=180000;s.tiltMin=-45000;s.tiltMax=30000;s.plan=p;s.runId=p.runId;
        s.state="FAULT";s.phase="NONE";s.lastError=error;s.nextImageIndex=index;s.completed=index;s.total=p.total();
        s.column=index/p.rows;s.row=index%p.rows;s.pan=15688;s.tilt=-1582;s.lastStatusMs=SystemClock.elapsedRealtime();return s;
    }
    private static String[] statusFields(DeviceState s) {
        int[] target=s.plan.target((int)s.nextImageIndex);
        return (s.runId+"|"+s.state+"|NONE|"+s.completed+"|"+s.total+"|"+s.column+"|"+s.row+"|"+target[0]+"|"+target[1]+"|"+s.pan+"|"+s.tilt+"|0|1|"+s.lastError+"|"+s.nextImageIndex+"|0|1|"+s.completed+"|0|0").split("\\|");
    }
    private static void capture(CaptureStore store,PanoramaPlan p,int index) throws Exception {
        int[] t=p.target(index);
        String[] f=("1|"+p.runId+"|"+(index+1)+"|0|"+index+"|"+t[2]+"|"+t[3]+"|"+t[0]+"|"+t[1]+"|"+t[0]+"|"+t[1]+"|0").split("\\|");
        store.accept("00:06:66:EE:18:E4",new DeviceState(),new P2Codec.Frame(1,"IMAGE_COMPLETED",f));
    }
    private void fresh() {
        service.state.lastStatusMs=SystemClock.elapsedRealtime();
        try { Field f=field(PanoramaService.class,"statusVersion");f.setLong(service,f.getLong(service)+1); }
        catch(Exception ex) { throw new RuntimeException(ex); }
    }
    private static Field field(Class<?> type,String name) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);return f; }
    private void set(String name,Object value) { try { field(PanoramaService.class,name).set(service,value); } catch(Exception ex) { throw new RuntimeException(ex); } }
    private void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private void main(Runnable action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error.set(ex); } });
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        for(int i=0;i<100;i++) { java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();main(() -> ready.set(condition.getAsBoolean()));if(ready.get()) return;Thread.sleep(100); }
        throw new AssertionError("Rest preparation timed out: "+stage+"; "+service.state.state+"; "+service.message);
    }
    private final class FakeLink implements DeviceLink {
        final List<String> commands=new ArrayList<>();boolean rejectClear,rejectLoad;
        void heartbeat() { fresh();ui.postDelayed(this::heartbeat,200); }
        public CompletableFuture<V2Client.Result> send(String type,String... f) {
            commands.add(type);
            if(type.equals("CLEAR_RUN")) {
                if(rejectClear) return CompletableFuture.completedFuture(new V2Client.Result(false,"REJECTED"));
                service.state.state="IDLE";service.state.runId=0;service.state.plan=null;service.state.completed=0;fresh();
            } else if(type.equals("LOAD_PLAN")) {
                if(rejectLoad) return CompletableFuture.completedFuture(new V2Client.Result(false,"REJECTED"));
                service.state.plan=PanoramaPlan.parse(f);service.state.runId=service.state.plan.runId;service.state.state="PLAN_READY";fresh();
            } else if(type.equals("GET_SUPPORT_BUNDLE")) fresh();
            else if(type.equals("PAUSE")) { service.state.state="PAUSED";fresh(); }
            else if(type.equals("RETRY_COLUMN")) {
                check(Long.parseLong(f[0])==service.state.runId,"Retry stays in original run");
                service.state.nextImageIndex=Math.max(0,service.state.nextImageIndex/service.state.plan.rows-Integer.parseInt(f[1]))*service.state.plan.rows;
                service.state.revision++;service.state.lastError="NONE";fresh();
            } else if(type.equals("SET_CAPTURE_STABILITY")) {
                CaptureStabilitySettings requested=CaptureStabilitySettings.parse(f);
                check(requested.revision==service.state.motionConfigRevision,"Setting command uses confirmed revision");
                String[] confirmed=requested.fields();confirmed[4]=""+(requested.revision+1);
                service.state.accept(new P2Codec.Frame(1,"CAPTURE_STABILITY_SETTINGS",confirmed),SystemClock.elapsedRealtime());
                service.observeStabilitySettings();fresh();
            }
            else throw new AssertionError("Forbidden command: "+type);
            return CompletableFuture.completedFuture(new V2Client.Result(true,"OK"));
        }
        public CompletableFuture<V2Client.Result> emergencyStop(String run) { throw new AssertionError("Unexpected ESTOP"); }
        public void fail(String reason) { throw new AssertionError(reason); }
        public void close() { ui.removeCallbacksAndMessages(null); }
    }
}
