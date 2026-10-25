package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import java.util.*;

/** Default checks are hardware-free; connectionCheck explicitly opts into HELLO/heartbeat diagnostics. */
public final class AppChecks extends Instrumentation {
    private int assertions;
    private boolean connectionCheck;
    private boolean abortPlanCheck;
    private boolean recoveryCheck;
    private boolean manualStatusCheck;
    private boolean manualHardwareCheck,panPlanHardwareCheck;
    private boolean panoramaHardwareCheck;
    private boolean captureWarningCheck;
    private boolean jogHardwareCheck;
    private boolean jogUiCheck,chartUiCheck,chartLandscape;
    private boolean jogUiHardwareCheck;
    private long connectionDelayMs=3000;
    private boolean connectionSecure=true;
    private String expectedFirmware="2.4.41";
    private boolean restRecoveryCheck;
    private boolean liveExportCheck;
    private int liveExportIterations=1000;
    private boolean stabilityHardwareCheck;
    private Bundle enduranceArguments;
    private boolean acknowledgedIncidentCheck;
    private boolean configureMotorStress;
    private boolean languageCheck;
    @Override public void callActivityOnCreate(android.app.Activity activity,Bundle state) {
        if(restRecoveryCheck || enduranceArguments!=null) {
            activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(android.os.Build.VERSION.SDK_INT>=27) { activity.setShowWhenLocked(true);activity.setTurnScreenOn(true); }
        }
        super.callActivityOnCreate(activity,state);
    }
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments); connectionCheck=arguments!=null && "true".equals(arguments.getString("connectionCheck"));
        languageCheck=arguments!=null && "true".equals(arguments.getString("languageCheck"));
        acknowledgedIncidentCheck=arguments!=null && "true".equals(arguments.getString("acknowledgedIncidentCheck"));
        configureMotorStress=arguments!=null && "true".equals(arguments.getString("configureMotorStress"));
        if(arguments!=null && "true".equals(arguments.getString("acquisitionEndurance"))) enduranceArguments=new Bundle(arguments);
        restRecoveryCheck=arguments!=null && "true".equals(arguments.getString("restRecoveryCheck"));
        liveExportCheck=arguments!=null && "true".equals(arguments.getString("liveExportCheck"));
        if(arguments!=null && arguments.containsKey("liveExportIterations")) liveExportIterations=Integer.parseInt(arguments.getString("liveExportIterations"));
        stabilityHardwareCheck=arguments!=null && "true".equals(arguments.getString("stabilityHardwareCheck"));
        abortPlanCheck=arguments!=null && "true".equals(arguments.getString("abortPlanCheck"));
        recoveryCheck=arguments!=null && "true".equals(arguments.getString("recoveryCheck"));
        manualStatusCheck=arguments!=null && "true".equals(arguments.getString("manualStatusCheck"));
        panPlanHardwareCheck=arguments!=null && "true".equals(arguments.getString("panPlanHardwareCheck"));
        manualHardwareCheck=arguments!=null && "true".equals(arguments.getString("manualHardwareCheck"));
        panoramaHardwareCheck=arguments!=null && "true".equals(arguments.getString("panoramaHardwareCheck"));
        captureWarningCheck=arguments!=null && "true".equals(arguments.getString("captureWarningCheck"));
        jogHardwareCheck=arguments!=null && "true".equals(arguments.getString("jogHardwareCheck"));
        chartLandscape=arguments!=null && "true".equals(arguments.getString("chartLandscape"));
        chartUiCheck=arguments!=null && "true".equals(arguments.getString("chartUiCheck"));
        jogUiCheck=arguments!=null && "true".equals(arguments.getString("jogUiCheck"));
        jogUiHardwareCheck=arguments!=null && "true".equals(arguments.getString("jogUiHardwareCheck"));
        if (arguments!=null && arguments.containsKey("connectionDelayMs")) connectionDelayMs=Long.parseLong(arguments.getString("connectionDelayMs"));
        if(arguments!=null && arguments.containsKey("expectedFirmware")) expectedFirmware=arguments.getString("expectedFirmware");
        if (arguments!=null && arguments.containsKey("connectionTransport")) {
            String transport=arguments.getString("connectionTransport");
            if (!"secure".equals(transport) && !"insecure".equals(transport)) throw new IllegalArgumentException("connectionTransport: secure oder insecure");
            connectionSecure="secure".equals(transport);
        }
        start();
    }
    @Override public void onStart() {
        Bundle report = new Bundle();
        int result=android.app.Activity.RESULT_OK;
        Context context = getTargetContext(); String name = "checks-"+UUID.randomUUID()+".db";
        try {
            if(languageCheck) {
                int checks=new LanguageUiChecks().run(this);
                report.putString("stream","\nPASS: "+checks+" language UI checks; no hardware connection.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(liveExportCheck) {
                int checks=new LivePapywizardChecks().run(this,liveExportIterations);
                report.putString("stream","\nPASS: "+checks+" live Papywizard checks; SQLite, external media, no hardware.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(configureMotorStress) {
                boolean previous=context.getSharedPreferences("settings",Context.MODE_PRIVATE).getBoolean("qualityPause",true);
                if(!context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("qualityPause",false).commit()) throw new AssertionError("Settings not saved");
                report.putString("stream","\nPASS: Optional 1-degree image-quality pause disabled for requested motor stress test; previous="+previous+". No hardware connection.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(acknowledgedIncidentCheck) {
                int checks=new AcknowledgedIncidentChecks().run(this);
                report.putString("stream","\nPASS: "+checks+" acknowledged-incident checks on production service with fake transport; no hardware.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(enduranceArguments!=null) {
                new AcquisitionEnduranceChecks().run(this,enduranceArguments,expectedFirmware);
                report.putString("stream",enduranceArguments.containsKey("stopAfterCaptures")?
                    "\nPASS: Bounded real acquisition diagnostic reached its explicit partial limit; inspect saved summary.\n":
                    "\nPASS: Real acquisition endurance completed through production service and visible Run page.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(stabilityHardwareCheck) {
                int checks=new CaptureStabilityHardwareChecks().run(this,expectedFirmware);
                report.putString("stream","\nPASS: "+checks+" real Bluetooth stability-setting checks; saved/read/restored, no movement or shutter commands.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(restRecoveryCheck) {
                int checks=new RestRecoveryChecks().run(this);
                storageChecks(context,name);captureWarningStorageChecks(context,name);
                report.putString("stream","\nPASS: "+checks+" rest recovery checks and "+assertions+" storage assertions; isolated SQLite/service/dialog, no hardware.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(captureWarningCheck) {
                storageChecks(context,name);captureWarningStorageChecks(context,name);
                report.putString("stream","\nPASS: "+assertions+" storage/warning assertions; no hardware connection.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(panoramaHardwareCheck) {
                int checks=new PanoramaHardwareChecks().run(this,expectedFirmware);
                report.putString("stream","\nPASS: "+checks+" checks: original 35-column / 280-shot panorama through real app service; stopped and test plan removed.\n");
                finish(android.app.Activity.RESULT_OK,report);return;
            }
            if(chartUiCheck) {
                int checks=new ChartUiChecks().run(this,chartLandscape);
                report.putString("stream","\nPASS: "+checks+" assertions: pinned Plan/Run plots, exact Pan +/-180 reset, large/normal toggle, Back and visible emergency stop. No movement or shutter commands.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(panPlanHardwareCheck) {
                int checks=new ManualHardwareChecks().runPanPlan(this);
                report.putString("stream","\nPASS: "+checks+" assertions: real app Plan-target service, Pan 500 / Tilt 200, 20-degree Pan and return, firmware REACHED, controls released. No shutter commands.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(jogUiHardwareCheck) {
                int checks=new ManualHardwareChecks().runUiJog(this);
                report.putString("stream","\nPASS: "+checks+" assertions: real Plan-page touch, all four held arrows, release, priority stop and IDLE. No shutter commands.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(jogUiCheck) {
                int checks=new JogUiChecks().run(this);
                report.putString("stream","\nPASS: "+checks+" assertions: held arrows across right diagram edge, stable rendering, release, focus loss, live timing dialog and durable settings. Simulated transport, no hardware.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(jogHardwareCheck) {
                int checks=new ManualHardwareChecks().runJog(this);
                report.putString("stream","\nPASS: "+checks+" assertions: real held Pan/Tilt both directions, immediate release, safe stop. No shutter commands.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(manualHardwareCheck) {
                int checks=new ManualHardwareChecks().run(this);
                report.putString("stream","\nPASS: "+checks+" assertions: physical 1-degree tilt and return, real service/firmware status, ESTOP and IDLE. No camera command.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(manualStatusCheck) {
                int checks=new ManualServiceChecks().run(this);
                report.putString("stream","\nPASS: "+checks+" assertions: manual action status in real service, simulated transport, no hardware.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(recoveryCheck) {
                int checks=new ServiceRecoveryChecks().run(this,launchActivity(context));
                report.putString("stream","\nPASS: "+checks+" assertions: simulated service/dialog recovery, retained run, missing diagnostic ACK, persistent sensor fault. No hardware transport.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if(abortPlanCheck) {
                abortPlanChecks(context);
                report.putString("stream","\nPASS: Realer Service: Einpunktplan geladen, STOP/STOPPED, CLEAR_RUN/IDLE, manuelle Freigabe. Kein START und kein Kamerabefehl.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            if (connectionCheck) {
                new ConnectionChecks().run(this,connectionDelayMs,connectionSecure,expectedFirmware);
                report.putString("stream","\nPASS: Zwei Bluetooth-Sitzungen mit aktuellem Firmware-/Statusnachweis.\n");
                finish(android.app.Activity.RESULT_OK,report); return;
            }
            storageChecks(context,name);
            manualFeedbackChecks();
            context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit()
                .putString("startPan","-40").putString("startTilt","10")
                .putString("endPan","-30").putString("endTilt","5").commit();
            android.app.Activity activity = launchActivity(context);
            check(activity != null,"Activity startet");
            android.app.KeyguardManager keyguard=context.getSystemService(android.app.KeyguardManager.class);
            check(!keyguard.isKeyguardLocked(),"Handy vor UI-/Rotationstest entsperren");
            android.app.Activity visible=activity;
            mainCall(() -> visible.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            draftChecks(activity,true);
            graphicalChecks(activity);
            navigationChecks(activity);
            activity=landscapeChecks(activity);
            ActivityMonitor recreated=addMonitor(MainActivity.class.getName(),null,false);
            try {
                android.app.Activity current=activity;
                mainCall(current::recreate);
                android.app.Activity replacement=recreated.waitForActivityWithTimeout(5000);
                check(replacement!=null && replacement!=activity,"Activity-Neuerstellung abgeschlossen");
                draftChecks(replacement,false);
            } finally { removeMonitor(recreated); }
            report.putString("stream", "\n"+assertions+" assertions passed: SQLite, XML/CSV, graphical touch/projection, saved plot, duplicate events, reconstruction, manual completion/quiet feedback, Activity recreation.\n");
        } catch (Throwable error) {
            report.putString("stream","\nFAILED: "+error.toString()+"\n"); result=android.app.Activity.RESULT_CANCELED;
        } finally { context.deleteDatabase(name); }
        finish(result,report);
    }
    @SuppressWarnings("unchecked")
    private void draftChecks(android.app.Activity activity,boolean coldStart) throws Exception {
        java.lang.reflect.Field inputField=MainActivity.class.getDeclaredField("inputs"); inputField.setAccessible(true);
        java.lang.reflect.Field serviceField=MainActivity.class.getDeclaredField("service"); serviceField.setAccessible(true);
        java.lang.reflect.Field planField=MainActivity.class.getDeclaredField("pendingPlan"); planField.setAccessible(true);
        java.lang.reflect.Method calculate=MainActivity.class.getDeclaredMethod("calculate"); calculate.setAccessible(true);
        java.lang.reflect.Method remove=MainActivity.class.getDeclaredMethod("removePlan"); remove.setAccessible(true);
        awaitUi(() -> { try { return serviceField.get(activity)!=null; } catch(Exception ex) { throw new RuntimeException(ex); } });
        mainCall(() -> {
            try {
                Map<String,android.widget.EditText> fields=(Map<String,android.widget.EditText>)inputField.get(activity);
                PanoramaService service=(PanoramaService)serviceField.get(activity);
                if(coldStart) {
                    for(String key:new String[]{"startPan","startTilt","endPan","endTilt"})
                        check(fields.get(key).getText().toString().equals(contextValue(activity,key)),"Frischer Start erhält gespeicherten Endpunkt "+key);
                    fields.get("startPan").setText("-40"); fields.get("startTilt").setText("10");
                    fields.get("endPan").setText("-30"); fields.get("endTilt").setText("5");
                    calculate.invoke(activity);
                    check(planField.get(activity)!=null,"Neuer Entwurf berechenbar");
                    remove.invoke(activity);
                } else {
                    check(fields.get("endPan").getText().toString().equals("-30"),"Entwurf bei Activity-Neuerstellung erhalten");
                    remove.invoke(activity);
                }
                for(String key:new String[]{"startPan","startTilt","endPan","endTilt"})
                    check(fields.get(key).getText().length()==0,"Plan entfernen leert "+key);
                check(planField.get(activity)==null && service.draftEndpoints.values().stream().allMatch(String::isEmpty),"Entfernen verwirft Rezept und Entwurf");
                if(coldStart) {
                    fields.get("startPan").setText("-40"); fields.get("startTilt").setText("10");
                    fields.get("endPan").setText("-30"); fields.get("endTilt").setText("5");
                    calculate.invoke(activity);
                }
            } catch(Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private android.app.Activity launchActivity(Context context) throws Exception {
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        try {
            mainCall(() -> context.startActivity(new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)));
            android.app.Activity activity=monitor.waitForActivityWithTimeout(5000);
            if(activity==null) throw new AssertionError("Activity-Start nach 5 s nicht abgeschlossen");
            return activity;
        } finally { removeMonitor(monitor); }
    }
    /** Explicit hardware opt-in: loads a stationary recipe, never starts it. */
    private void abortPlanChecks(Context context) throws Exception {
        android.app.Activity activity=launchActivity(context);
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("service"); field.setAccessible(true);
        java.util.concurrent.atomic.AtomicReference<PanoramaService> reference=new java.util.concurrent.atomic.AtomicReference<>();
        awaitUi(() -> { try { reference.set((PanoramaService)field.get(activity)); return reference.get()!=null; } catch(Exception ex) { throw new RuntimeException(ex); } });
        PanoramaService service=reference.get();
        mainCall(() -> {
            android.bluetooth.BluetoothAdapter adapter=context.getSystemService(android.bluetooth.BluetoothManager.class).getAdapter();
            android.bluetooth.BluetoothDevice target=null;
            for(android.bluetooth.BluetoothDevice device:adapter.getBondedDevices()) if("RNBT-18E4".equals(device.getName())) target=device;
            if(target==null) throw new AssertionError("Gekoppeltes RNBT-18E4 fehlt");
            context.startForegroundService(new Intent(context,PanoramaService.class).putExtra("address",target.getAddress()));
        });
        awaitUi(() -> service.fresh() && !service.busy);
        long run=System.currentTimeMillis()%2000000000+1;
        mainCall(() -> {
            check(service.state.state.equals("IDLE"),"Hardwaretest beginnt nur ohne bestehenden Auftrag");
            service.loadPlan(PanoramaPlan.create(run,false,-service.state.pan/1000.0,service.state.tilt/1000.0,-service.state.pan/1000.0,service.state.tilt/1000.0,5,5,0,10,30,50,500,false));
        });
        awaitUi(() -> service.fresh() && !service.busy && service.state.state.equals("PLAN_READY") && service.state.runId==run);
        mainCall(() -> { check(!service.manualAllowed(),"Geladener Plan sperrt manuelle Fahrt"); service.abortPlan(); });
        awaitUi(() -> service.fresh() && !service.busy && !service.abortingPlan && service.state.state.equals("IDLE"));
        mainCall(() -> {
            check(service.manualAllowed(),"Nach Abbruch wieder manuelle Freigabe");
            check(service.state.completed==0,"Abbruchtest ohne Aufnahme");
        });
    }
    private void awaitUi(java.util.function.BooleanSupplier predicate) throws Exception {
        for(int attempt=0;attempt<200;attempt++) {
            java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
            mainCall(() -> ready.set(predicate.getAsBoolean()));
            if(ready.get()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Erwarteter Gerätestand nach 20 s nicht erreicht");
    }
    @SuppressWarnings("unchecked")
    private android.app.Activity landscapeChecks(android.app.Activity activity) throws Exception {
        java.lang.reflect.Field previewField=MainActivity.class.getDeclaredField("preview"); previewField.setAccessible(true);
        java.lang.reflect.Field inputField=MainActivity.class.getDeclaredField("inputs"); inputField.setAccessible(true);
        java.lang.reflect.Field fineField=MainActivity.class.getDeclaredField("navigationBar"); fineField.setAccessible(true);
        PlanPreview before=(PlanPreview)previewField.get(activity);
        Map<String,android.widget.EditText> inputs=(Map<String,android.widget.EditText>)inputField.get(activity);
        String start=inputs.get("startPan").getText().toString(),end=inputs.get("endTilt").getText().toString();
        double left=before.viewport.left,top=before.viewport.top;
        int original=activity.getRequestedOrientation();
        android.app.Activity rotated=rotate(activity,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        PlanPreview after=(PlanPreview)previewField.get(rotated);
        android.view.View fine=(android.view.View)fineField.get(rotated);
        for(int attempt=0;attempt<30 && after.getWidth()==0;attempt++) Thread.sleep(100);
        mainCall(() -> {
            int[] windowPosition=new int[2],controlPosition=new int[2];
            rotated.getWindow().getDecorView().getLocationOnScreen(windowPosition); fine.getLocationOnScreen(controlPosition);
            android.graphics.Rect visible=new android.graphics.Rect();
            check(fine.getLocalVisibleRect(visible) && fine.getHeight()>0 && controlPosition[1]>=windowPosition[1] && controlPosition[1]+fine.getHeight()<=windowPosition[1]+rotated.getWindow().getDecorView().getHeight(),"Pfeilsteuerung im Querformat sichtbar am unteren Bildschirmrand");
            check(after.getWidth()>=rotated.getWindow().getDecorView().getWidth()*.85,"Pan-Grafik nutzt nahezu gesamte Displaybreite");
            check(after.getWidth()>after.getHeight() && after.getHeight()>0,"Querformatgrafik nutzt breite Ansicht");
            check(after.getHeight()>=after.alignmentHeight(),"Tilt-Zeichenfläche mindestens 25 mm hoch, unabhängig von Scrollhöhe");
        });
        Map<String,android.widget.EditText> restored=(Map<String,android.widget.EditText>)inputField.get(rotated);
        check(restored.get("startPan").getText().toString().equals(start) && restored.get("endTilt").getText().toString().equals(end),"Panorama-Endpunkte nach Drehung erhalten");
        check(after.viewport.left==left && after.viewport.top==top,"Grafikausschnitt nach Drehung erhalten");
        return rotate(rotated,original);
    }
    private android.app.Activity rotate(android.app.Activity activity,int orientation) throws Exception {
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
        try {
            mainCall(() -> activity.setRequestedOrientation(orientation));
            android.app.Activity replacement=monitor.waitForActivityWithTimeout(500);
            if(replacement==null) {
                int actual=activity.getResources().getConfiguration().orientation;
                boolean alreadyMatches=orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    || (orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && actual==android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                    || (orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && actual==android.content.res.Configuration.ORIENTATION_PORTRAIT);
                if(alreadyMatches) return activity;
                replacement=monitor.waitForActivityWithTimeout(4500);
            }
            if(replacement==null) throw new AssertionError("Orientierungswechsel nach 5 s nicht abgeschlossen");
            for(int attempt=0;attempt<30 && !replacement.hasWindowFocus();attempt++) Thread.sleep(100);
            return replacement;
        } finally { removeMonitor(monitor); }
    }
    /** Wait for our own operation, not for an idle UI receiving continuous telemetry. */
    private void mainCall(Runnable action) throws Exception {
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try { action.run(); } catch(Throwable ex) { error.set(ex); } finally { done.countDown(); }
        });
        if(!done.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("UI-Operation nach 5 s nicht abgeschlossen");
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void manualFeedbackChecks() throws Exception {
        PanoramaService service = new PanoramaService();
        service.message = "Bestätigt: Fahrziel angenommen";
        service.commandFeedback("Geräteabfrage wird gesendet …",true,true);
        service.commandFeedback("Bestätigt: Geräteabfrage angenommen",true,true);
        check(service.message.equals("Bestätigt: Fahrziel angenommen"),"Hintergrundabfrage verändert Fahrmeldung nicht");
        java.lang.reflect.Field tracker=PanoramaService.class.getDeclaredField("manualProgress"); tracker.setAccessible(true);
        tracker.set(service,new ManualMoveProgress(40,22028,264)); service.manualMoving=true;
        service.acceptManualEvent(new String[]{"41","3841000","13","0","0","IDLE","NONE","22000","200","21000","200","1000","0","0"});
        service.acceptManualEvent(new String[]{"42","3841766","14","0","0","IDLE","NONE","22000","200","22700","200","-700","0","0"});
        check(!service.manualMoving && service.message.equals("Manuelles Ziel erreicht"),"Firmware-Abschluss mit null stabilen Samples erkannt");
        service.commandFeedback("Bestätigt: Geräteabfrage angenommen",true,true);
        check(service.message.equals("Manuelles Ziel erreicht"),"Späte Abfragebestätigung überschreibt Abschluss nicht");
        service.commandFeedback("Keine eindeutige Bestätigung",true,false);
        check(service.message.equals("Keine eindeutige Bestätigung"),"Hintergrundfehler bleibt sichtbar");
    }
    @SuppressWarnings("unchecked")
    private void graphicalChecks(android.app.Activity activity) throws Exception {
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("preview"); field.setAccessible(true);
        PlanPreview view=(PlanPreview)field.get(activity);
        java.lang.reflect.Field inputField=MainActivity.class.getDeclaredField("inputs"); inputField.setAccessible(true);
        Map<String,android.widget.EditText> inputs=(Map<String,android.widget.EditText>)inputField.get(activity);
        String oldPan=inputs.get("startPan").getText().toString(),oldTilt=inputs.get("startTilt").getText().toString();
        java.lang.reflect.Method select=MainActivity.class.getDeclaredMethod("selectPage",int.class); select.setAccessible(true);
        mainCall(() -> { try { select.invoke(activity,2); } catch(Exception ex) { throw new RuntimeException(ex); } });
        for(int attempt=0;attempt<30 && view.getWidth()==0;attempt++) Thread.sleep(100);
        check(view.getWidth()>0 && view.getHeight()>0,"Diagramm vor Touch gemessen");
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        mainCall(() -> {
            try {
                PlanPreview ranges=new PlanPreview(activity);
                DeviceState limits=new DeviceState(); limits.limitsKnown=true;
                limits.panMin=-180000; limits.panMax=180000; limits.tiltMin=-45000; limits.tiltMax=50000;
                ranges.setLive(limits,new RunPlotModel(),true);
                ranges.alignmentRange(25);
                check(ranges.viewport.bottom==-25 && ranges.viewport.top==25 && ranges.viewport.left==-180 && ranges.viewport.right==180,"Tilt ±25° bei vollem Pan-Bereich");
                ranges.alignmentRange(45);
                check(ranges.viewport.bottom==-45 && ranges.viewport.top==45,"Tilt ±45° auswählbar");
                ranges.alignmentRange(0);
                check(ranges.viewport.bottom==-45 && ranges.viewport.top==50,"Gesamter Tilt-Bereich folgt tatsächlichen Gerätegrenzen, positive Werte nach oben");
                check(limits.tiltMin==-45000 && limits.tiltMax==50000,"Grafikstufen ändern keine Fahrgrenzen");
                view.setEditMode(1); view.viewport.fit(-54,-50,-7,-1);
                float d=activity.getResources().getDisplayMetrics().density;
                float x=55*d+(view.getWidth()-69*d)*.25f,y=28*d+(view.getHeight()-76*d)*.25f;
                double expectedPan=Math.rint(view.viewport.pan(.25)*1000)/1000, expectedTilt=Math.rint(view.viewport.tilt(.25)*1000)/1000;
                long time=android.os.SystemClock.uptimeMillis();
                android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,0,x,y,0),up=android.view.MotionEvent.obtain(time,time+20,1,x,y,0);
                view.dispatchTouchEvent(down); view.dispatchTouchEvent(up); down.recycle(); up.recycle();
                check(inputs.get("startPan").getText().toString().equals(oldPan) && inputs.get("startTilt").getText().toString().equals(oldTilt),"Touch verändert ohne Übernahme keinen Startpunkt");
                check(view.selection.pending && Math.abs(view.selection.tilt-expectedTilt)<.002,"Beide Achsen frei als Vorschlag wählbar");
                view.commitSelection();
                check(Math.abs(Double.parseDouble(inputs.get("startPan").getText().toString())-expectedPan)<.002,"Touch schreibt Pan in echte Eingabe: Ist="+inputs.get("startPan").getText()+", Soll="+expectedPan+", Breite="+view.getWidth());
                check(Math.abs(Double.parseDouble(inputs.get("startTilt").getText().toString())-expectedTilt)<.002,"Touch schreibt Tilt mit richtigem Vorzeichen");
                android.os.Bundle saved=new android.os.Bundle(); view.zoom(.5); view.save(saved,"view");
                PlanPreview restored=new PlanPreview(activity); restored.restore(saved,"view");
                check(restored.viewport.left==view.viewport.left && restored.viewport.top==view.viewport.top,"Zoom und Ausschnitt wiederherstellbar");
                view.setEditMode(3);
                down=android.view.MotionEvent.obtain(time,time,0,x,y,0); up=android.view.MotionEvent.obtain(time,time+20,1,x,y,0);
                String committed=inputs.get("startPan").getText().toString();
                view.dispatchTouchEvent(down); view.dispatchTouchEvent(up); down.recycle(); up.recycle();
                view.commitSelection();
                check(inputs.get("startPan").getText().toString().equals(committed),"Fahrpunkt-Vorschlag verändert keine Panorama-Endpunkte");
            } catch(Throwable ex) { failure.set(ex); }
            finally { inputs.get("startPan").setText(oldPan); inputs.get("startTilt").setText(oldTilt); view.setEditMode(0); view.fit(); }
        });
        if(failure.get()!=null) throw new AssertionError(failure.get());
    }
    private void navigationChecks(android.app.Activity activity) throws Exception {
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("preview"); field.setAccessible(true);
        java.lang.reflect.Field sf=MainActivity.class.getDeclaredField("service"); sf.setAccessible(true);
        java.lang.reflect.Method select=MainActivity.class.getDeclaredMethod("selectPage",int.class); select.setAccessible(true);
        java.lang.reflect.Method render=MainActivity.class.getDeclaredMethod("render"); render.setAccessible(true);
        PlanPreview preview=(PlanPreview)field.get(activity);
        mainCall(() -> {
            try {
                PanoramaService service=(PanoramaService)sf.get(activity);
                check(!service.hasConnection(),"UI-Navigationstest ohne Hardwareverbindung");
                DeviceState original=service.state;
                DeviceState simulated=new DeviceState(); simulated.compatible=simulated.limitsKnown=simulated.confirmed=true;
                simulated.inactiveNavigation=true; simulated.panMin=-180000; simulated.panMax=180000;
                simulated.tiltMin=-45000; simulated.tiltMax=50000; simulated.runId=42; simulated.completed=7;
                service.state=simulated; service.connected=true;
                try {
                    android.widget.LinearLayout bar=activity.findViewById(android.R.id.content).findViewWithTag("navigationBar");
                    for(int page=0;page<5;page++) {
                        simulated.state="IDLE"; simulated.lastStatusMs=android.os.SystemClock.elapsedRealtime();
                        select.invoke(activity,page);
                        check((bar.getVisibility()==android.view.View.VISIBLE)==(page==1 || page==2),"Pfeilleiste nur Plan/Manuell: Seite "+page);
                    }
                    select.invoke(activity,2);
                    for(String state:new String[]{"IDLE","PAUSED","PLAN_READY","STOPPED","COMPLETED","RUNNING","PAUSE_PENDING","TIMELAPSE"}) {
                        simulated.state=state; simulated.lastStatusMs=android.os.SystemClock.elapsedRealtime(); render.invoke(activity);
                        boolean allowed=!(state.equals("RUNNING") || state.equals("PAUSE_PENDING") || state.equals("TIMELAPSE"));
                        for(int i=1;i<bar.getChildCount();i++) check(bar.getChildAt(i).isEnabled(),"Pfeil erklärt Einschränkung bei "+state);
                        check(service.navigationAllowed()==allowed,"Gerätefreigabe getrennt von erklärbarer Taste bei "+state);
                        if(!allowed) {
                            try { service.move(0,1); throw new AssertionError("Aufnahme muss Pfeilbefehl ablehnen"); }
                            catch(IllegalArgumentException expected) { check(simulated.state.equals(state) && simulated.runId==42 && simulated.completed==7,"Pfeilversuch verändert keinen Lauf: "+state); }
                        }
                    }
                    simulated.state="IDLE"; simulated.lastStatusMs=android.os.SystemClock.elapsedRealtime(); render.invoke(activity);
                    preview.setEditMode(3); preview.alignmentRange(25);
                    float d=activity.getResources().getDisplayMetrics().density;
                    float x=55*d+(preview.getWidth()-69*d)*.7f,y=28*d+(preview.getHeight()-76*d)*.3f;
                    tap(preview,x,y);
                    android.view.View actions=preview.findViewWithTag("selectionActions");
                    android.widget.Button drive=preview.findViewWithTag("driveSelection");
                    check(preview.selection.pending && actions.getVisibility()==android.view.View.VISIBLE,"Tippen zeigt Anfahren direkt am Fahrpunkt");
                    check(Math.abs(preview.selection.tilt)>5,"Direkt-Anfahren auch bei grossem Tiltziel pruefen");
                    int[] drives={0}; preview.setDriveAction(() -> drives[0]++); preview.setDriveReady(true); drive.performClick();
                    check(drives[0]==1,"Ein zweiter Tipp startet genau eine Aktion ohne weiteren Dialog");
                    check(simulated.runId==42 && simulated.completed==7,"Punktwahl und Aktion loeschen keinen Lauf");
                    ((android.widget.Button)preview.findViewWithTag("discardSelection")).performClick();
                    check(!preview.selection.pending && actions.getVisibility()==android.view.View.GONE,"Verwerfen schliesst Fahrpunktaktionen");
                    long time=android.os.SystemClock.uptimeMillis();
                    android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,0,x,y,0);
                    android.view.MotionEvent move=android.view.MotionEvent.obtain(time,time+30,2,x+80*d,y,0);
                    android.view.MotionEvent up=android.view.MotionEvent.obtain(time,time+60,1,x+80*d,y,0);
                    preview.dispatchTouchEvent(down); preview.dispatchTouchEvent(move); preview.dispatchTouchEvent(up);
                    down.recycle(); move.recycle(); up.recycle();
                    check(!preview.selection.pending && drives[0]==1,"Wischen setzt kein Fahrziel und startet keine Fahrt");
                } finally {
                    service.state=original; service.connected=false; preview.setDriveAction(() -> { }); preview.setEditMode(0); preview.fit(); render.invoke(activity);
                }
            } catch(Exception ex) { throw new RuntimeException(ex); }
        });
    }
    private static void tap(PlanPreview preview,float x,float y) {
        long time=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,0,x,y,0),up=android.view.MotionEvent.obtain(time,time+20,1,x,y,0);
        preview.dispatchTouchEvent(down); preview.dispatchTouchEvent(up); down.recycle(); up.recycle();
    }
    private static String contextValue(android.app.Activity activity,String key) {
        return activity.getSharedPreferences("settings",Context.MODE_PRIVATE).getString(key,"");
    }
    private void storageChecks(Context context,String name) throws Exception {
        DeviceState state = new DeviceState();
        PanoramaPlan plan = PanoramaPlan.create(5,false,0,0,10,-10,5,5,0,10,30,50,500,false);
        try (CaptureStore store = new CaptureStore(context,name)) {
            feed(store,state,"PLAN",plan.fields());
            feed(store,state,"RUN_SEGMENT","5","0","0","0");
            String[] image = {"1","5","1","0","0","0","0","0","0","123","456","0"};
            feed(store,state,"IMAGE_COMPLETED",image); feed(store,state,"IMAGE_COMPLETED",image);
            feed(store,state,"STATUS","5","RUNNING","MOVING","2","9","0","1","0","5000","0","0","0","2","NONE","2","0","1","2");
            List<CaptureStore.Run> runs=store.runs(); check(runs.size()==1,"Ein Lauf");
            String csv=store.export(runs.get(0).key,false), xml=store.export(runs.get(0).key,true);
            check(csv.split("\n").length==3,"Eine gemessene und eine rekonstruierte Aufnahme");
            check(csv.contains("RECONSTRUCTED_RASTER_TARGET"),"Rekonstruktion markiert");
            CaptureStore.PlotSnapshot plot=store.plotSnapshot(runs.get(0).key);
            check(plot.model.captures.size()==2 && plot.plan.runId==5,"Grafik liest gespeicherten Lauf ohne Zusatzaufnahme");
            check(!plot.model.captures.get(0).reconstructed && plot.model.captures.get(1).reconstructed,"Grafik unterscheidet gemessen und rekonstruiert");
            check(csv.contains("-0.123;0.456"),"Istposition in UI-Koordinaten, Tilt positiv nach oben");
            check(count(xml,"<pict ")==2,"XML ohne Duplikate");
            javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            feed(store,state,"IMAGE_COMPLETED","2","5","2","0","1","0","1","0","5000","10","5010","0");
            csv=store.export(runs.get(0).key,false);
            check(!csv.contains("RECONSTRUCTED_RASTER_TARGET"),"Spätes Ereignis ersetzt Rekonstruktion");
            check(csv.split("\n").length==3,"Ersetzen erzeugt keine Zusatzaufnahme");
            feed(store,state,"MANUAL_IMAGE_TRIGGERED","3","5","1","2","10","5010","12000","0");
            feed(store,state,"MANUAL_IMAGE_TRIGGERED","3","5","1","2","10","5010","12000","1");
            check(store.export(runs.get(0).key,false).split("\n").length==4,"Manuelles Replay dedupliziert");
            check(store.export(runs.get(0).key,false).contains("MEASURED_LIVE"),"Gemessene Daten bleiben erhalten");
        }
        try (CaptureStore reopened = new CaptureStore(context,name)) {
            check(reopened.runs().size()==1,"Aufnahmen nach Datenbank-Neustart erhalten");
        }
    }
    private static void feed(CaptureStore store,DeviceState state,String type,String... fields) throws Exception {
        P2Codec.Frame frame=P2Codec.decode(P2Codec.encode(1,type,fields)); state.accept(frame,1000); store.accept("TEST_DEVICE",state,frame);
    }
    private void captureWarningStorageChecks(Context context,String name) throws Exception {
        // Recreate the precise v1 schema on a temporary test database only.
        try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).getPath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)) {
            db.execSQL("DROP TABLE capture_warnings");db.setVersion(1);
        }
        String key;
        try(CaptureStore store=new CaptureStore(context,name)) {
            key=store.runs().get(0).key;
            check(store.export(key,false).split("\n").length==4,"v1-to-v2 migration retains raster and manual captures");
            DeviceState state=new DeviceState();
            P2Codec.Frame warning=new P2Codec.Frame(10,"CAPTURE_WARNING",new String[]{"5","1","9"});
            check(store.accept("TEST_DEVICE",state,warning),"First warning accepted");
            check(!store.accept("TEST_DEVICE",state,warning),"Repeated warning cannot generate another sound");
            CaptureStore.PlotSnapshot snapshot=store.plotSnapshot(key);
            check(snapshot.model.captures.get(0).warningFlags==9 && snapshot.model.captures.size()==3,"Late warning attaches to exact capture without creating a photograph");
            feed(store,state,"CAPTURE_WARNING","5","3","4");
            feed(store,state,"STATUS","5","RUNNING","MOVING","3","9","0","2","0","10000","0","0","0","2","NONE","3","0","1","3");
            snapshot=store.plotSnapshot(key);
            RunPlotModel.Capture inferred=snapshot.model.captures.stream().filter(c -> c.key.equals("R-5-3")).findFirst().get();
            check(inferred.reconstructed && inferred.warningFlags==4,"Warning before missing image survives raster reconstruction");
            feed(store,state,"IMAGE_COMPLETED","9","5","3","0","2","0","2","0","10000","500","9900","0");
            snapshot=store.plotSnapshot(key);
            RunPlotModel.Capture measured=snapshot.model.captures.stream().filter(c -> c.key.equals("R-5-3")).findFirst().get();
            check(!measured.reconstructed && measured.warningFlags==4 && measured.pan==500,"Late measured image retains its warning and actual coordinates");
            check(store.export(key,false).contains("Pan-Abweichung über Toleranz") && store.export(key,false).contains("Fahrzeitgrenze erreicht"),"CSV retains warning reasons");
            // Force the first warning outside the ordinary 512-image window.
            for(int i=4;i<=520;i++) feed(store,state,"IMAGE_COMPLETED",""+(20+i),"5",""+i,"0","0","0","0","0","0","0","0","0");
            check(store.plotSnapshot(key).model.captures.stream().anyMatch(c -> c.key.equals("R-5-1") && c.warningFlags==9),"Old warning points never fall out of the 512-image view");
        }
        try(CaptureStore store=new CaptureStore(context,name)) {
            check(store.plotSnapshot(key).model.captures.stream().anyMatch(c -> c.key.equals("R-5-3") && c.warningFlags==4),"Warning persists after database restart");
        }
    }
    private void check(boolean condition,String name) { if (!condition) throw new AssertionError(name); assertions++; }
    private static int count(String text,String part) { return text.split(java.util.regex.Pattern.quote(part),-1).length-1; }
}
