package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.bluetooth.*;
import android.content.Intent;
import android.os.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Started foreground service owns Bluetooth independently of Activity recreation. */
public final class PanoramaService extends Service {
    public interface Observer { void changed(); }
    public final class LocalBinder extends Binder { public PanoramaService service() { return PanoramaService.this; } }
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Observer> observers = new ArrayList<>();
    private final ReconnectPolicy reconnect = new ReconnectPolicy();
    private DeviceLink session;
    private BluetoothDevice device;
    private String plotDevice="";
    private int generation, actionGeneration;
    private PowerManager.WakeLock wakeLock;
    public DeviceState state = new DeviceState();
    final RunPlotModel plot = new RunPlotModel();
    public String connection = "Nicht verbunden", message = "Bereit", diagnostics = "Noch keine Diagnose";
    public boolean connected, busy, manualMoving, manualTrigger, uncertain;
    public boolean abortingPlan;
    public boolean preparingRest;
    private RestPreparation restPreparation;
    private PlanAbort planAbort;
    private boolean discardDraftAfterAbort;
    public int draftRevision;
    public final Map<String,String> draftEndpoints=new LinkedHashMap<>();
    public boolean draftNeedsReview;
    private String draftDevice="";
    private DeviceSession deviceSession;
    private boolean sessionIdentityReceived;
    private int targetPan, targetTilt;
    private boolean targetKnown;
    private boolean manualPreparing, preparationAcknowledged;
    private int manualBaseline = -1;
    private Runnable pendingManual;
    private ManualMoveProgress manualProgress;
    private ManualActionProgress manualAction;
    final JogHold jog=new JogHold();
    private final Runnable jogAlive=new Runnable() {
        public void run() {
            if(!jog.held) return;
            if(!connected || session==null || !fresh() || incident.needsAcknowledgement()) { endJog(); return; }
            if(!busy && jog.sequence>0) execute("MANUAL_JOG_KEEPALIVE",new String[]{""+jog.token},false,true);
            ui.postDelayed(this,150);
        }
    };
    private int newestDebugSequence;
    private boolean debugPending;
    private int debugRequestId;
    private long statusVersion, manualDeadlineMs, triggerDeadlineMs;
    private long endpointCaptureAfterMs;
    final IncidentTracker incident=new IncidentTracker();
    public boolean recovering;
    private boolean recoveryRequested;
    private RecoveryFlow recoveryFlow;
    private long nextRecoveryAttemptMs;
    private long nextDiagnosticMs, openedMs;
    private final StatusLogPolicy statusLogPolicy=new StatusLogPolicy();
    private String savedFault="";
    private String savedPause="";
    private CaptureStabilitySettings pendingStability;
    private final CaptureQualityGuard qualityGuard=new CaptureQualityGuard();
    private CaptureQualityGuard.Issue pendingQuality;
    private android.media.ToneGenerator warningTone;
    public CaptureStore captures;
    LivePapywizard livePapywizard;
    private boolean exportServiceActive=true;
    String papywizardStatus() { return livePapywizard==null?"Papywizard-Sicherung wird vorbereitet.":livePapywizard.status(); }
    void rebuildPapywizard() { if(livePapywizard!=null) livePapywizard.rebuild(); }
    public SupportLog support;
    private final Runnable retry = () -> open();
    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            if (connected && session != null) {
                long now = SystemClock.elapsedRealtime();
                if (now - (state.lastStatusMs == 0 ? openedMs : state.lastStatusMs) > 5000) {
                    session.fail("Keine aktuellen Statusdaten seit 5 Sekunden");
                } else if(state.manualStatusSupported) {
                    updateManualAction();
                    if(!state.manualStatusFresh(now)) {
                        long last=state.lastManualStatusMs==0?state.manualStatusExpectedMs:state.lastManualStatusMs;
                        if(now-last>5000) session.fail("Manueller Betriebsstatus fehlt seit 5 Sekunden; Gerätestand prüfen");
                        else if(!busy && now>nextDiagnosticMs) {
                            nextDiagnosticMs=now+1000;
                            execute("GET_STATUS",new String[0],false,true);
                        }
                    }
                } else if (manualMoving && !manualPreparing && !debugPending && !busy && now > nextDiagnosticMs) {
                    nextDiagnosticMs = now + 1000;
                    execute("GET_DEBUG_LOG", new String[]{""+newestDebugSequence}, false, true);
                }
                pauseForQuality();
                if((manualMoving && now>=manualDeadlineMs) || (manualTrigger && now>=triggerDeadlineMs)) {
                    raiseIncident("ACTION_COMPLETION", "Der Abschluss der manuellen Aktion wurde nicht bestätigt. Das alte Ziel wird verworfen.");
                    manualMoving=manualTrigger=targetKnown=false; uncertain=true;
                    emergencyStop();
                }
                if(incident.active && incident.acknowledged && uncertain && !recovering && !busy && fresh() && now>=nextRecoveryAttemptMs)
                    recoverDevice();
            }
            notifyObservers(); ui.postDelayed(this, 500);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        captures = new CaptureStore(this); support = new SupportLog(getFilesDir());
        support.append("APP_START version="+BuildConfig.VERSION_NAME);
        CaptureStore exportReader=new CaptureStore(this);
        java.io.File[] media=getExternalMediaDirs();
        java.io.File base=media.length>0 && media[0]!=null?media[0]:getFilesDir();
        livePapywizard=new LivePapywizard(new LivePapywizard.Source() {
            public List<String> roots() { return exportReader.liveRoots(); }
            public LivePapywizard.Snapshot snapshot(String key) { return exportReader.liveSnapshot(key); }
            public void close() { exportReader.close(); }
        },new java.io.File(base,"Papywizard"),status -> {
            // SupportLog and observers belong to the main thread, not the file writer.
            if(status.startsWith("Papywizard-Sicherung fehlgeschlagen")) ui.post(() -> { if(exportServiceActive) support.append(status); });
        });
        captures.onExportChanged(livePapywizard::request);
        livePapywizard.rebuild();
        android.content.SharedPreferences settings=getSharedPreferences("settings",MODE_PRIVATE);
        for(String key:new String[]{"startPan","startTilt","endPan","endTilt"}) draftEndpoints.put(key,settings.getString(key,""));
        draftDevice=settings.getString("draftDevice",settings.getString("device",""));
        draftNeedsReview=settings.getBoolean("draftNeedsReview",false);
        android.content.SharedPreferences faults=getSharedPreferences("incidents",MODE_PRIVATE);
        incident.id=faults.getLong("id",0); incident.key=faults.getString("key","");
        incident.detail=faults.getString("detail",""); incident.active=faults.getBoolean("active",false);
        incident.acknowledged=faults.getBoolean("acknowledged",true);
        incident.requiresStop=faults.getBoolean("requiresStop",true);
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("connection", "Panoramagerät", NotificationManager.IMPORTANCE_LOW));
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Panorama:Bluetooth");
        ui.post(monitor);
    }
    @Override public IBinder onBind(Intent intent) { return new LocalBinder(); }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if ("DISCONNECT".equals(intent.getAction())) { disconnect(); return START_NOT_STICKY; }
        String address = intent.getStringExtra("address");
        if (address != null && device == null) {
            try {
                if(!address.equals(plotDevice)) { plot.runId=-1; plot.trace.clear(); plot.captures=Collections.emptyList(); plot.disconnected(); plotDevice=address; }
                device = getSystemService(BluetoothManager.class).getAdapter().getRemoteDevice(address);
                getSharedPreferences("settings", MODE_PRIVATE).edit().putString("device", address).apply();
                startForeground(1, notification());
                wakeLock.acquire(); open();
            } catch (RuntimeException ex) { disconnect(); message = "Bluetooth nicht verfügbar oder Berechtigung fehlt"; notifyObservers(); }
        }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent show = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, PanoramaService.class).setAction("DISCONNECT"), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "connection").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("PanoramaControl").setContentText(connection).setContentIntent(show)
            .setOngoing(true).addAction(new Notification.Action.Builder(null, "Trennen", stop).build()).build();
    }
    public void observe(Observer observer) { observers.add(observer); observer.changed(); }
    public void removeObserver(Observer observer) { observers.remove(observer); }
    private void notifyObservers() { for (Observer observer : new ArrayList<>(observers)) observer.changed(); }
    private void open() {
        if (device == null || session != null) return;
        cancelRest();savedFault="";
        jog.finish(jog.token); ui.removeCallbacks(jogAlive);
        if(wakeLock!=null && !wakeLock.isHeld()) wakeLock.acquire();
        int id = ++generation; ++actionGeneration;
        String address=device.getAddress();
        if(!draftDevice.isEmpty() && !draftDevice.equals(address)) markDraftForReview();
        draftDevice=address;
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("draftDevice",address).apply();
        deviceSession=new DeviceSession(getSharedPreferences("settings",MODE_PRIVATE).getLong("session."+address,-1));
        sessionIdentityReceived=false;
        state = new DeviceState(); connected = busy = manualMoving = manualTrigger = targetKnown = uncertain = false;
        cancelPreparation(); manualProgress = null; manualAction=null;
        debugPending = false; newestDebugSequence = 0;
        connection = "Bluetooth-Verbindung wird aufgebaut …";
        BluetoothSession opening = new BluetoothSession(new BluetoothSession.Listener() {
            @Override public void onAwaitingProtocol() {
                if (id != generation) return; connection = "Bluetooth verbunden · Warte auf V2 …"; notifyObservers();
            }
            @Override public void onConnected() {
                if (id != generation) return;
                connected = true; openedMs = SystemClock.elapsedRealtime(); reconnect.connected();
                connection = "Bluetooth verbunden · Protokoll V2";
                getSystemService(NotificationManager.class).notify(1, notification()); notifyObservers();
            }
            @Override public void onFrame(P2Codec.Frame frame) {
                if (id != generation) return;
                try {
                    String previousState = state.state;
                    long previousRun=state.runId, previousCount=state.completed;
                    state.accept(frame, SystemClock.elapsedRealtime());
                    if(frame.type.equals("TILT_CALIBRATION")) {
                        android.content.SharedPreferences settings=getSharedPreferences("settings",MODE_PRIVATE);
                        String key="tiltZero."+device.getAddress();
                        if(settings.contains(key) && settings.getInt(key,0)!=state.tiltZeroOffset) {
                            markDraftForReview(); targetKnown=false; state.lastStatusMs=0;
                            message="Tilt-Nullpunkt geändert · Entwurfreferenz prüfen";
                        }
                        settings.edit().putInt(key,state.tiltZeroOffset).apply();
                    }
                    if(frame.type.equals("DEVICE_SESSION")) {
                        if(frame.fields.length!=1) throw new IllegalArgumentException("Gerätesitzung unvollständig");
                        long boot=DeviceState.unsigned(frame.fields[0]);
                        boolean reset=deviceSession.accept(boot);
                        getSharedPreferences("settings",MODE_PRIVATE).edit().putLong("session."+device.getAddress(),boot).apply();
                        if(reset) {
                            cancelRest();
                            message="Gerät neu gestartet · Entwurf erhalten, Gerätestand prüfen";
                            raiseIncident("DEVICE_RESTART","Das Gerät wurde neu gestartet. Alte Aktionen werden verworfen; der Entwurf bleibt erhalten.",false);
                            state.lastStatusMs=0; state.plan=null; targetKnown=false;
                            cancelPreparation(); manualProgress=null; manualAction=null; manualMoving=manualTrigger=false;
                            planAbort=null; abortingPlan=discardDraftAfterAbort=false;
                            if(sessionIdentityReceived) {
                                ++actionGeneration; busy=false; uncertain=false;
                                session.fail("Gerät neu gestartet · Gerätestand prüfen");
                                notifyObservers(); return;
                            }
                        }
                        sessionIdentityReceived=true;
                    }
                    if (frame.type.equals("STATUS")) { statusVersion++; if(!previousState.equals(state.state)) targetKnown=false; }
                    if(frame.type.equals("CAPTURE_STABILITY_SETTINGS")) observeStabilitySettings();
                    boolean newWarning=captures.accept(device.getAddress(), state, frame);
                    if(frame.type.equals("IMAGE_COMPLETED") && fresh() && frame.fields.length==12 && state.plan!=null && state.plan.mode==0 &&
                        state.runId==Long.parseLong(frame.fields[1]) && (state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")) &&
                        getSharedPreferences("settings",MODE_PRIVATE).getBoolean("qualityPause",true)) {
                        CaptureQualityGuard.Issue issue=qualityGuard.accept(frame.fields);
                        if(issue!=null && pendingQuality==null) { pendingQuality=issue;support.append("QUALITY_DEVIATION "+issue.describe()); }
                    }
                    if(newWarning) {
                        CaptureWarning warning=CaptureWarning.parse(frame.fields);
                        message="Warnung bei Aufnahme "+warning.ordinal+": "+CaptureWarning.describe(warning.flags)+" · Lauf wird fortgesetzt";
                        signalQualityWarning();
                    }
                    if(frame.type.equals("STATUS")) plot.status(frame,SystemClock.elapsedRealtime());
                    if(frame.type.equals("IMAGE_COMPLETED") || frame.type.equals("MANUAL_IMAGE_TRIGGERED") || frame.type.equals("CAPTURE_WARNING") ||
                        (frame.type.equals("STATUS") && (previousRun!=state.runId || previousCount!=state.completed || !previousState.equals(state.state))))
                        plot.captures=captures.plotCaptures(device.getAddress(),state.runId);
                    if (frame.type.equals("STATUS") && statusLogPolicy.log(frame.fields,SystemClock.elapsedRealtime())) {
                        support.append("STATUS " + String.join("|",frame.fields));
                    }
                    if (!frame.type.equals("STATUS")) support.append("RX " + P2Codec.encode(Math.max(1, frame.sequence), frame.type, frame.fields).trim());
                    if (frame.type.equals("STATUS") && !targetKnown) { targetPan = state.pan; targetTilt = state.tilt; targetKnown = true; }
                    if (frame.type.equals("STATUS") && state.state.equals("FAULT")) {
                        recordRunFault(frame.fields);
                        cancelPreparation(); manualProgress=null; manualAction=null; manualMoving=manualTrigger=false;
                        raiseIncident(state.lastError,faultDescription(state.lastError));
                        message="Gerätefehler: "+faultDescription(state.lastError);
                    }
                    if(frame.type.equals("STATUS") && state.state.equals("STOPPED") && RasterRecovery.recoverable(state.lastError))
                        recordRunFault(frame.fields);
                    if(frame.type.equals("STATUS")) observeRecoveryPause(frame.fields);
                    if(frame.type.equals("STATUS") && (state.lastError.equals("STEP_TARGET_MISSED") || state.lastError.equals("STEP_MOVE_TIMEOUT")))
                        message="Aufnahmepunkt innerhalb der Fahrgrenze nicht genau erreicht · Beruhigung und Aufnahme an der Istposition; anschließend weiter.";
                    if (frame.type.equals("MANUAL_IMAGE_TRIGGERED") && !state.manualStatusSupported) { manualTrigger = false; message = "Manuelle Aufnahme abgeschlossen"; }
                    if (frame.type.equals("MANUAL_STATUS")) updateManualAction();
                    if (frame.type.equals("DIAGNOSTICS")) readDiagnostics(frame.fields);
                    if (frame.type.equals("DEBUG_LOG_END")) {
                        newestDebugSequence = Integer.parseInt(frame.fields[0]); debugPending = false;
                        if (manualPreparing) { manualBaseline = newestDebugSequence; finishPreparation(); }
                    }
                    if (frame.type.equals("DEBUG_EVENT")) acceptManualEvent(frame.fields);
                    if(recoveryRequested && !recovering && fresh()) beginRecovery();
                } catch (IllegalArgumentException | IOException ex) { message = "Daten-/Speicherfehler: " + ex.getMessage(); }
                notifyObservers();
            }
            @Override public void onDisconnected(String reason) {
                if (id != generation) return;
                cancelRest();
                pendingStability=null;savedPause="";
                pendingTiming=null;
                jog.finish(jog.token); ui.removeCallbacks(jogAlive);
                if(connected && !incident.active) raiseIncident("CONNECTION",reason+". Der Gerätestand wird nach Wiederverbindung geprüft; alte Fahrten werden nicht wiederholt.",manualMoving || manualTrigger || uncertain);
                if(recovering) recoveryRequested=true;
                recoveryFlow=null; recovering=false;
                plot.disconnected();
                session = null; connected = busy = false; ++generation; ++actionGeneration;
                manualMoving = manualTrigger = targetKnown = false;
                cancelPreparation(); manualProgress = null; manualAction=null;
                debugPending = false;
                state = new DeviceState();
                int attempt = reconnect.nextAttempt();
                if (attempt > 0) { connection = reason + " · Neuer Versuch " + attempt + "/3 in 3 s"; ui.postDelayed(retry, 3000); }
                else {
                    connection=reason+" · Warte auf Gerät, neuer Versuch in 30 s · Trennen beendet die Suche";
                    ui.postDelayed(retry,30000);
                }
                if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release();
                notifyObservers();
            }
        });
        session=opening; opening.connect(device); notifyObservers();
    }
    private void readDiagnostics(String[] f) {
        if (f.length < 21) throw new IllegalArgumentException("Diagnose unvollständig");
        int stable = Integer.parseInt(f[5]), required = Integer.parseInt(f[6]);
        diagnostics = "Pan-Fehler: " + f[11] + " m° · Tilt-Fehler: " + f[12] + " m°\n" +
            (state.profiledMotion?"Ruhebestätigung: ":"Stabile Samples: ")
            + stable + "/" + required + " · RAM: " + f[19] + " / Minimum " + f[20];
    }
    private long currentBoot() {
        return device==null?-1:getSharedPreferences("settings",MODE_PRIVATE).getLong("session."+device.getAddress(),-1);
    }
    private void pauseForQuality() {
        CaptureQualityGuard.Issue issue=pendingQuality;
        if(issue==null || busy || uncertain || recovering || recoveryRequested || preparingRest || abortingPlan || !fresh() || incident.needsAcknowledgement()) return;
        if(issue.run!=state.runId || state.state.equals("FAULT") || state.state.equals("STOPPED") || state.state.equals("COMPLETED")) { pendingQuality=null;return; }
        if(state.state.equals("PAUSED")) {
            pendingQuality=null;raiseIncident("QUALITY_DEVIATION",issue.describe()+"Auftrag ist pausiert. Eine Spalte zurückgehen und bewusst fortsetzen.",false);return;
        }
        if(!state.state.equals("RUNNING")) return;
        pendingQuality=null;int id=generation;
        execute("PAUSE",new String[]{""+issue.run},false,true).whenComplete((reply,error) -> {
            if(generation!=id || state.runId!=issue.run) return;
            if(state.state.equals("FAULT") || incident.needsAcknowledgement()) { support.append("QUALITY_PAUSE superseded by device incident");return; }
            String detail=issue.describe()+(error==null && reply.accepted ?
                "Pause nach dem aktuellen Bild angefordert. Danach eine Spalte zurückgehen und bewusst fortsetzen; die betroffene Aufnahme prüfen.":
                "Pause nicht bestätigt. Aktuellen Gerätestand prüfen und gegebenenfalls stoppen.");
            support.append("QUALITY_PAUSE "+detail);raiseIncident("QUALITY_DEVIATION",detail,false);notifyObservers();
        });
    }
    private void recordRunFault(String[] fields) {
        String identity=state.runId+"|"+state.nextImageIndex+"|"+state.revision+"|"+state.completed+"|"+state.lastError;
        if(identity.equals(savedFault)) return;
        if(captures.recordFault(device.getAddress(),currentBoot(),state,fields)) {
            savedFault=identity;
            support.append("FAULT_CONTEXT boot="+currentBoot()+" status="+String.join("|",fields));
            int id=generation;long run=state.runId;
            ui.post(() -> {
                if(id==generation && run==state.runId && (state.state.equals("FAULT") || RunRecoveryPause.active(state)) && !busy && !recovering && !preparingRest)
                    execute("GET_DIAGNOSTICS",new String[0],false,true);
            });
        }
    }
    void observeRecoveryPause(String[] fields) {
        if(!RunRecoveryPause.active(state)) {savedPause="";return;}
        recordRunFault(fields);
        String key=state.runId+"|"+state.nextImageIndex+"|"+state.revision+"|"+state.completed+"|"+state.lastError;
        if(key.equals(savedPause)) return;
        savedPause=key;pendingQuality=null;
        raiseIncident("RUN_PAUSE_"+state.lastError,RunRecoveryPause.detail(state),false);
        message="Aufnahme pausiert · Auftrag erhalten · Ursache: "+state.lastError;
        support.append("RUN_RECOVERY_PAUSE "+key);
    }
    public boolean canPrepareRest() {
        if(device==null || !sessionIdentityReceived || !state.tiltCalibrationKnown || !fresh() || !(state.state.equals("STOPPED") || state.state.equals("IDLE") || state.state.equals("PAUSED")) || !state.positionSafe() ||
            busy || uncertain || recovering || recoveryRequested || preparingRest || abortingPlan || manualMoving || manualTrigger ||
            incident.needsAcknowledgement() || draftNeedsReview) return false;
        CaptureStore.Checkpoint checkpoint=recoveryCheckpoint();
        return navigationReason().isEmpty() && checkpoint!=null && checkpoint.boot>0 && checkpoint.boot==currentBoot() && checkpoint.tiltZero==state.tiltZeroOffset &&
            (state.plan==null || checkpoint.raster.source.sameRecipe(state.plan));
    }
    CaptureStore.Checkpoint restCheckpoint() {
        if(!canPrepareRest()) throw new IllegalArgumentException("Zuerst Fehler quittieren und unveränderten Gerätestand bestätigen");
        return recoveryCheckpoint();
    }
    private CaptureStore.Checkpoint recoveryCheckpoint() {
        if(device==null) return null;
        return state.state.equals("IDLE")?captures.pendingCheckpoint(device.getAddress()):captures.checkpoint(device.getAddress(),state.runId);
    }
    boolean hasRecoveryCheckpoint() {
        return (state.state.equals("FAULT") || state.state.equals("STOPPED") || state.state.equals("PAUSED") || state.state.equals("IDLE")) && recoveryCheckpoint()!=null;
    }
    boolean recoveryInProgress() { return recovering || recoveryRequested; }
    String recoveryGuidance() {
        CaptureStore.Checkpoint checkpoint=recoveryCheckpoint();
        if(checkpoint==null) return "";
        String retained="Aufnahmen und Originalauftrag bleiben erhalten. ";
        if(incident.needsAcknowledgement()) return retained+"Zuerst Fehler quittieren und Gerät prüfen. Es wird keine Fahrt gestartet.";
        if(recovering || recoveryRequested) return retained+"Gerätestand wird geprüft. Erst bei wieder gültiger Rückmeldung wird die Wiederaufnahme freigegeben.";
        if(!fresh()) return retained+"Aktuelle Geräteverbindung abwarten, anschließend Gerät prüfen.";
        if(currentBoot()!=checkpoint.boot) return retained+"Controller wurde neu gestartet. Die alte Positionsreferenz ist nicht bestätigt; keine automatische Übernahme. Originalauftrag und Unterbrechung bleiben im Export erhalten.";
        if(state.tiltZeroOffset!=checkpoint.tiltZero || draftNeedsReview) return retained+"Die Positionsreferenz muss zuerst geprüft werden; alte Zielkoordinaten werden nicht ungeprüft freigegeben.";
        if(canPrepareRest()) return retained+"Wiederaufnahme bereit: "+checkpoint.raster.unit()+" wählen, Restauftrag laden und anschließend separat starten. "+
            (state.state.equals("PAUSED")?"Fortsetzen ohne Wiederholung ist über Fortsetzen möglich.":"");
        return retained+(state.state.equals("FAULT")?faultDescription(state.lastError)+" Erst nach behobener Ursache erneut prüfen.":navigationReason());
    }
    private void cancelRest() { restPreparation=null;preparingRest=false;pendingQuality=null; }
    /** Only prepare; the normal separately confirmed START remains the sole release for movement. */
    public void prepareRest(boolean previousColumn,long expectedRun,long expectedIndex) {
        requireReady();CaptureStore.Checkpoint source=restCheckpoint();
        if(source.raster.source.runId!=expectedRun || source.raster.nextIndex!=expectedIndex)
            throw new IllegalArgumentException("Unterbrochener Auftrag hat sich geändert; erneut auswählen");
        long newId;
        do { newId=java.util.concurrent.ThreadLocalRandom.current().nextLong(1,4294967296L); } while(newId==expectedRun);
        PanoramaPlan rest=source.raster.remaining(newId,previousColumn);rest.validate(state);
        captures.linkRest(source,rest,source.raster.startColumn(previousColumn));
        RestPreparation flow=new RestPreparation(expectedRun,newId,state.state.equals("IDLE"));restPreparation=flow;preparingRest=true;
        int id=generation;long boot=currentBoot();
        support.append("REST_PREPARE source="+source.key+" index="+expectedIndex+" startColumn="+source.raster.startColumn(previousColumn)+" recipe="+String.join("|",rest.fields()));
        Runnable advance=new Runnable() {
            long deadline=SystemClock.elapsedRealtime()+5000;
            public void run() {
                if(restPreparation!=flow) return;
                if(id!=generation || boot!=currentBoot() || SystemClock.elapsedRealtime()>=deadline || incident.needsAcknowledgement() || uncertain) {
                    finish("Restauftrag nicht bestätigt · Gerätestand prüfen");return;
                }
                String next=flow.next(state.state,state.runId,statusVersion,fresh(),state.plan!=null && state.plan.runId==rest.runId && rest.sameRecipe(state.plan));
                if(flow.failed()) { finish("Restauftrag abgelehnt · alter Lauf bleibt gespeichert");return; }
                if(flow.done()) { finish("Restauftrag geladen · "+rest.total()+" Aufnahmen · zum Fahren separat Auftrag starten");return; }
                if(next!=null) {
                    execute(next,next.equals("LOAD_PLAN")?rest.fields():new String[0],false,true).whenComplete((reply,error) -> {
                        if(restPreparation!=flow || generation!=id) return;
                        flow.acknowledged(error==null && reply.accepted);deadline=SystemClock.elapsedRealtime()+5000;
                        if(error==null && reply.accepted && next.equals("LOAD_PLAN")) {
                            // A fresh support bundle confirms the exact recipe as well as PLAN_READY.
                            execute("GET_SUPPORT_BUNDLE",new String[0],false,true).whenComplete((bundle,bundleError) -> {
                                if(restPreparation==flow) ui.post(this);
                            });
                        } else ui.post(this);
                    });
                } else ui.postDelayed(this,100);
            }
            void finish(String detail) {
                restPreparation=null;preparingRest=false;message=detail;support.append("REST_RESULT "+detail);notifyObservers();
            }
        };
        advance.run();notifyObservers();
    }
    void acceptManualEvent(String[] fields) {
        if (!state.manualStatusSupported && manualMoving && manualProgress != null && manualProgress.accept(fields)) {
            manualMoving = false; manualProgress = null; message = "Manuelles Ziel erreicht";
        }
    }
    private void updateManualAction() {
        if(jog.sequence>0 && state.manualStatusFresh(SystemClock.elapsedRealtime()) && state.manualStatus!=null &&
           state.manualStatus.commandSequence==jog.sequence && state.manualStatus.kind.equals("JOG") && !state.manualStatus.active()) {
            jog.finish(jog.token); ui.removeCallbacks(jogAlive); manualMoving=targetKnown=false;
            message="Dauerfahrt beendet"; deferEndpointCapture();
        }
        if(manualAction==null || (!manualMoving && !manualTrigger) || !fresh() || !state.manualStatusFresh(SystemClock.elapsedRealtime())) return;
        ManualActionProgress.Outcome outcome=manualAction.accept(state.manualStatus,state.manualStatusVersion);
        if(outcome==ManualActionProgress.Outcome.WAITING) return;
        boolean wasMove=manualMoving;
        manualAction=null; manualProgress=null; manualMoving=manualTrigger=false;
        if(outcome==ManualActionProgress.Outcome.SUCCEEDED) { message=wasMove?"Manuelles Ziel erreicht":"Manuelle Kameraauslösung bestätigt"; if(wasMove) deferEndpointCapture(); }
        else if(outcome==ManualActionProgress.Outcome.WARNING) {
            targetKnown=false; deferEndpointCapture();
            message="Warnung: Manuelles Ziel nicht genau erreicht · Motoren gestoppt, erneutes Anfahren möglich";
            signalQualityWarning();
        }
        else {
            targetKnown=false;
            raiseIncident("MANUAL_"+state.manualStatus.result,"Die manuelle Aktion endete mit "+state.manualStatus.result+". Das alte Ziel wird verworfen; Gerätestand prüfen.");
            message="Manuelle Aktion unterbrochen · Fehler quittieren";
        }
        support.append("MANUAL_COMPLETION "+state.manualStatus.commandSequence+" "+state.manualStatus.result);
        notifyObservers();
    }
    void commandFeedback(String text, boolean background, boolean success) {
        if (!background || !success) message = text;
    }
    public boolean fresh() { return connected && state.fresh(SystemClock.elapsedRealtime()); }
    public boolean manualAllowed() { return navigationReason().isEmpty() && state.manual(SystemClock.elapsedRealtime()); }
    public boolean navigationAllowed() { return navigationReason().isEmpty(); }
    boolean connectedTo(String address) { return device!=null && device.getAddress().equals(address); }
    private void signalQualityWarning() {
        if(!getSharedPreferences("settings",MODE_PRIVATE).getBoolean("captureWarningSound",true)) return;
        try {
            if(warningTone==null) warningTone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,60);
            warningTone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2,180);
        } catch(RuntimeException ex) { support.append("WARNING_SOUND_UNAVAILABLE"); }
    }
    public String navigationReason() {
        if(incident.needsAcknowledgement()) return "Fehler quittieren, danach wird der Gerätestand geprüft.";
        if(recovering || recoveryRequested) return "Gerätestand wird geprüft …";
        if(!fresh()) return hasConnection()?"Aktuelle Statusdaten fehlen · Verbindung wird geprüft.":"Zum Ausrichten mit dem Gerät verbinden.";
        if(uncertain) return "Befehlswirkung unklar · Gerätestand prüfen.";
        if(!state.limitsKnown || !state.confirmed) return "Gerätegrenzen sind noch nicht bestätigt.";
        if(state.state.equals("FAULT")) return faultDescription(state.lastError)+" · Gerätestand prüfen.";
        if(state.manualStatusSupported && !state.manualStatusFresh(SystemClock.elapsedRealtime())) return "Aktueller Status der manuellen Aktion fehlt · Gerätestand wird geprüft.";
        if(state.manualStatusSupported && state.manualStatus!=null && state.manualStatus.active() && !manualMoving && !manualTrigger)
            return "Die Steuerung führt noch eine manuelle Aktion aus · Abschluss abwarten oder Not-Stop.";
        if(jog.held) return "Handfahrt aktiv · Pfeil loslassen zum Stoppen.";
        if(jog.stopping) return "Handfahrt wird gestoppt …";
        if(busy) return "Befehlsbestätigung wird abgewartet …";
        if(abortingPlan) return "Auftrag wird gestoppt und zurückgesetzt …";
        if(preparingRest) return "Restauftrag wird geladen · noch keine Fahrfreigabe.";
        if(debugPending || manualPreparing) return "Fahrt wird vorbereitet; Diagnose hat eine begrenzte Wartezeit.";
        if(manualTrigger) return "Kameraauslösung wird abgeschlossen …";
        if(!state.navigation(SystemClock.elapsedRealtime())) return state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")?
            "Aufnahme läuft · zum Ausrichten pausieren oder stoppen.":state.state.equals("TIMELAPSE")?"Timelapse läuft · zuerst stoppen.":
            "Fahrt im Zustand "+state.state+" nicht freigegeben · Gerätestand prüfen.";
        return "";
    }
    static String faultDescription(String code) {
        if(code.equals("MOTION_STALLED")) return "Die Achse hat sich nicht ausreichend zum Ziel bewegt.";
        if(code.equals("MOTION_TARGET_MISSED") || code.equals("MOTION_TARGET_MISSE")) return "Die Fahrt ist beendet, aber die Zielposition liegt außerhalb der eingestellten Toleranz. Motoren sind abgeschaltet.";
        if(code.equals("EXPOSURE_UNSTABLE")) return "Während der Aufnahme wurde eine Positionsänderung erkannt. Die betroffene Aufnahme wurde nicht als erfolgreich gezählt; Motoren und Auslösung sind abgeschaltet.";
        if(code.equals("MOTION_NOT_QUIET") || code.equals("MOTION_SETTLE_TIMEOUT")) return "Die Achse wurde innerhalb der Zeitgrenze nicht ruhig. Motoren sind abgeschaltet.";
        if(code.equals("FAULT_PAN_SENSOR")) return "Die Pan-Encoder-Rückmeldung konnte nicht gültig gelesen werden. Motoren sind abgeschaltet. Nach wieder gültiger Rückmeldung ist eine Wiederaufnahme möglich.";
        if(code.equals("FAULT_TILT_SENSOR")) return "Die Tilt-Encoder-Rückmeldung konnte nicht gültig gelesen werden. Motoren sind abgeschaltet. Nach wieder gültiger Rückmeldung ist eine Wiederaufnahme möglich.";
        if(code.equals("MANUAL_MOVE_TIMEOUT")) return "Die manuelle Fahrt hat ihre Zeitgrenze erreicht.";
        return "Gerätefehler "+code;
    }
    private void saveIncident() {
        getSharedPreferences("incidents",MODE_PRIVATE).edit().putLong("id",incident.id).putString("key",incident.key)
            .putString("detail",incident.detail).putBoolean("active",incident.active).putBoolean("acknowledged",incident.acknowledged)
            .putBoolean("requiresStop",incident.requiresStop).apply();
    }
    private void raiseIncident(String code,String detail) {
        raiseIncident(code,detail,true);
    }
    private void raiseIncident(String code,String detail,boolean requiresStop) {
        String key=(device==null?"":device.getAddress())+"|"+code;
        if(incident.active && incident.key.equals(key) && incident.detail.equals(detail)) return;
        if(incident.active && incident.key.equals(key)) { incident.detail=detail; incident.requiresStop|=requiresStop; }
        else { incident.observe(key,detail); incident.requiresStop=requiresStop; }
        saveIncident(); support.append("INCIDENT "+incident.id+" "+key+" "+detail);
    }
    public void acknowledgeIncident(long id) {
        if(!incident.acknowledge(id)) return;
        saveIncident(); recoverDevice();
    }
    public void recoverDevice() {
        if(incident.needsAcknowledgement()) return;
        if(recovering) return;
        recoveryRequested=true; cancelPreparation(); manualProgress=null; manualAction=null; debugPending=false;
        if(fresh()) beginRecovery();
        else if(device!=null && session==null) { ui.removeCallbacks(retry); open(); }
        else message=hasConnection()?"Warte auf aktuelle Geräteverbindung …":"Zum Prüfen zuerst mit dem Gerät verbinden.";
        notifyObservers();
    }
    private void beginRecovery() {
        if(!uncertain && !incident.requiresStop && fresh() && state.limitsKnown && state.confirmed && !state.state.equals("FAULT")) {
            recoveryRequested=false; incident.resolved(); saveIncident(); targetKnown=false;
            message="Gerätestand abgeglichen · Auftrag "+state.runId+" im Zustand "+state.state+" erhalten";
            support.append("RECOVERY RESYNC_WITHOUT_STOP "+state.runId+" "+state.state); notifyObservers(); return;
        }
        recoveryRequested=false; recovering=true; planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        RecoveryFlow flow=new RecoveryFlow(SystemClock.elapsedRealtime()); recoveryFlow=flow;
        int id=generation;
        Runnable advance=new Runnable() {
            public void run() {
                if(recoveryFlow!=flow || generation!=id) return;
                String next=flow.next(state.state,state.runId,statusVersion,fresh(),SystemClock.elapsedRealtime());
                if(flow.done() || flow.failed()) {
                    recovering=false; recoveryFlow=null; manualMoving=manualTrigger=targetKnown=false;
                    uncertain=!flow.done();
                    nextRecoveryAttemptMs=SystemClock.elapsedRealtime()+10000;
                    if(flow.done()) { incident.resolved(); saveIncident(); message="Gerätestand bestätigt · altes Fahrziel verworfen"; }
                    else message=state.state.equals("FAULT")?faultDescription(state.lastError)+" · automatische Prüfung in 10 s.":"Gerätestand noch nicht bestätigt · automatische Prüfung in 10 s.";
                    support.append("RECOVERY "+(flow.done()?"CONFIRMED":"WAITING")); notifyObservers(); return;
                }
                if(next!=null) {
                    message="Gerätestand wird geprüft …";
                    execute(next,new String[0],next.equals("ESTOP"),true).whenComplete((reply,error) -> {
                        if(recoveryFlow!=flow || generation!=id) return;
                        flow.acknowledged(error==null && reply.accepted,SystemClock.elapsedRealtime()); ui.post(this);
                    });
                } else ui.postDelayed(this,100);
            }
        };
        advance.run(); notifyObservers();
    }
    public boolean hasConnection() { return device != null; }
    public void disconnect() {
        cancelRest();
        pendingTiming=null;
        endJog(); jog.finish(jog.token); ui.removeCallbacks(jogAlive);
        recoveryFlow=null; recovering=recoveryRequested=false;
        planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        plot.disconnected();
        ui.removeCallbacks(retry); reconnect.stop(); ++generation; ++actionGeneration;
        DeviceLink old = session; session = null;
        if (old != null) old.close();
        device = null; connected = busy = manualMoving = manualTrigger = targetKnown = uncertain = false;
        cancelPreparation(); manualProgress = null; manualAction=null;
        debugPending = false;
        state = new DeviceState(); connection = "Nicht verbunden";
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); notifyObservers();
    }
    private void requireReady() {
        if (!fresh() || session == null) throw new IllegalArgumentException("Aktuelle Geräteverbindung fehlt");
        if(incident.needsAcknowledgement() || uncertain || recovering || recoveryRequested) throw new IllegalArgumentException(navigationReason());
        if(state.manualStatusSupported && (!state.manualStatusFresh(SystemClock.elapsedRealtime()) ||
            state.manualStatus.active() && !manualMoving && !manualTrigger)) throw new IllegalArgumentException(navigationReason());
        if (busy) throw new IllegalArgumentException("Befehl wird noch bestätigt");
        if (preparingRest) throw new IllegalArgumentException("Restauftrag wird vorbereitet");
        if (abortingPlan) throw new IllegalArgumentException("Auftrag wird abgebrochen und zurückgesetzt");
        if (manualPreparing) throw new IllegalArgumentException("Fahrt wird vorbereitet");
        if (debugPending) throw new IllegalArgumentException("Gerätelog wird noch übertragen");
    }
    public CompletableFuture<V2Client.Result> command(String type, String... fields) {
        requireReady();
        return execute(type, fields, false);
    }
    private CompletableFuture<V2Client.Result> execute(String type, String[] fields, boolean emergency) {
        return execute(type, fields, emergency, false);
    }
    private CompletableFuture<V2Client.Result> execute(String type, String[] fields, boolean emergency, boolean background) {
        boolean jogStop=type.equals("MANUAL_JOG_STOP");
        if(session==null || (!emergency && !jogStop && busy)) {
            CompletableFuture<V2Client.Result> unavailable=new CompletableFuture<>();
            unavailable.completeExceptionally(new IOException(session==null?"Keine Geräteverbindung":"Andere Aktion wartet auf Bestätigung")); return unavailable;
        }
        int sessionId = generation, actionId = ++actionGeneration;
        if (type.equals("GET_DEBUG_LOG")) {
            int requestId=++debugRequestId;
            debugPending = true;
            ui.postDelayed(() -> {
                if (sessionId == generation && debugPending && requestId == debugRequestId) {
                    debugPending=false; cancelPreparation();
                    message="Diagnoselog unvollständig · Abfrage beendet; Gerätestand bleibt sichtbar"; notifyObservers();
                }
            },5000);
        }
        busy = true; commandFeedback(actionName(type) + " wird gesendet …", background, true); support.append("TX " + type + " " + String.join("|", fields));
        CompletableFuture<V2Client.Result> wireResult;
        try { wireResult=emergency?session.emergencyStop(""+state.runId):jogStop?session.stopJog(fields[0]):session.send(type,fields); }
        catch(RuntimeException ex) { wireResult=new CompletableFuture<>(); wireResult.completeExceptionally(ex); }
        CompletableFuture<V2Client.Result> result = new CompletableFuture<>();
        wireResult.whenComplete((reply, error) -> ui.post(() -> {
            if (sessionId != generation || actionId != actionGeneration) {
                result.completeExceptionally(new IOException("Alte Aktion verworfen")); return;
            }
            busy = false;
            if(error!=null && !CommandEffects.readOnly(type) && !(error instanceof IllegalArgumentException)) {
                uncertain=true; raiseIncident("COMMAND_"+type,"Die Wirkung von „"+actionName(type)+"“ wurde nicht bestätigt. Gerätestand prüfen.");
            }
            if (type.equals("GET_DEBUG_LOG") && (error != null || !reply.accepted)) debugPending = false;
            commandFeedback(error != null ? (CommandEffects.readOnly(type)?"Geräteabfrage nicht abgeschlossen; keine neue Bewegung gesendet.":"Befehlswirkung unklar · Fehler quittieren und Gerätestand prüfen.")
                : (reply.accepted ? "Bestätigt: " : "Abgelehnt: ") + resultName(reply.code), background, error == null && reply.accepted);
            support.append(error == null ? "RESULT " + type + " " + reply.code : "ERROR " + type + " " + error.getMessage());
            if (emergency && error == null && reply.accepted) { manualMoving = manualTrigger = targetKnown = uncertain = false; }
            if (error == null) result.complete(reply); else result.completeExceptionally(error);
            notifyObservers();
        }));
        notifyObservers(); return result;
    }
    private static String actionName(String type) {
        switch (type) {
            case "MANUAL_JOG_START": return "Handfahrt";
            case "MANUAL_JOG_START_VELOCITY": return "Handfahrt";
            case "MANUAL_JOG_KEEPALIVE": return "Tastensignal";
            case "MANUAL_JOG_STOP": return "Fahrtstopp";
            case "MANUAL_MOVE": return "Fahrziel"; case "TRIGGER_ONCE": return "Kameraauslösung";
            case "LOAD_PLAN": return "Panoramaauftrag"; case "START": return "Start";
            case "PAUSE": return "Pause"; case "RESUME": return "Fortsetzen";
            case "STOP": return "Stop"; case "ESTOP": return "Not-Stop";
            case "START_TIMELAPSE": return "Timelapse-Start"; case "STOP_TIMELAPSE": return "Timelapse-Stop";
            case "CLEAR_RUN": return "Auftrag zurücksetzen"; case "UPDATE_TIMING": return "Zeitänderung";
            case "REWIND_COLUMNS": return "Spaltenrücksprung"; case "FORWARD_COLUMNS": return "Spaltenvorsprung";
            case "SET_TILT_LIMITS": return "Tilt-Grenzen";
            case "SET_CAPTURE_WARNING_TOLERANCE": return "Warnschwellen speichern";
            case "SET_CAPTURE_STABILITY": return "Stabilitätsgrenzen speichern";
            case "GET_CAPTURE_STABILITY": return "Stabilitätsgrenzen auslesen";
            case "RETRY_COLUMN": return "Spaltenwiederholung vorbereiten";
            case "SET_TILT_ZERO": return "Tilt-Nullpunkt"; case "RESET_TILT_ZERO": return "Tilt-Werkreferenz";
            default: return "Geräteabfrage";
        }
    }
    private static String resultName(String code) {
        switch (code) {
            case "JOGGING": return "Handfahrt aktiv";
            case "JOG_ALIVE": return "Taste gehalten";
            case "JOG_STOPPED": return "Handfahrt gestoppt";
            case "JOG_RELEASED": return "Handfahrt beendet";
            case "JOG_NOT_READY": return "Achse beruhigt sich oder Grenze erreicht · Taste loslassen und erneut halten";
            case "JOG_SPEED_LIMIT": return "Haltefahrt-Tempo liegt über der Geräteobergrenze · Tempo reduzieren oder Motor-Konfiguration anpassen";
            case "OK": return "Geräteabfrage angenommen"; case "MOVING": return "Fahrziel angenommen";
            case "TRIGGERED": return "Kamera-Puls gestartet"; case "PLAN_ACCEPTED": return "Plan übernommen";
            case "STARTED": return "Auftrag gestartet"; case "PAUSED": return "Pausiert";
            case "PAUSE_PENDING": return "Pause nach dem aktuellen Bild angefordert"; case "RESUMED": return "Auftrag fortgesetzt";
            case "STOPPED": return "Gestoppt"; case "CLEARED": return "Auftrag gelöscht";
            case "TIMING_UPDATED": return "Neue Zeiten übernommen"; case "TIMING_QUEUED": return "Neue Zeiten vorgemerkt";
            case "WARNING_TOLERANCE_SAVED": return "Warnschwellen dauerhaft im Gerät gespeichert";
            case "STABILITY_SETTINGS_SAVED": return "Stabilitätsgrenzen gespeichert · Rücklesen abwarten";
            case "COLUMN_RETRY_READY": return "Spalte vorbereitet · zum Fahren separat Fortsetzen drücken";
            case "INVALID_STATE": return "Im aktuellen Gerätezustand nicht möglich";
            case "MANUAL_ACTION_ACTIVE": return "Manuelle Aktion noch aktiv";
            case "SAFETY_UNCONFIRMED": return "Sicherheitsgrenzen nicht bestätigt";
            case "PAN_LIMIT": return "Pan-Grenze überschritten"; case "TILT_LIMIT": return "Tilt-Grenze überschritten";
            case "VALUE_OUT_OF_RANGE": return "Wert außerhalb des erlaubten Bereichs";
            case "TILT_ZERO_SAVED": return "Tilt-Nullpunkt gespeichert";
            case "STORE_FAILED": return "Speichern im Gerät fehlgeschlagen";
            case "CALIBRATION_UNSUPPORTED": return "Gerät unterstützt keinen Tilt-Nullpunkt";
            case "SEQUENCE_CONFLICT": return "Befehlsnummer bereits verwendet; Aktion wurde abgelehnt";
            default: return code;
        }
    }
    public void emergencyStop() {
        cancelRest();
        jog.finish(jog.token); ui.removeCallbacks(jogAlive);
        recoveryFlow=null; recovering=recoveryRequested=false;
        planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        if (!connected || session == null) throw new IllegalArgumentException("Keine Bluetooth-Verbindung für Not-Stop");
        cancelPreparation(); manualProgress = null; debugPending = false;
        manualAction=null;
        execute("ESTOP", new String[]{""+state.runId, "ANDROID_ESTOP"}, true);
    }
    public void setTiltZero(boolean reset) {
        requireReady();
        if(!state.tiltCalibrationKnown || !state.positionSafe() || !state.state.equals("IDLE") || manualMoving || manualTrigger)
            throw new IllegalArgumentException("Tilt-Nullpunkt nur im Stillstand ohne Geräteauftrag setzen");
        int id=generation;
        command(reset?"RESET_TILT_ZERO":"SET_TILT_ZERO").whenComplete((reply,error) -> {
            if(id==generation && error==null && reply.accepted) {
                markDraftForReview(); targetKnown=false; state.lastStatusMs=0;
                message="Tilt-Nullpunkt gespeichert · Entwurfreferenz prüfen"; notifyObservers();
                // A cached ACK after a retry does not repeat calibration metadata.
                execute("GET_SUPPORT_BUNDLE",new String[0],false,true);
            }
        });
    }
    public void move(double panDelta, double tiltDelta) {
        requireReady();
        if(manualMoving || jog.held || jog.stopping) throw new IllegalArgumentException("Laufende Fahrt zuerst beenden");
        if (!navigationAllowed()) throw new IllegalArgumentException(navigationReason());
        int dx = DeviceState.toDevice(panDelta), dy = DeviceState.toTiltDevice(tiltDelta);
        int x = ManualTargets.step(targetKnown && state.positionSafe() ? targetPan : state.pan, state.pan, dx);
        int y = ManualTargets.step(targetKnown && state.positionSafe() ? targetTilt : state.tilt, state.tilt, dy);
        x=DeviceState.manualStep(state.pan,x,state.panMin,state.panMax);
        y=DeviceState.manualStep(state.tilt,y,state.tiltMin,state.tiltMax);
        if(x==state.pan && y==state.tilt) throw new IllegalArgumentException("Grenze erreicht · in Gegenrichtung zurückfahren");
        prepareMove(x,y);
    }
    public void setManualTempo(int tempo) {
        if(tempo<1 || tempo>1000) throw new IllegalArgumentException("Fahrtempo muss 1–1000 ‰ betragen");
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("manualSpeed",""+tempo).apply();
    }
    public void setManualTempos(int panTempo,int tiltTempo) {
        if(panTempo<1 || panTempo>1000 || tiltTempo<1 || tiltTempo>1000)
            throw new IllegalArgumentException("Pan-/Tilt-Zielfahrtempo muss 1–1000 ‰ betragen");
        getSharedPreferences("settings",MODE_PRIVATE).edit()
            .putString("manualPanSpeed",Integer.toString(panTempo))
            .putString("manualSpeed",Integer.toString(tiltTempo)).apply();
    }
    private String[] manualMoveFields(int x,int y) {
        android.content.SharedPreferences preferences=getSharedPreferences("settings",MODE_PRIVATE);
        int panTempo=Integer.parseInt(preferences.getString("manualPanSpeed","500"));
        int tiltTempo=Integer.parseInt(preferences.getString("manualSpeed","500"));
        return ManualMotionOptions.fields(state,x,y,panTempo,tiltTempo);
    }
    public boolean jogAllowed() { return state.jogSupported && navigationAllowed() && !busy && !manualMoving && !jog.held && !jog.stopping; }
    public void setJogSpeed(double speed) {
        setJogSpeed(1,speed);
    }
    public void setJogSpeed(int axis,double speed) {
        int maximum=axis==0 && state.jogMaximumPanVelocityMdeg>0?state.jogMaximumPanVelocityMdeg:
            state.jogMaximumVelocityMdeg>0?state.jogMaximumVelocityMdeg:10000;
        if(!Double.isFinite(speed) || speed<0.5 || speed>maximum/1000.0)
            throw new IllegalArgumentException("Haltefahrt-Tempo: 0,5 bis "+(maximum/1000.0)+" °/s; Geräteobergrenze per Motor-Konfiguration anpassbar");
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString(axis==0?"panJogSpeed":"jogSpeed",Double.toString(speed)).apply();
    }
    public void setRememberSettleMillis(int millis) {
        if(millis<0 || millis>5000) throw new IllegalArgumentException("Beruhigung vor Positionmerken: 0 bis 5000 ms");
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("rememberSettleMs",Integer.toString(millis)).apply();
    }
    private void deferEndpointCapture() {
        int settle=1000;
        try { int configured=Integer.parseInt(getSharedPreferences("settings",MODE_PRIVATE).getString("rememberSettleMs","1000"));
            if(configured>=0 && configured<=5000) settle=configured;
        } catch(NumberFormatException ignored) { /* Invalid edits keep the safe default. */ }
        endpointCaptureAfterMs=SystemClock.elapsedRealtime()+settle;
        ui.postDelayed(this::notifyObservers,settle);
    }
    public boolean endpointCaptureAllowed() {
        return fresh() && !manualMoving && !manualTrigger && !jog.held && !jog.stopping &&
            SystemClock.elapsedRealtime()>=endpointCaptureAfterMs && state.lastStatusMs>=endpointCaptureAfterMs;
    }
    public boolean jogHeldFor(int axis,int direction) { return jog.matches(axis,direction); }
    public void beginJog(int axis,int direction) {
        requireReady();
        if(!jogAllowed()) throw new IllegalArgumentException(state.jogSupported?"Fahrt zuerst beenden":"Firmware für Gedrückthalten aktualisieren");
        boolean velocityMode=state.jogMaximumVelocityMdeg>0;
        int maximum=axis==0 && state.jogMaximumPanVelocityMdeg>0?state.jogMaximumPanVelocityMdeg:state.jogMaximumVelocityMdeg;
        String defaultSpeed=axis==0?Double.toString(Math.min(10,maximum/1000.0)):"5";
        double requested=velocityMode?Double.parseDouble(getSharedPreferences("settings",MODE_PRIVATE).getString(axis==0?"panJogSpeed":"jogSpeed",defaultSpeed)):0;
        if(velocityMode && (!Double.isFinite(requested) || requested<0.5 || requested>maximum/1000.0))
            throw new IllegalArgumentException("Haltefahrt-Tempo überschreitet die Geräteobergrenze; Tempo einstellen");
        int speed=velocityMode?(int)Math.round(requested*1000):
            Integer.parseInt(getSharedPreferences("settings",MODE_PRIVATE).getString("manualSpeed","300"));
        if(!jog.begin(axis,direction,SystemClock.elapsedRealtime())) return;
        long token=jog.token; int id=generation;
        manualAction=null; manualProgress=null; targetKnown=false; manualMoving=true;
        // A held gesture has no target-completion deadline. The renewable
        // firmware lease and freshness checks bound loss of touch or link.
        manualDeadlineMs=Long.MAX_VALUE;
        execute(velocityMode?"MANUAL_JOG_START_VELOCITY":"MANUAL_JOG_START",new String[]{""+token,""+axis,""+direction,""+speed},false).whenComplete((reply,error) -> {
            if(id!=generation || jog.token!=token || !jog.held) return;
            if(error!=null || !reply.accepted) { jog.finish(token); manualMoving=false; ui.removeCallbacks(jogAlive); }
            else { jog.sequence=reply.commandSequence; message="Gedrückt halten · Loslassen stoppt"; }
            notifyObservers();
        });
        ui.postDelayed(jogAlive,150);
    }
    public void endJog() {
        long token=jog.release(); ui.removeCallbacks(jogAlive);
        if(token==0) return;
        int id=generation;
        execute("MANUAL_JOG_STOP",new String[]{""+token},false,true).whenComplete((reply,error) -> {
            if(id!=generation || jog.token!=token) return;
            jog.finish(token); manualMoving=targetKnown=false;
            if(error==null && reply.accepted) {
                message="Dauerfahrt gestoppt";
                deferEndpointCapture();
            }
            notifyObservers();
        });
    }
    public void moveToUi(double pan,double tilt) {
        requireReady();
        if(manualMoving) throw new IllegalArgumentException("Vor der nächsten grafischen Fahrt Zielankunft abwarten");
        prepareMove(DeviceState.toDevice(pan),DeviceState.toTiltDevice(tilt));
    }
    private void prepareMove(int x,int y) {
        requireReady();
        if(!navigationAllowed()) throw new IllegalArgumentException("Fahrt in diesem Gerätezustand nicht freigegeben");
        state.validateManualTarget(x, y);
        if(state.manualStatusSupported) { sendManualMove(x,y); return; }
        int id = generation;
        manualPreparing = true; preparationAcknowledged = false; manualBaseline = -1;
        message = "Fahrt wird vorbereitet …";
        pendingManual = () -> {
            if (!navigationAllowed()) { message = "Fahrt nicht gestartet: aktuelle Freigabe fehlt"; return; }
            state.validateManualTarget(x,y);
            CompletableFuture<V2Client.Result> move = command("MANUAL_MOVE", manualMoveFields(x,y));
            int action = actionGeneration;
            move.whenComplete((reply,error) -> {
                if (id == generation && action == actionGeneration && error == null && reply.accepted) {
                    targetPan = x; targetTilt = y; targetKnown = true; manualMoving = true;
                    manualDeadlineMs=SystemClock.elapsedRealtime()+125000;
                    manualProgress = new ManualMoveProgress(manualBaseline,x,y); notifyObservers();
                }
            });
        };
        Runnable prepared = pendingManual;
        execute("GET_DEBUG_LOG", new String[]{""+newestDebugSequence}, false, true).whenComplete((reply,error) -> {
            if (id != generation || pendingManual != prepared) return;
            if (error != null || !reply.accepted) { cancelPreparation(); message = "Fahrt nicht gestartet: Gerätelog nicht verfügbar"; }
            else { preparationAcknowledged = true; finishPreparation(); }
        });
        ui.postDelayed(() -> {
            if (id == generation && pendingManual == prepared) {
                cancelPreparation(); message = "Fahrt nicht gestartet: Gerätelog unvollständig"; notifyObservers();
            }
        }, 5000);
    }
    private void sendManualMove(int x,int y) {
        int id=generation;
        CompletableFuture<V2Client.Result> sent=command("MANUAL_MOVE",manualMoveFields(x,y));
        int action=actionGeneration;
        sent.whenComplete((reply,error) -> {
            if(id!=generation || action!=actionGeneration || error!=null || !reply.accepted) return;
            if(reply.commandSequence<1 || reply.commandSequence>65535) {
                uncertain=true; raiseIncident("MANUAL_CONFIRMATION","Fahrbestätigung ohne gültige Befehlsnummer. Gerätestand prüfen."); notifyObservers(); return;
            }
            targetPan=x; targetTilt=y; targetKnown=true; manualMoving=true;
            manualDeadlineMs=SystemClock.elapsedRealtime()+125000;
            // Discard all status received before this ACK, including an old
            // retained result with a coincidentally reused uint16 sequence.
            manualProgress=null; manualAction=new ManualActionProgress(reply.commandSequence,"MOVE",x,y,state.manualStatusVersion);
            updateManualAction(); notifyObservers();
        });
    }
    private void cancelPreparation() { pendingManual = null; manualPreparing = false; preparationAcknowledged = false; }
    private void finishPreparation() {
        if (pendingManual == null || !preparationAcknowledged || manualBaseline < 0 || busy) return;
        Runnable action = pendingManual; cancelPreparation();
        try { action.run(); } catch(RuntimeException ex) { message="Fahrt nicht gestartet: "+ex.getMessage(); notifyObservers(); }
    }
    public void trigger(int pulse) {
        requireReady();
        if (!manualAllowed() || !state.positionSafe() || manualMoving || pulse < 1 || pulse > 60000) {
            throw new IllegalArgumentException("Kamera-Test erst im Stillstand; Puls 1–60000 ms");
        }
        manualTrigger = true;
        triggerDeadlineMs=SystemClock.elapsedRealtime()+pulse+5000;
        int id = generation;
        CompletableFuture<V2Client.Result> sent=command("TRIGGER_ONCE", ""+pulse);
        int action=actionGeneration;
        sent.whenComplete((reply,error) -> {
            if(id!=generation || action!=actionGeneration || error!=null) return;
            if(!reply.accepted) manualTrigger=false;
            else if(state.manualStatusSupported) {
                if(reply.commandSequence<1 || reply.commandSequence>65535) {
                    uncertain=true; raiseIncident("MANUAL_CONFIRMATION","Auslösungsbestätigung ohne gültige Befehlsnummer. Gerätestand prüfen.");
                } else {
                    manualAction=new ManualActionProgress(reply.commandSequence,"TRIGGER",0,0,state.manualStatusVersion); updateManualAction();
                }
            }
            notifyObservers();
        });
    }
    public void loadPlan(PanoramaPlan plan) {
        requireReady();
        if(draftNeedsReview) throw new IllegalArgumentException("Entwurfreferenz nach Geräte-/Nullpunktwechsel prüfen und bestätigen");
        if (!state.state.equals("IDLE")) throw new IllegalArgumentException("Vor neuem Plan vorhandenen Lauf bewusst löschen");
        if (manualMoving || manualTrigger) throw new IllegalArgumentException("Manuelle Aktion noch aktiv");
        plan.validate(state);
        int id = generation;
        command("LOAD_PLAN", plan.fields()).whenComplete((reply, error) -> {
            if (id == generation && error == null && reply.accepted) command("GET_SUPPORT_BUNDLE");
        });
    }
    public void runCommand(String type) {
        requireReady();
        String s = state.state;
        boolean valid;
        switch (type) {
            case "START": valid = s.equals("PLAN_READY") && state.plan != null && !manualMoving && !manualTrigger; break;
            case "PAUSE": valid = s.equals("RUNNING"); break;
            case "RESUME": valid = s.equals("PAUSED") && !manualMoving && !manualTrigger; break;
            case "STOP": valid = !s.equals("IDLE") && !s.startsWith("TIMELAPSE"); break;
            case "STOP_TIMELAPSE": valid = s.equals("TIMELAPSE"); break;
            case "CLEAR_RUN": valid = s.equals("STOPPED") || s.equals("COMPLETED") || s.equals("FAULT") || s.equals("PLAN_READY") || s.equals("TIMELAPSE_STOPPED"); break;
            default: throw new IllegalArgumentException("Unbekannte Laufaktion");
        }
        if (!valid) throw new IllegalArgumentException("Aktion im Zustand " + s + " nicht möglich");
        if(type.equals("CLEAR_RUN") && s.equals("PLAN_READY")) { abortPlan(); return; }
        if ((type.equals("START") || type.equals("RESUME")) && !state.positionSafe())
            throw new IllegalArgumentException("Gemeldete Istposition außerhalb bestätigter Grenzen; Encoder/Mechanik prüfen");
        if (type.equals("CLEAR_RUN")) command(type); else command(type, ""+state.runId);
    }
    public void abortPlan() {
        abortPlan(false);
    }
    private void markDraftForReview() {
        draftNeedsReview=true; getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("draftNeedsReview",true).apply();
    }
    public void confirmDraftReference() {
        if(!fresh() || !state.limitsKnown || !state.confirmed) throw new IllegalArgumentException("Aktuelle Referenz und Grenzen zuerst vom Gerät lesen");
        draftNeedsReview=false; getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("draftNeedsReview",false).apply(); notifyObservers();
    }
    public void discardDraft() {
        draftEndpoints.clear(); draftRevision++; draftNeedsReview=false;
        android.content.SharedPreferences.Editor editor=getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("draftNeedsReview",false);
        for(String key:new String[]{"startPan","startTilt","endPan","endTilt"}) editor.putString(key,""); editor.apply();
    }
    public void abortPlan(boolean discardDraftWhenIdle) {
        requireReady();
        discardDraftAfterAbort=discardDraftWhenIdle;
        PlanAbort progress=new PlanAbort(state.state); planAbort=progress; abortingPlan=true;
        int id=generation; long run=state.runId;
        Runnable sequence=new Runnable() {
            long deadline=SystemClock.elapsedRealtime()+5000;
            public void run() {
                if(planAbort!=progress) return;
                if(generation!=id) { planAbort=null; abortingPlan=false; notifyObservers(); return; }
                if(progress.failed()) { planAbort=null; abortingPlan=false; notifyObservers(); return; }
                if(SystemClock.elapsedRealtime()>deadline || (state.runId!=run && !state.state.equals("IDLE"))) {
                    planAbort=null; abortingPlan=false; message="Abbruch nicht vollständig bestätigt · Gerätestand prüfen"; notifyObservers(); return;
                }
                String command=progress.next(state.state,fresh());
                if(progress.done()) {
                    planAbort=null; abortingPlan=false; manualMoving=manualTrigger=targetKnown=false;
                    if(discardDraftAfterAbort) { discardDraft(); message="Plan entfernt · bereit für neuen Plan"; }
                    else message="Auftrag abgebrochen · bereit für neuen Plan";
                    discardDraftAfterAbort=false; notifyObservers(); return;
                }
                if(command!=null) {
                    execute(command,command.equals("STOP")?new String[]{""+run}:new String[0],false).whenComplete((reply,error) -> {
                        if(planAbort!=progress) { return; }
                        if(generation!=id) { planAbort=null; abortingPlan=false; notifyObservers(); return; }
                        progress.acknowledged(error==null && reply.accepted); deadline=SystemClock.elapsedRealtime()+5000;
                        ui.post(this);
                    });
                } else ui.postDelayed(this,100);
            }
        };
        sequence.run(); notifyObservers();
    }
    public void columns(boolean forward) {
        requireReady();
        if (!state.positionSafe()) throw new IllegalArgumentException("Istposition außerhalb der Sicherheitsgrenzen");
        if (state.plan == null || state.plan.mode != 0 || !(state.state.equals("PAUSED") || state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")))
            throw new IllegalArgumentException("Spaltenwechsel nur in einem laufenden/pausierten Raster");
        command(forward ? "FORWARD_COLUMNS" : "REWIND_COLUMNS", ""+state.runId, "1");
    }
    int[] pendingTiming;
    long pendingTimingRun;
    String timingSummary() {
        PanoramaPlan p=state.plan;
        if(p==null) return "Noch kein Geräteplan";
        if(pendingTiming!=null && (pendingTimingRun!=state.runId ||
            (p.settleMs==pendingTiming[0] && p.pulseMs==pendingTiming[1] && p.exposureMs==pendingTiming[2]))) pendingTiming=null;
        if(pendingTiming!=null && pendingTimingRun==state.runId &&
            (p.settleMs!=pendingTiming[0] || p.pulseMs!=pendingTiming[1] || p.exposureMs!=pendingTiming[2]) &&
            (state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")))
            return "Vorgemerkt nach aktuellem Bild: Beruhigung "+pendingTiming[0]+" ms · Puls "+pendingTiming[1]+" ms · Belichtung "+pendingTiming[2]+" ms";
        return "Aktiv: Beruhigung "+p.settleMs+" ms · Puls "+p.pulseMs+" ms · Belichtung "+p.exposureMs+" ms";
    }
    public void saveWarningTolerances(double pan,double tilt) {
        requireReady();
        if(!state.warningSettingsSupported || manualMoving || manualTrigger || jog.held || jog.stopping ||
            !(state.state.equals("IDLE") || state.state.equals("STOPPED")))
            throw new IllegalArgumentException("Warnschwellen im Zustand Bereit oder Gestoppt speichern");
        if(!Double.isFinite(pan) || !Double.isFinite(tilt) || pan<.044 || pan>10 || tilt<.044 || tilt>10)
            throw new IllegalArgumentException("Warnschwelle: 0,044 bis 10 Grad");
        command("SET_CAPTURE_WARNING_TOLERANCE",""+Math.round(pan*1000),""+Math.round(tilt*1000),""+state.motionConfigRevision);
    }
    boolean canConfigureStability() {
        boolean idle=state.runId==0 && (state.state.equals("IDLE") || state.state.equals("STOPPED"));
        boolean paused=state.recoveryPauseSupported && state.state.equals("PAUSED");
        return fresh() && state.captureStability!=null && !busy && !manualMoving && !manualTrigger &&
            !jog.held && !jog.stopping && !uncertain && !incident.needsAcknowledgement() &&
            (idle || paused);
    }
    void observeStabilitySettings() {
        if(pendingStability!=null && pendingStability.sameValues(state.captureStability) &&
            state.motionConfigRevision>=pendingStability.revision) {
            pendingStability=null;message="Stabilitätsgrenzen dauerhaft gespeichert und zurückgelesen";
        }
    }
    public void readCaptureStability() {
        requireReady();
        if(state.captureStability==null) throw new IllegalArgumentException("Diese Firmware bietet keine Stabilitätseinstellungen in der App");
        command("GET_CAPTURE_STABILITY");
    }
    public void saveCaptureStability(double span,double pan,double tilt,double seconds) {
        requireReady();
        if(!canConfigureStability()) throw new IllegalArgumentException("Stabilitätsgrenzen bei stillstehendem Kopf ohne Auftrag oder in einer quittierten Pause speichern");
        CaptureStabilitySettings request=CaptureStabilitySettings.fromUi(span,pan,tilt,seconds,state.motionConfigRevision);
        pendingStability=request;int id=generation;
        command("SET_CAPTURE_STABILITY",request.fields()).whenComplete((reply,error) -> {
            if(id!=generation) return;
            if(error!=null || !reply.accepted) {pendingStability=null;return;}
            if(request.sameValues(state.captureStability) && state.motionConfigRevision>=request.revision) {
                pendingStability=null;message="Stabilitätsgrenzen dauerhaft gespeichert und zurückgelesen";notifyObservers();
            } else if(pendingStability==request) {
                execute("GET_CAPTURE_STABILITY",new String[0],false,true);
                ui.postDelayed(() -> {
                    if(id==generation && pendingStability==request) {
                        pendingStability=null;message="Speicherung bestätigt, Rücklesen fehlt · Stabilitätsgrenzen erneut auslesen";notifyObservers();
                    }
                },4000);
            }
        });
    }
    boolean canRetryColumn() {
        return navigationReason().isEmpty() && state.recoveryPauseSupported && state.state.equals("PAUSED") &&
            state.plan!=null && state.plan.mode==0 && state.nextImageIndex<state.plan.total() && !manualMoving && !manualTrigger;
    }
    public void retryColumn(boolean previous,long expectedRun,long expectedIndex) {
        requireReady();
        if(!canRetryColumn() || state.runId!=expectedRun || state.nextImageIndex!=expectedIndex)
            throw new IllegalArgumentException("Pausierter Auftrag hat sich geändert · erneut auswählen");
        command("RETRY_COLUMN",""+state.runId,previous?"1":"0");
    }
    public void updateTiming(int settle, int pulse, int exposure) {
        requireReady();
        if (state.plan == null || settle < 0 || settle > 600000 || pulse < 1 || pulse > 60000 || exposure < 0 || exposure > 3600000)
            throw new IllegalArgumentException("Zeitparameter oder Auftrag ungültig");
        long run=state.runId;
        command("UPDATE_TIMING", ""+run, ""+settle, ""+pulse, ""+exposure).whenComplete((reply,error) -> {
            if(error==null && reply.accepted) {
                pendingTimingRun=run;pendingTiming=new int[]{settle,pulse,exposure};notifyObservers();
            }
        });
    }
    public void timelapse(long id, int interval, int pulse) {
        requireReady();
        if (!state.state.equals("IDLE") || manualMoving || manualTrigger || id < 1 || id > 4294967295L
                || interval < 100 || interval > 3600000 || pulse < 1 || pulse > 60000 || pulse >= interval)
            throw new IllegalArgumentException("Timelapse benötigt IDLE, gültiges Intervall und kürzeren Puls");
        command("START_TIMELAPSE", ""+id, ""+interval, ""+pulse);
    }
    @Override public void onDestroy() { jog.finish(jog.token); ui.removeCallbacks(jogAlive); ++generation; ++actionGeneration;
        disconnect(); ui.removeCallbacksAndMessages(null); if(warningTone!=null) warningTone.release();
        exportServiceActive=false;
        captures.onExportChanged(key -> { }); if(livePapywizard!=null) livePapywizard.close();
        captures.close(); support.close(); observers.clear(); super.onDestroy();
    }
}
