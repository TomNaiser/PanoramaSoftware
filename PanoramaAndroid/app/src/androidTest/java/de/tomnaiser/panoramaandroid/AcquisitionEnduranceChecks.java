package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/** Explicit real hardware + shutter test. Uses the production service and visible Run page. */
final class AcquisitionEnduranceChecks {
    private Instrumentation runner;
    private PanoramaService service;
    private Activity activity;
    private File directory;
    private PanoramaPlan plan;
    private boolean loaded, stopConfirmed;
    private long started, lastOrdinal, maxUiMs, probes;
    private String outcome="preparing", failure="", lastState="UNKNOWN";
    private long completed;
    private PrintWriter states, captures;
    private final Handler ui=new Handler(Looper.getMainLooper());

    void run(Instrumentation instrumentation,Bundle arguments,String expectedFirmware) throws Exception {
        runner=instrumentation;Context context=runner.getTargetContext();
        String tag=arguments.getString("enduranceTag","");
        require(tag.matches("[a-z0-9-]{1,64}"),"Unique enduranceTag required");
        // Base64 avoids shell interpretation of protocol field separators by adb shell.
        String planText=new String(android.util.Base64.decode(arguments.getString("planFieldsBase64",""),android.util.Base64.DEFAULT),StandardCharsets.UTF_8);
        plan=PanoramaPlan.parse(planText.split("\\|",-1));
        require(plan.total()>=1000 && plan.policy==1 && plan.heartbeatMs==5000,"At least 1000 points and stop on lost heartbeat required");
        int diagnosticLimit=Integer.parseInt(arguments.getString("stopAfterCaptures","0"));
        require(diagnosticLimit==0 || diagnosticLimit>0 && diagnosticLimit<plan.total(),"Valid explicit partial diagnostic limit required");
        int minutes=Integer.parseInt(arguments.getString("maximumMinutes","120"));
        require(minutes>=1 && minutes<=120,"Bounded test duration");
        directory=new File(context.getFilesDir(),"endurance/"+tag);
        require(!directory.exists() && directory.mkdirs(),"New durable test directory required");
        Files.write(new File(directory,"plan.txt").toPath(),String.join("|",plan.fields()).getBytes(StandardCharsets.UTF_8));
        started=SystemClock.elapsedRealtime();
        states=new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(directory,"states.csv")),StandardCharsets.UTF_8),true);
        captures=new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(directory,"captures.csv")),StandardCharsets.UTF_8),true);
        states.println("elapsedMs,uiMs,state,phase,completed,nextIndex,panMdeg,tiltMdeg,lastError,fresh");
        captures.println("elapsedMs,ordinal,imageIndex,column,row,targetPanMdeg,targetTiltMdeg,actualPanMdeg,actualTiltMdeg,reconstructed");
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName n,IBinder b) {service=((PanoramaService.LocalBinder)b).service();}
            public void onServiceDisconnected(ComponentName n) {service=null;}
        };
        boolean bound=false;
        try {
            main(() -> require(context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE),"Bind production service"));bound=true;
            await(() -> service!=null,5000);
            main(() -> {
                require(!service.hasConnection(),"Existing user connection must remain untouched");
                String address=context.getSharedPreferences("settings",0).getString("device",null);
                require(address!=null,"Saved controller required");
                context.startForegroundService(new Intent(context,PanoramaService.class).putExtra("address",address));
            });
            // Periodic STATUS/MANUAL_STATUS can arrive before the HELLO capability bundle.
            await(() -> service.fresh() && service.state.manualStatusFresh(SystemClock.elapsedRealtime()) &&
                service.state.limitsKnown && service.state.tiltCalibrationKnown &&
                service.state.captureStability!=null && service.state.warningSettingsSupported &&
                service.state.runMaximumTempo>=Math.max(plan.panSpeed,plan.tiltSpeed),20000);
            main(() -> {
                require(service.state.firmware.equals(expectedFirmware),"Expected normal firmware");
                require(service.state.runId==0 && service.state.state.equals("IDLE") && service.state.positionSafe(),"Head must be idle without a user job");
                plan.validate(service.state);
                for(int i=0;i<plan.total();i++) {
                    int[] t=plan.target(i);
                    require(t[0]>=service.state.panMin+2500 && t[0]<=service.state.panMax-2500 &&
                        t[1]>=service.state.tiltMin+2500 && t[1]<=service.state.tiltMax-2500,"Every target preserves the 2.5 degree limit reserve");
                }
                if(service.incident.needsAcknowledgement()) {
                    require(service.incident.key.endsWith("|DEVICE_RESTART"),"Only expected upload restart may be acknowledged");
                    service.acknowledgeIncident(service.incident.id);
                }
            });
            await(() -> service.navigationAllowed() && !service.busy,10000);
            activity=runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(() -> {
                activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                try {java.lang.reflect.Method page=MainActivity.class.getDeclaredMethod("selectPage",int.class);page.setAccessible(true);page.invoke(activity,3);}
                catch(Exception ex) {throw new AssertionError(ex);}
                service.support.append("TEST_START ACQUISITION_ENDURANCE tag="+tag+" run="+plan.runId+" planned="+plan.total());
                loaded=true;service.command("LOAD_PLAN",plan.fields());
            });
            await(() -> !service.busy && service.state.state.equals("PLAN_READY") && service.state.runId==plan.runId,10000);
            main(() -> service.command("GET_SUPPORT_BUNDLE"));
            await(() -> !service.busy && service.state.plan!=null && plan.sameRecipe(service.state.plan),10000);
            main(() -> service.runCommand("START"));
            await(() -> !service.busy && service.state.state.equals("RUNNING"),10000);
            outcome="running";
            long deadline=SystemClock.elapsedRealtime()+minutes*60000L, lastProgress=SystemClock.elapsedRealtime(), previous=-1, nextReport=0;
            while(SystemClock.elapsedRealtime()<deadline) {
                if(new File(directory,"STOP").exists()) {outcome="operator_stopped";throw new AssertionError("Operator STOP file");}
                String[] stateRow={null};
                main(() -> {
                    long now=SystemClock.elapsedRealtime();
                    lastState=service.state.state;completed=service.state.completed;
                    stateRow[0]=(now-started)+","+lastUiMs+","+lastState+","+service.state.phase+","+completed+","+service.state.nextImageIndex+","+
                        service.state.pan+","+service.state.tilt+","+service.state.lastError+","+service.fresh();
                    require(service.fresh() && service.state.runId==plan.runId,"Fresh original run identity required");
                    require(service.state.revision==0,"No column navigation during fixed acquisition test");
                    require(!service.incident.needsAcknowledgement(),"App incident: "+service.message);
                    require(lastState.equals("RUNNING") || lastState.equals("COMPLETED"),"Acquisition interrupted: "+lastState+" / "+service.state.lastError);
                });
                states.println(stateRow[0]);
                drainCaptures();
                if(completed!=previous) {previous=completed;lastProgress=SystemClock.elapsedRealtime();summary();}
                require(SystemClock.elapsedRealtime()-lastProgress<180000,"No completed point for 180 seconds");
                if(SystemClock.elapsedRealtime()>=nextReport) {
                    nextReport=SystemClock.elapsedRealtime()+15000;
                    Bundle b=new Bundle();b.putString("stream","\nENDURANCE run="+plan.runId+" completed="+completed+"/"+plan.total()+" state="+lastState+" maxUiMs="+maxUiMs+"\n");runner.sendStatus(0,b);
                }
                if(lastState.equals("COMPLETED")) {
                    require(completed==plan.total() && lastOrdinal==plan.total(),"Every point must be stored exactly once");
                    outcome="complete";break;
                }
                if(diagnosticLimit>0 && lastOrdinal>=diagnosticLimit) {outcome="diagnostic_limit";break;}
                Thread.sleep(500);
            }
            require(outcome.equals("complete") || outcome.equals("diagnostic_limit"),"Acquisition test duration reached");
        } catch(Throwable ex) {
            if(!outcome.equals("operator_stopped")) outcome="failed";
            failure=ex.toString();throw ex;
        } finally {
            try {
                if(loaded && service!=null) {
                    main(() -> {require(service.connected && service.state.runId==plan.runId,"Cleanup must own the original run");service.emergencyStop();});
                    await(() -> !service.busy && !service.uncertain && service.fresh() && service.state.state.equals("STOPPED"),10000);
                    stopConfirmed=true;
                    drainCaptures();
                    // Preserve a failed/partial job for investigation; clear only a complete test.
                    if(outcome.equals("complete") || outcome.equals("diagnostic_limit")) {
                        main(() -> service.command("CLEAR_RUN"));
                        await(() -> !service.busy && service.fresh() && service.state.runId==0 && service.state.state.equals("IDLE"),10000);
                    }
                    service.support.append("TEST_END ACQUISITION_ENDURANCE run="+plan.runId+" outcome="+outcome+" completed="+completed);
                    Files.write(new File(directory,"support.log").toPath(),service.support.snapshot().getBytes(StandardCharsets.UTF_8));
                }
            } finally {
                summary();states.close();captures.close();
                if(bound) main(() -> {if(service!=null) service.disconnect();context.unbindService(binding);});
            }
        }
    }
    private long lastUiMs;
    private void main(Runnable action) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        long sent=SystemClock.elapsedRealtime();
        ui.post(() -> {try {lastUiMs=SystemClock.elapsedRealtime()-sent;maxUiMs=Math.max(maxUiMs,lastUiMs);probes++;action.run();}catch(Throwable ex){error.set(ex);}finally{done.countDown();}});
        require(done.await(5,TimeUnit.SECONDS),"Android main thread unresponsive for 5 seconds");
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void await(java.util.function.BooleanSupplier ready,int timeout) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<deadline) {
            boolean[] yes={false};main(() -> yes[0]=ready.getAsBoolean());if(yes[0]) return;Thread.sleep(100);
        }
        throw new AssertionError("App state timeout: "+(service==null?"no service":service.message));
    }
    private void drainCaptures() {
        try(Cursor c=service.captures.getReadableDatabase().rawQuery(
            "SELECT c.ordinal,c.payload FROM captures c JOIN runs r ON r.id=c.run WHERE r.run_id=? AND c.manual=0 AND c.ordinal>? ORDER BY c.ordinal",
            new String[]{""+plan.runId,""+lastOrdinal})) {
            while(c.moveToNext()) {
                long ordinal=c.getLong(0);String[] f=c.getString(1).split("\\|",-1);
                require(ordinal==lastOrdinal+1 && f.length==13 && f[0].equals("IMAGE_COMPLETED") &&
                    Long.parseLong(f[2])==plan.runId && Long.parseLong(f[5])==ordinal-1,"Unique ordered original capture identity");
                captures.println((SystemClock.elapsedRealtime()-started)+","+ordinal+","+f[5]+","+f[6]+","+f[7]+","+f[8]+","+f[9]+","+f[10]+","+f[11]+","+f[12]);
                lastOrdinal=ordinal;
            }
        }
    }
    private void summary() throws Exception {
        JSONObject s=new JSONObject().put("runId",plan.runId).put("planned",plan.total()).put("completed",completed).put("stored",lastOrdinal)
            .put("outcome",outcome).put("failure",failure).put("lastState",lastState).put("stopConfirmed",stopConfirmed)
            .put("elapsedMs",SystemClock.elapsedRealtime()-started).put("uiProbes",probes).put("maxUiMs",maxUiMs);
        File temporary=new File(directory,"summary.tmp");Files.write(temporary.toPath(),s.toString(2).getBytes(StandardCharsets.UTF_8));
        Files.move(temporary.toPath(),new File(directory,"summary.json").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    private static void require(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
}
