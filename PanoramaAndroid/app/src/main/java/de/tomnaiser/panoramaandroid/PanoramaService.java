package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.bluetooth.*;
import android.content.Intent;
import android.os.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
    public String connection = UiText.NOT_CONNECTED.text(), message = UiText.READY.text(), diagnostics = UiText.NO_DIAGNOSTICS_YET.text();
    private UiText.Message connectionDisplay=UiText.capture(UiText.NOT_CONNECTED::text);
    private UiText.Message messageDisplay=UiText.capture(UiText.READY::text);
    private UiText.Message diagnosticsDisplay=UiText.capture(UiText.NO_DIAGNOSTICS_YET::text);
    private UiText.Message incidentDisplay;
    String connectionText() { return connectionDisplay.text(); }
    String messageText() { return messageDisplay.text(); }
    String diagnosticsText() { return diagnosticsDisplay.text(); }
    String incidentDetail() { return incidentDisplay==null?incident.detail:incidentDisplay.text(); }
    private void setConnection(Supplier<String> text) { connectionDisplay=UiText.capture(text);connection=connectionDisplay.text(); }
    private void setMessage(Supplier<String> text) { messageDisplay=UiText.capture(text);message=messageDisplay.text(); }
    private void setDiagnostics(Supplier<String> text) { diagnosticsDisplay=UiText.capture(text);diagnostics=diagnosticsDisplay.text(); }
    void languageChanged() {
        connection=connectionText();message=messageText();diagnostics=diagnosticsText();
        if(incidentDisplay!=null) incident.detail=incidentDisplay.text();
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("connection",UiText.PANORAMA_DEVICE.text(),NotificationManager.IMPORTANCE_LOW));
        if(hasConnection()) getSystemService(NotificationManager.class).notify(1,notification());
    }
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
    String papywizardStatus() { return livePapywizard==null?UiText.PREPARING_PAPYWIZARD_BACKUP.text():livePapywizard.status(); }
    void rebuildPapywizard() { if(livePapywizard!=null) livePapywizard.rebuild(); }
    public SupportLog support;
    private final Runnable retry = () -> open();
    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            if (connected && session != null) {
                long now = SystemClock.elapsedRealtime();
                if (now - (state.lastStatusMs == 0 ? openedMs : state.lastStatusMs) > 5000) {
                    session.fail(UiText.NO_CURRENT_STATUS_DATA_FOR_5_SECONDS.text());
                } else if(state.manualStatusSupported) {
                    updateManualAction();
                    if(!state.manualStatusFresh(now)) {
                        long last=state.lastManualStatusMs==0?state.manualStatusExpectedMs:state.lastManualStatusMs;
                        if(now-last>5000) session.fail(UiText.MANUAL_OPERATING_STATUS_MISSING_FOR_5_SECONDS_CHECK.text());
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
                    raiseIncident("ACTION_COMPLETION",() ->  UiText.COMPLETION_OF_THE_MANUAL_ACTION_WAS_NOT_CONFIRMED.text());
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
            if(status.startsWith(UiText.PAPYWIZARD_BACKUP_FAILED_2.text())) ui.post(() -> { if(exportServiceActive) support.append(status); });
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
        incidentDisplay=new UiText.Message(faults.getString("detailDe",incident.detail),faults.getString("detailEn",incident.detail));
        incident.detail=incidentDisplay.text();
        incident.acknowledged=faults.getBoolean("acknowledged",true);
        incident.requiresStop=faults.getBoolean("requiresStop",true);
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("connection", UiText.PANORAMA_DEVICE.text(), NotificationManager.IMPORTANCE_LOW));
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
            } catch (RuntimeException ex) { disconnect(); setMessage(() -> UiText.BLUETOOTH_UNAVAILABLE_OR_PERMISSION_MISSING.text()); notifyObservers(); }
        }
        return START_NOT_STICKY;
    }
    private Notification notification() {
        PendingIntent show = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, PanoramaService.class).setAction("DISCONNECT"), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "connection").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("PanoramaControl").setContentText(connectionText()).setContentIntent(show)
            .setOngoing(true).addAction(new Notification.Action.Builder(null, UiText.DISCONNECT.text(), stop).build()).build();
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
        setConnection(() -> UiText.CONNECTING_VIA_BLUETOOTH.text());
        BluetoothSession opening = new BluetoothSession(new BluetoothSession.Listener() {
            @Override public void onAwaitingProtocol() {
                if (id != generation) return; setConnection(() -> UiText.BLUETOOTH_CONNECTED_WAITING_FOR_V2.text()); notifyObservers();
            }
            @Override public void onConnected() {
                if (id != generation) return;
                connected = true; openedMs = SystemClock.elapsedRealtime(); reconnect.connected();
                setConnection(() -> UiText.BLUETOOTH_CONNECTED_PROTOCOL_V2.text());
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
                            setMessage(() -> UiText.TILT_ZERO_POINT_CHANGED_CHECK_DRAFT_REFERENCE.text());
                        }
                        settings.edit().putInt(key,state.tiltZeroOffset).apply();
                    }
                    if(frame.type.equals("DEVICE_SESSION")) {
                        if(frame.fields.length!=1) throw new IllegalArgumentException(UiText.INCOMPLETE_DEVICE_SESSION.text());
                        long boot=DeviceState.unsigned(frame.fields[0]);
                        boolean reset=deviceSession.accept(boot);
                        getSharedPreferences("settings",MODE_PRIVATE).edit().putLong("session."+device.getAddress(),boot).apply();
                        if(reset) {
                            cancelRest();
                            setMessage(() -> UiText.DEVICE_RESTARTED_DRAFT_RETAINED_CHECK_DEVICE_STATE.text());
                            raiseIncident("DEVICE_RESTART",() -> UiText.THE_DEVICE_WAS_RESTARTED_OLD_ACTIONS_ARE_DISCARDED.text(),false);
                            state.lastStatusMs=0; state.plan=null; targetKnown=false;
                            cancelPreparation(); manualProgress=null; manualAction=null; manualMoving=manualTrigger=false;
                            planAbort=null; abortingPlan=discardDraftAfterAbort=false;
                            if(sessionIdentityReceived) {
                                ++actionGeneration; busy=false; uncertain=false;
                                session.fail(UiText.DEVICE_RESTARTED_CHECK_DEVICE_STATE.text());
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
                        setMessage(() -> UiText.WARNING_FOR_CAPTURE.text()+warning.ordinal+": "+CaptureWarning.describe(warning.flags)+UiText.RUN_CONTINUES.text());
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
                        raiseIncident(state.lastError,() -> faultDescription(state.lastError));
                        setMessage(() -> UiText.DEVICE_ERROR_2.text()+faultDescription(state.lastError));
                    }
                    if(frame.type.equals("STATUS") && state.state.equals("STOPPED") && RasterRecovery.recoverable(state.lastError))
                        recordRunFault(frame.fields);
                    if(frame.type.equals("STATUS")) observeRecoveryPause(frame.fields);
                    if(frame.type.equals("STATUS") && (state.lastError.equals("STEP_TARGET_MISSED") || state.lastError.equals("STEP_MOVE_TIMEOUT")))
                        setMessage(() -> UiText.CAPTURE_TARGET_NOT_REACHED_PRECISELY_WITHIN_THE_MOVEMENT.text());
                    if (frame.type.equals("MANUAL_IMAGE_TRIGGERED") && !state.manualStatusSupported) { manualTrigger = false; setMessage(() -> UiText.MANUAL_CAPTURE_COMPLETED.text()); }
                    if (frame.type.equals("MANUAL_STATUS")) updateManualAction();
                    if (frame.type.equals("DIAGNOSTICS")) readDiagnostics(frame.fields);
                    if (frame.type.equals("DEBUG_LOG_END")) {
                        newestDebugSequence = Integer.parseInt(frame.fields[0]); debugPending = false;
                        if (manualPreparing) { manualBaseline = newestDebugSequence; finishPreparation(); }
                    }
                    if (frame.type.equals("DEBUG_EVENT")) acceptManualEvent(frame.fields);
                    if(recoveryRequested && !recovering && fresh()) beginRecovery();
                } catch (IllegalArgumentException | IOException ex) { setMessage(() -> UiText.DATA_STORAGE_ERROR.text() + ex.getMessage()); }
                notifyObservers();
            }
            @Override public void onDisconnected(String reason) {
                if (id != generation) return;
                cancelRest();
                pendingStability=null;savedPause="";
                pendingTiming=null;
                jog.finish(jog.token); ui.removeCallbacks(jogAlive);
                if(connected && !incident.active) raiseIncident("CONNECTION",() -> UiText.existing(reason)+UiText.THE_DEVICE_STATE_WILL_BE_CHECKED_AFTER_RECONNECTION.text(),manualMoving || manualTrigger || uncertain);
                if(recovering) recoveryRequested=true;
                recoveryFlow=null; recovering=false;
                plot.disconnected();
                session = null; connected = busy = false; ++generation; ++actionGeneration;
                manualMoving = manualTrigger = targetKnown = false;
                cancelPreparation(); manualProgress = null; manualAction=null;
                debugPending = false;
                state = new DeviceState();
                int attempt = reconnect.nextAttempt();
                if (attempt > 0) { setConnection(() -> UiText.existing(reason) + UiText.RETRY.text() + attempt + "/3 in 3 s"); ui.postDelayed(retry, 3000); }
                else {
                    setConnection(() -> UiText.existing(reason)+UiText.WAITING_FOR_DEVICE_RETRY_IN_30_S_DISCONNECT.text());
                    ui.postDelayed(retry,30000);
                }
                if(wakeLock!=null && wakeLock.isHeld()) wakeLock.release();
                notifyObservers();
            }
        });
        session=opening; opening.connect(device); notifyObservers();
    }
    private void readDiagnostics(String[] f) {
        if (f.length < 21) throw new IllegalArgumentException(UiText.INCOMPLETE_DIAGNOSTICS.text());
        int stable = Integer.parseInt(f[5]), required = Integer.parseInt(f[6]);
        setDiagnostics(() -> UiText.PAN_ERROR.text() + f[11] + UiText.M_TILT_ERROR.text() + f[12] + " m°\n" +
            (state.profiledMotion?UiText.STABILITY_CONFIRMATION.text():UiText.STABLE_SAMPLES.text())
            + stable + "/" + required + " · RAM: " + f[19] + UiText.MINIMUM.text() + f[20]);
    }
    private long currentBoot() {
        return device==null?-1:getSharedPreferences("settings",MODE_PRIVATE).getLong("session."+device.getAddress(),-1);
    }
    private void pauseForQuality() {
        CaptureQualityGuard.Issue issue=pendingQuality;
        if(issue==null || busy || uncertain || recovering || recoveryRequested || preparingRest || abortingPlan || !fresh() || incident.needsAcknowledgement()) return;
        if(issue.run!=state.runId || state.state.equals("FAULT") || state.state.equals("STOPPED") || state.state.equals("COMPLETED")) { pendingQuality=null;return; }
        if(state.state.equals("PAUSED")) {
            pendingQuality=null;raiseIncident("QUALITY_DEVIATION",() -> issue.describe()+UiText.JOB_IS_PAUSED_GO_BACK_ONE_COLUMN_AND.text(),false);return;
        }
        if(!state.state.equals("RUNNING")) return;
        pendingQuality=null;int id=generation;
        execute("PAUSE",new String[]{""+issue.run},false,true).whenComplete((reply,error) -> {
            if(generation!=id || state.runId!=issue.run) return;
            if(state.state.equals("FAULT") || incident.needsAcknowledgement()) { support.append("QUALITY_PAUSE superseded by device incident");return; }
            Supplier<String> detail=() -> issue.describe()+(error==null && reply.accepted ?
                UiText.PAUSE_REQUESTED_AFTER_THE_CURRENT_IMAGE_THEN_GO.text():
                UiText.PAUSE_NOT_CONFIRMED_CHECK_CURRENT_DEVICE_STATE_AND.text());
            support.append("QUALITY_PAUSE "+detail.get());raiseIncident("QUALITY_DEVIATION",detail,false);notifyObservers();
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
        raiseIncident("RUN_PAUSE_"+state.lastError,() -> RunRecoveryPause.detail(state),false);
        setMessage(() -> UiText.CAPTURE_PAUSED_JOB_RETAINED_CAUSE.text()+state.lastError);
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
        if(!canPrepareRest()) throw new IllegalArgumentException(UiText.FIRST_ACKNOWLEDGE_ERROR_AND_CONFIRM_UNCHANGED_DEVICE_STATE.text());
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
        String retained=UiText.CAPTURES_AND_ORIGINAL_JOB_ARE_RETAINED.text();
        if(incident.needsAcknowledgement()) return retained+UiText.FIRST_ACKNOWLEDGE_ERROR_AND_CHECK_DEVICE_NO_MOVEMENT.text();
        if(recovering || recoveryRequested) return retained+UiText.CHECKING_DEVICE_STATE_RECOVERY_IS_ENABLED_ONLY_AFTER.text();
        if(!fresh()) return retained+UiText.WAIT_FOR_A_CURRENT_DEVICE_CONNECTION_THEN_CHECK.text();
        if(currentBoot()!=checkpoint.boot) return retained+UiText.CONTROLLER_RESTARTED_THE_OLD_POSITION_REFERENCE_IS_NOT.text();
        if(state.tiltZeroOffset!=checkpoint.tiltZero || draftNeedsReview) return retained+UiText.THE_POSITION_REFERENCE_MUST_BE_CHECKED_FIRST_OLD.text();
        if(canPrepareRest()) return retained+UiText.RECOVERY_READY_SELECT.text()+checkpoint.raster.unit()+UiText.LOAD_RECOVERY_JOB_THEN_START_IT_SEPARATELY.text()+
            (state.state.equals("PAUSED")?UiText.USE_RESUME_TO_CONTINUE_WITHOUT_REPEATING.text():"");
        return retained+(state.state.equals("FAULT")?faultDescription(state.lastError)+UiText.CHECK_AGAIN_ONLY_AFTER_RESOLVING_THE_CAUSE.text():navigationReason());
    }
    private void cancelRest() { restPreparation=null;preparingRest=false;pendingQuality=null; }
    /** Only prepare; the normal separately confirmed START remains the sole release for movement. */
    public void prepareRest(boolean previousColumn,long expectedRun,long expectedIndex) {
        requireReady();CaptureStore.Checkpoint source=restCheckpoint();
        if(source.raster.source.runId!=expectedRun || source.raster.nextIndex!=expectedIndex)
            throw new IllegalArgumentException(UiText.INTERRUPTED_JOB_HAS_CHANGED_SELECT_AGAIN.text());
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
                    finish(() -> UiText.RECOVERY_JOB_NOT_CONFIRMED_CHECK_DEVICE_STATE.text());return;
                }
                String next=flow.next(state.state,state.runId,statusVersion,fresh(),state.plan!=null && state.plan.runId==rest.runId && rest.sameRecipe(state.plan));
                if(flow.failed()) { finish(() -> UiText.RECOVERY_JOB_REJECTED_OLD_RUN_REMAINS_SAVED.text());return; }
                if(flow.done()) { finish(() -> UiText.RECOVERY_JOB_LOADED.text()+rest.total()+UiText.CAPTURES_START_JOB_SEPARATELY_TO_MOVE.text());return; }
                if(next!=null) {
                    String[] fields=next.equals("LOAD_PLAN")?rest.fields():
                        next.equals("STOP")?new String[]{""+expectedRun}:new String[0];
                    execute(next,fields,false,true).whenComplete((reply,error) -> {
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
            void finish(Supplier<String> detail) {
                restPreparation=null;preparingRest=false;setMessage(detail);support.append("REST_RESULT "+message);notifyObservers();
            }
        };
        advance.run();notifyObservers();
    }
    void acceptManualEvent(String[] fields) {
        if (!state.manualStatusSupported && manualMoving && manualProgress != null && manualProgress.accept(fields)) {
            manualMoving = false; manualProgress = null; setMessage(() -> UiText.MANUAL_TARGET_REACHED.text());
        }
    }
    private void updateManualAction() {
        if(jog.sequence>0 && state.manualStatusFresh(SystemClock.elapsedRealtime()) && state.manualStatus!=null &&
           state.manualStatus.commandSequence==jog.sequence && state.manualStatus.kind.equals("JOG") && !state.manualStatus.active()) {
            jog.finish(jog.token); ui.removeCallbacks(jogAlive); manualMoving=targetKnown=false;
            setMessage(() -> UiText.JOG_ENDED.text()); deferEndpointCapture();
        }
        if(manualAction==null || (!manualMoving && !manualTrigger) || !fresh() || !state.manualStatusFresh(SystemClock.elapsedRealtime())) return;
        ManualActionProgress.Outcome outcome=manualAction.accept(state.manualStatus,state.manualStatusVersion);
        if(outcome==ManualActionProgress.Outcome.WAITING) return;
        boolean wasMove=manualMoving;
        manualAction=null; manualProgress=null; manualMoving=manualTrigger=false;
        if(outcome==ManualActionProgress.Outcome.SUCCEEDED) { setMessage(() -> wasMove?UiText.MANUAL_TARGET_REACHED.text():UiText.MANUAL_CAMERA_TRIGGER_CONFIRMED.text()); if(wasMove) deferEndpointCapture(); }
        else if(outcome==ManualActionProgress.Outcome.WARNING) {
            targetKnown=false; deferEndpointCapture();
            setMessage(() -> UiText.WARNING_MANUAL_TARGET_NOT_REACHED_PRECISELY_MOTORS_STOPPED.text());
            signalQualityWarning();
        }
        else {
            targetKnown=false;
            raiseIncident("MANUAL_"+state.manualStatus.result,() -> UiText.THE_MANUAL_ACTION_ENDED_WITH.text()+state.manualStatus.result+UiText.THE_OLD_TARGET_IS_DISCARDED_CHECK_DEVICE_STATE.text());
            setMessage(() -> UiText.MANUAL_ACTION_INTERRUPTED_ACKNOWLEDGE_ERROR.text());
        }
        support.append("MANUAL_COMPLETION "+state.manualStatus.commandSequence+" "+state.manualStatus.result);
        notifyObservers();
    }
    void commandFeedback(String text, boolean background, boolean success) {
        commandFeedback(() -> text,background,success);
    }
    private void commandFeedback(Supplier<String> text,boolean background,boolean success) {
        if (!background || !success) setMessage(text);
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
        if(incident.needsAcknowledgement()) return UiText.ACKNOWLEDGE_ERROR_DEVICE_STATE_WILL_THEN_BE_CHECKED.text();
        if(recovering || recoveryRequested) return UiText.CHECKING_DEVICE_STATE.text();
        if(!fresh()) return hasConnection()?UiText.CURRENT_STATUS_DATA_MISSING_CHECKING_CONNECTION.text():UiText.CONNECT_TO_THE_DEVICE_TO_ALIGN.text();
        if(uncertain) return UiText.COMMAND_EFFECT_UNCERTAIN_CHECK_DEVICE_STATE.text();
        if(!state.limitsKnown || !state.confirmed) return UiText.DEVICE_LIMITS_ARE_NOT_YET_CONFIRMED.text();
        if(state.state.equals("FAULT")) return faultDescription(state.lastError)+UiText.CHECK_DEVICE_STATE.text();
        if(state.manualStatusSupported && !state.manualStatusFresh(SystemClock.elapsedRealtime())) return UiText.CURRENT_MANUAL_ACTION_STATUS_MISSING_CHECKING_DEVICE_STATE.text();
        if(state.manualStatusSupported && state.manualStatus!=null && state.manualStatus.active() && !manualMoving && !manualTrigger)
            return UiText.THE_CONTROLLER_IS_STILL_PERFORMING_A_MANUAL_ACTION.text();
        if(jog.held) return UiText.JOG_ACTIVE_RELEASE_ARROW_TO_STOP.text();
        if(jog.stopping) return UiText.STOPPING_JOG.text();
        if(busy) return UiText.WAITING_FOR_COMMAND_ACKNOWLEDGEMENT.text();
        if(abortingPlan) return UiText.STOPPING_AND_RESETTING_JOB.text();
        if(preparingRest) return UiText.LOADING_RECOVERY_JOB_MOVEMENT_NOT_YET_ENABLED.text();
        if(debugPending || manualPreparing) return UiText.PREPARING_MOVEMENT_DIAGNOSTICS_HAVE_A_BOUNDED_WAIT_TIME.text();
        if(manualTrigger) return UiText.COMPLETING_CAMERA_TRIGGER.text();
        if(!state.navigation(SystemClock.elapsedRealtime())) return state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")?
            UiText.CAPTURE_IN_PROGRESS_PAUSE_OR_STOP_TO_ALIGN.text():state.state.equals("TIMELAPSE")?UiText.TIMELAPSE_IN_PROGRESS_STOP_FIRST.text():
            UiText.MOVEMENT_IN_STATE.text()+state.state+UiText.NOT_ENABLED_CHECK_DEVICE_STATE.text();
        return "";
    }
    static String faultDescription(String code) {
        if(code.equals("MOTION_STALLED")) return UiText.THE_AXIS_DID_NOT_MOVE_SUFFICIENTLY_TOWARDS_THE.text();
        if(code.equals("MOTION_TARGET_MISSED") || code.equals("MOTION_TARGET_MISSE")) return UiText.MOVEMENT_HAS_ENDED_BUT_THE_TARGET_POSITION_IS.text();
        if(code.equals("EXPOSURE_UNSTABLE")) return UiText.A_POSITION_CHANGE_WAS_DETECTED_DURING_CAPTURE_THE.text();
        if(code.equals("MOTION_NOT_QUIET") || code.equals("MOTION_SETTLE_TIMEOUT")) return UiText.THE_AXIS_DID_NOT_SETTLE_WITHIN_THE_TIME.text();
        if(code.equals("FAULT_PAN_SENSOR")) return UiText.THE_PAN_ENCODER_FEEDBACK_COULD_NOT_BE_READ.text();
        if(code.equals("FAULT_TILT_SENSOR")) return UiText.THE_TILT_ENCODER_FEEDBACK_COULD_NOT_BE_READ.text();
        if(code.equals("MANUAL_MOVE_TIMEOUT")) return UiText.THE_MANUAL_MOVEMENT_REACHED_ITS_TIME_LIMIT.text();
        return UiText.DEVICE_ERROR_3.text()+code;
    }
    private void saveIncident() {
        getSharedPreferences("incidents",MODE_PRIVATE).edit().putLong("id",incident.id).putString("key",incident.key)
            .putString("detail",incident.detail).putBoolean("active",incident.active).putBoolean("acknowledged",incident.acknowledged)
            .putString("detailDe",incidentDisplay==null?incident.detail:incidentDisplay.in(UiText.Language.GERMAN))
            .putString("detailEn",incidentDisplay==null?incident.detail:incidentDisplay.in(UiText.Language.ENGLISH))
            .putBoolean("requiresStop",incident.requiresStop).apply();
    }
    private void raiseIncident(String code,Supplier<String> detail) {
        raiseIncident(code,detail,true);
    }
    private void raiseIncident(String code,Supplier<String> text,boolean requiresStop) {
        UiText.Message display=UiText.capture(text);
        String detail=display.text();
        String key=(device==null?"":device.getAddress())+"|"+code;
        if(incident.active && incident.key.equals(key) && incident.detail.equals(detail)) return;
        incidentDisplay=display;
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
        else setMessage(() -> hasConnection()?UiText.WAITING_FOR_CURRENT_DEVICE_CONNECTION.text():UiText.CONNECT_TO_THE_DEVICE_BEFORE_CHECKING.text());
        notifyObservers();
    }
    private void beginRecovery() {
        if(!uncertain && !incident.requiresStop && fresh() && state.limitsKnown && state.confirmed && !state.state.equals("FAULT")) {
            recoveryRequested=false; incident.resolved(); saveIncident(); targetKnown=false;
            setMessage(() -> UiText.DEVICE_STATE_RECONCILED_JOB.text()+state.runId+UiText.IN_STATE.text()+state.state+UiText.RETAINED.text());
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
                    if(flow.done()) { incident.resolved(); saveIncident(); setMessage(() -> UiText.DEVICE_STATE_CONFIRMED_OLD_MOVEMENT_TARGET_DISCARDED.text()); }
                    else setMessage(() -> state.state.equals("FAULT")?faultDescription(state.lastError)+UiText.AUTOMATIC_CHECK_IN_10_S.text():UiText.DEVICE_STATE_NOT_YET_CONFIRMED_AUTOMATIC_CHECK_IN.text());
                    support.append("RECOVERY "+(flow.done()?"CONFIRMED":"WAITING")); notifyObservers(); return;
                }
                if(next!=null) {
                    setMessage(() -> UiText.CHECKING_DEVICE_STATE.text());
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
        state = new DeviceState(); setConnection(() -> UiText.NOT_CONNECTED.text());
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); notifyObservers();
    }
    private void requireReady() {
        if (!fresh() || session == null) throw new IllegalArgumentException(UiText.CURRENT_DEVICE_CONNECTION_MISSING.text());
        if(incident.needsAcknowledgement() || uncertain || recovering || recoveryRequested) throw new IllegalArgumentException(navigationReason());
        if(state.manualStatusSupported && (!state.manualStatusFresh(SystemClock.elapsedRealtime()) ||
            state.manualStatus.active() && !manualMoving && !manualTrigger)) throw new IllegalArgumentException(navigationReason());
        if (busy) throw new IllegalArgumentException(UiText.COMMAND_IS_STILL_AWAITING_ACKNOWLEDGEMENT.text());
        if (preparingRest) throw new IllegalArgumentException(UiText.PREPARING_RECOVERY_JOB.text());
        if (abortingPlan) throw new IllegalArgumentException(UiText.ABORTING_AND_RESETTING_JOB.text());
        if (manualPreparing) throw new IllegalArgumentException(UiText.PREPARING_MOVEMENT_2.text());
        if (debugPending) throw new IllegalArgumentException(UiText.DEVICE_LOG_IS_STILL_BEING_TRANSFERRED.text());
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
            unavailable.completeExceptionally(new IOException(session==null?UiText.NO_DEVICE_CONNECTION.text():UiText.ANOTHER_ACTION_IS_AWAITING_ACKNOWLEDGEMENT.text())); return unavailable;
        }
        int sessionId = generation, actionId = ++actionGeneration;
        if (type.equals("GET_DEBUG_LOG")) {
            int requestId=++debugRequestId;
            debugPending = true;
            ui.postDelayed(() -> {
                if (sessionId == generation && debugPending && requestId == debugRequestId) {
                    debugPending=false; cancelPreparation();
                    setMessage(() -> UiText.DIAGNOSTIC_LOG_INCOMPLETE_QUERY_ENDED_DEVICE_STATE_REMAINS.text()); notifyObservers();
                }
            },5000);
        }
        busy = true; commandFeedback(() -> actionName(type) + UiText.IS_BEING_SENT.text(), background, true); support.append("TX " + type + " " + String.join("|", fields));
        CompletableFuture<V2Client.Result> wireResult;
        try { wireResult=emergency?session.emergencyStop(""+state.runId):jogStop?session.stopJog(fields[0]):session.send(type,fields); }
        catch(RuntimeException ex) { wireResult=new CompletableFuture<>(); wireResult.completeExceptionally(ex); }
        CompletableFuture<V2Client.Result> result = new CompletableFuture<>();
        wireResult.whenComplete((reply, error) -> ui.post(() -> {
            if (sessionId != generation || actionId != actionGeneration) {
                result.completeExceptionally(new IOException(UiText.OLD_ACTION_DISCARDED.text())); return;
            }
            busy = false;
            if(error!=null && !CommandEffects.readOnly(type) && !(error instanceof IllegalArgumentException)) {
                uncertain=true; raiseIncident("COMMAND_"+type,() -> UiText.THE_EFFECT_OF.text()+actionName(type)+UiText.WAS_NOT_CONFIRMED_CHECK_DEVICE_STATE.text());
            }
            if (type.equals("GET_DEBUG_LOG") && (error != null || !reply.accepted)) debugPending = false;
            commandFeedback(() -> error != null ? (CommandEffects.readOnly(type)?UiText.DEVICE_QUERY_NOT_COMPLETED_NO_NEW_MOVEMENT_SENT.text():UiText.COMMAND_EFFECT_UNCERTAIN_ACKNOWLEDGE_ERROR_AND_CHECK_DEVICE.text())
                : (reply.accepted ? UiText.CONFIRMED_2.text() : UiText.REJECTED.text()) + resultName(reply.code), background, error == null && reply.accepted);
            support.append(error == null ? "RESULT " + type + " " + reply.code : "ERROR " + type + " " + error.getMessage());
            if (emergency && error == null && reply.accepted) { manualMoving = manualTrigger = targetKnown = uncertain = false; }
            if (error == null) result.complete(reply); else result.completeExceptionally(error);
            notifyObservers();
        }));
        notifyObservers(); return result;
    }
    private static String actionName(String type) {
        switch (type) {
            case "MANUAL_JOG_START": return UiText.JOG.text();
            case "MANUAL_JOG_START_VELOCITY": return UiText.JOG.text();
            case "MANUAL_JOG_KEEPALIVE": return UiText.BUTTON_SIGNAL.text();
            case "MANUAL_JOG_STOP": return UiText.STOP_MOVEMENT.text();
            case "MANUAL_MOVE": return UiText.MOVEMENT_TARGET.text(); case "TRIGGER_ONCE": return UiText.CAMERA_TRIGGER.text();
            case "LOAD_PLAN": return UiText.PANORAMA_JOB.text(); case "START": return "Start";
            case "PAUSE": return "Pause"; case "RESUME": return UiText.RESUME.text();
            case "STOP": return "Stop"; case "ESTOP": return UiText.EMERGENCY_STOP.text();
            case "START_TIMELAPSE": return UiText.START_TIMELAPSE_3.text(); case "STOP_TIMELAPSE": return UiText.STOP_TIMELAPSE_2.text();
            case "CLEAR_RUN": return UiText.RESET_JOB.text(); case "UPDATE_TIMING": return UiText.TIMING_CHANGE.text();
            case "REWIND_COLUMNS": return UiText.COLUMN_REWIND.text(); case "FORWARD_COLUMNS": return UiText.COLUMN_ADVANCE.text();
            case "SET_TILT_LIMITS": return UiText.TILT_LIMITS.text();
            case "SET_CAPTURE_WARNING_TOLERANCE": return UiText.SAVE_WARNING_THRESHOLDS.text();
            case "SET_CAPTURE_STABILITY": return UiText.SAVE_STABILITY_LIMITS.text();
            case "GET_CAPTURE_STABILITY": return UiText.READ_STABILITY_LIMITS.text();
            case "RETRY_COLUMN": return UiText.PREPARE_COLUMN_REPEAT.text();
            case "SET_TILT_ZERO": return UiText.TILT_ZERO_POINT.text(); case "RESET_TILT_ZERO": return UiText.FACTORY_TILT_REFERENCE.text();
            default: return UiText.DEVICE_QUERY.text();
        }
    }
    private static String resultName(String code) {
        switch (code) {
            case "JOGGING": return UiText.JOG_ACTIVE.text();
            case "JOG_ALIVE": return UiText.BUTTON_HELD.text();
            case "JOG_STOPPED": return UiText.JOG_STOPPED.text();
            case "JOG_RELEASED": return UiText.JOG_ENDED_2.text();
            case "JOG_NOT_READY": return UiText.AXIS_SETTLING_OR_LIMIT_REACHED_RELEASE_BUTTON_AND.text();
            case "JOG_SPEED_LIMIT": return UiText.JOG_SPEED_EXCEEDS_THE_DEVICE_MAXIMUM_REDUCE_SPEED.text();
            case "OK": return UiText.DEVICE_QUERY_ACCEPTED.text(); case "MOVING": return UiText.MOVEMENT_TARGET_ACCEPTED.text();
            case "TRIGGERED": return UiText.CAMERA_PULSE_STARTED.text(); case "PLAN_ACCEPTED": return UiText.PLAN_ACCEPTED.text();
            case "STARTED": return UiText.JOB_STARTED.text(); case "PAUSED": return UiText.PAUSED.text();
            case "PAUSE_PENDING": return UiText.PAUSE_REQUESTED_AFTER_CURRENT_IMAGE.text(); case "RESUMED": return UiText.JOB_RESUMED.text();
            case "STOPPED": return UiText.STOPPED.text(); case "CLEARED": return UiText.JOB_CLEARED.text();
            case "TIMING_UPDATED": return UiText.NEW_TIMING_APPLIED.text(); case "TIMING_QUEUED": return UiText.NEW_TIMING_QUEUED.text();
            case "WARNING_TOLERANCE_SAVED": return UiText.WARNING_THRESHOLDS_SAVED_PERMANENTLY_TO_DEVICE.text();
            case "STABILITY_SETTINGS_SAVED": return UiText.STABILITY_LIMITS_SAVED_WAITING_FOR_READBACK.text();
            case "COLUMN_RETRY_READY": return UiText.COLUMN_PREPARED_PRESS_RESUME_SEPARATELY_TO_MOVE.text();
            case "INVALID_STATE": return UiText.UNAVAILABLE_IN_THE_CURRENT_DEVICE_STATE.text();
            case "MANUAL_ACTION_ACTIVE": return UiText.MANUAL_ACTION_STILL_ACTIVE.text();
            case "SAFETY_UNCONFIRMED": return UiText.SAFETY_LIMITS_NOT_CONFIRMED.text();
            case "PAN_LIMIT": return UiText.PAN_LIMIT_EXCEEDED.text(); case "TILT_LIMIT": return UiText.TILT_LIMIT_EXCEEDED.text();
            case "VALUE_OUT_OF_RANGE": return UiText.VALUE_OUTSIDE_THE_ALLOWED_RANGE.text();
            case "TILT_ZERO_SAVED": return UiText.TILT_ZERO_POINT_SAVED.text();
            case "STORE_FAILED": return UiText.SAVING_TO_DEVICE_FAILED.text();
            case "CALIBRATION_UNSUPPORTED": return UiText.DEVICE_DOES_NOT_SUPPORT_A_TILT_ZERO_POINT.text();
            case "SEQUENCE_CONFLICT": return UiText.COMMAND_NUMBER_ALREADY_USED_ACTION_REJECTED.text();
            default: return code;
        }
    }
    public void emergencyStop() {
        cancelRest();
        jog.finish(jog.token); ui.removeCallbacks(jogAlive);
        recoveryFlow=null; recovering=recoveryRequested=false;
        planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        if (!connected || session == null) throw new IllegalArgumentException(UiText.NO_BLUETOOTH_CONNECTION_FOR_EMERGENCY_STOP.text());
        cancelPreparation(); manualProgress = null; debugPending = false;
        manualAction=null;
        execute("ESTOP", new String[]{""+state.runId, "ANDROID_ESTOP"}, true);
    }
    public void setTiltZero(boolean reset) {
        requireReady();
        if(!state.tiltCalibrationKnown || !state.positionSafe() || !state.state.equals("IDLE") || manualMoving || manualTrigger)
            throw new IllegalArgumentException(UiText.SET_TILT_ZERO_POINT_ONLY_WHILE_STATIONARY_WITH.text());
        int id=generation;
        command(reset?"RESET_TILT_ZERO":"SET_TILT_ZERO").whenComplete((reply,error) -> {
            if(id==generation && error==null && reply.accepted) {
                markDraftForReview(); targetKnown=false; state.lastStatusMs=0;
                setMessage(() -> UiText.TILT_ZERO_POINT_SAVED_CHECK_DRAFT_REFERENCE.text()); notifyObservers();
                // A cached ACK after a retry does not repeat calibration metadata.
                execute("GET_SUPPORT_BUNDLE",new String[0],false,true);
            }
        });
    }
    public void move(double panDelta, double tiltDelta) {
        requireReady();
        if(manualMoving || jog.held || jog.stopping) throw new IllegalArgumentException(UiText.FINISH_CURRENT_MOVEMENT_FIRST.text());
        if (!navigationAllowed()) throw new IllegalArgumentException(navigationReason());
        int dx = DeviceState.toDevice(panDelta), dy = DeviceState.toTiltDevice(tiltDelta);
        int x = ManualTargets.step(targetKnown && state.positionSafe() ? targetPan : state.pan, state.pan, dx);
        int y = ManualTargets.step(targetKnown && state.positionSafe() ? targetTilt : state.tilt, state.tilt, dy);
        x=DeviceState.manualStep(state.pan,x,state.panMin,state.panMax);
        y=DeviceState.manualStep(state.tilt,y,state.tiltMin,state.tiltMax);
        if(x==state.pan && y==state.tilt) throw new IllegalArgumentException(UiText.LIMIT_REACHED_MOVE_BACK_IN_THE_OPPOSITE_DIRECTION.text());
        prepareMove(x,y);
    }
    public void setManualTempo(int tempo) {
        if(tempo<1 || tempo>1000) throw new IllegalArgumentException(UiText.MOVEMENT_SPEED_MUST_BE_1_1000.text());
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("manualSpeed",""+tempo).apply();
    }
    public void setManualTempos(int panTempo,int tiltTempo) {
        if(panTempo<1 || panTempo>1000 || tiltTempo<1 || tiltTempo>1000)
            throw new IllegalArgumentException(UiText.PAN_TILT_TARGET_SPEED_MUST_BE_1_1000.text());
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
            throw new IllegalArgumentException(UiText.JOG_SPEED_0_5_TO.text()+(maximum/1000.0)+UiText.S_DEVICE_MAXIMUM_ADJUSTABLE_IN_MOTOR_CONFIGURATION.text());
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString(axis==0?"panJogSpeed":"jogSpeed",Double.toString(speed)).apply();
    }
    public void setRememberSettleMillis(int millis) {
        if(millis<0 || millis>5000) throw new IllegalArgumentException(UiText.SETTLING_BEFORE_REMEMBERING_POSITION_0_TO_5000_MS.text());
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
        if(!jogAllowed()) throw new IllegalArgumentException(state.jogSupported?UiText.FINISH_MOVEMENT_FIRST.text():UiText.UPDATE_FIRMWARE_FOR_HOLD_TO_MOVE_CONTROL.text());
        boolean velocityMode=state.jogMaximumVelocityMdeg>0;
        int maximum=axis==0 && state.jogMaximumPanVelocityMdeg>0?state.jogMaximumPanVelocityMdeg:state.jogMaximumVelocityMdeg;
        String defaultSpeed=axis==0?Double.toString(Math.min(10,maximum/1000.0)):"5";
        double requested=velocityMode?Double.parseDouble(getSharedPreferences("settings",MODE_PRIVATE).getString(axis==0?"panJogSpeed":"jogSpeed",defaultSpeed)):0;
        if(velocityMode && (!Double.isFinite(requested) || requested<0.5 || requested>maximum/1000.0))
            throw new IllegalArgumentException(UiText.JOG_SPEED_EXCEEDS_DEVICE_MAXIMUM_ADJUST_SPEED.text());
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
            else { jog.sequence=reply.commandSequence; setMessage(() -> UiText.HOLD_TO_MOVE_RELEASE_TO_STOP.text()); }
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
                setMessage(() -> UiText.JOG_STOPPED_2.text());
                deferEndpointCapture();
            }
            notifyObservers();
        });
    }
    public void moveToUi(double pan,double tilt) {
        requireReady();
        if(manualMoving) throw new IllegalArgumentException(UiText.WAIT_FOR_ARRIVAL_BEFORE_THE_NEXT_GRAPHICAL_MOVE.text());
        prepareMove(DeviceState.toDevice(pan),DeviceState.toTiltDevice(tilt));
    }
    private void prepareMove(int x,int y) {
        requireReady();
        if(!navigationAllowed()) throw new IllegalArgumentException(UiText.MOVEMENT_NOT_ENABLED_IN_THIS_DEVICE_STATE.text());
        state.validateManualTarget(x, y);
        if(state.manualStatusSupported) { sendManualMove(x,y); return; }
        int id = generation;
        manualPreparing = true; preparationAcknowledged = false; manualBaseline = -1;
        setMessage(() -> UiText.PREPARING_MOVEMENT.text());
        pendingManual = () -> {
            if (!navigationAllowed()) { setMessage(() -> UiText.MOVEMENT_NOT_STARTED_CURRENT_PERMISSION_MISSING.text()); return; }
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
            if (error != null || !reply.accepted) { cancelPreparation(); setMessage(() -> UiText.MOVEMENT_NOT_STARTED_DEVICE_LOG_UNAVAILABLE.text()); }
            else { preparationAcknowledged = true; finishPreparation(); }
        });
        ui.postDelayed(() -> {
            if (id == generation && pendingManual == prepared) {
                cancelPreparation(); setMessage(() -> UiText.MOVEMENT_NOT_STARTED_DEVICE_LOG_INCOMPLETE.text()); notifyObservers();
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
                uncertain=true; raiseIncident("MANUAL_CONFIRMATION",() -> UiText.MOVEMENT_ACKNOWLEDGEMENT_WITHOUT_VALID_COMMAND_NUMBER_CHECK_DEVICE.text()); notifyObservers(); return;
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
        try { action.run(); } catch(RuntimeException ex) { setMessage(() -> UiText.MOVEMENT_NOT_STARTED.text()+ex.getMessage()); notifyObservers(); }
    }
    public void trigger(int pulse) {
        requireReady();
        if (!manualAllowed() || !state.positionSafe() || manualMoving || pulse < 1 || pulse > 60000) {
            throw new IllegalArgumentException(UiText.CAMERA_TEST_ONLY_WHILE_STATIONARY_PULSE_1_60000.text());
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
                    uncertain=true; raiseIncident("MANUAL_CONFIRMATION",() -> UiText.TRIGGER_ACKNOWLEDGEMENT_WITHOUT_VALID_COMMAND_NUMBER_CHECK_DEVICE.text());
                } else {
                    manualAction=new ManualActionProgress(reply.commandSequence,"TRIGGER",0,0,state.manualStatusVersion); updateManualAction();
                }
            }
            notifyObservers();
        });
    }
    public void loadPlan(PanoramaPlan plan) {
        requireReady();
        if(draftNeedsReview) throw new IllegalArgumentException(UiText.CHECK_AND_CONFIRM_DRAFT_REFERENCE_AFTER_CHANGING_DEVICE.text());
        if (!state.state.equals("IDLE")) throw new IllegalArgumentException(UiText.DELIBERATELY_CLEAR_THE_EXISTING_RUN_BEFORE_LOADING_A.text());
        if (manualMoving || manualTrigger) throw new IllegalArgumentException(UiText.MANUAL_ACTION_STILL_ACTIVE.text());
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
            default: throw new IllegalArgumentException(UiText.UNKNOWN_RUN_ACTION.text());
        }
        if (!valid) throw new IllegalArgumentException(UiText.ACTION_IN_STATE.text() + s + UiText.UNAVAILABLE.text());
        if(type.equals("CLEAR_RUN") && s.equals("PLAN_READY")) { abortPlan(); return; }
        if ((type.equals("START") || type.equals("RESUME")) && !state.positionSafe())
            throw new IllegalArgumentException(UiText.REPORTED_ACTUAL_POSITION_OUTSIDE_CONFIRMED_LIMITS_CHECK_ENCODER.text());
        if (type.equals("CLEAR_RUN")) command(type); else command(type, ""+state.runId);
    }
    public void abortPlan() {
        abortPlan(false);
    }
    private void markDraftForReview() {
        draftNeedsReview=true; getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("draftNeedsReview",true).apply();
    }
    public void confirmDraftReference() {
        if(!fresh() || !state.limitsKnown || !state.confirmed) throw new IllegalArgumentException(UiText.READ_CURRENT_REFERENCE_AND_LIMITS_FROM_THE_DEVICE.text());
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
                    planAbort=null; abortingPlan=false; setMessage(() -> UiText.ABORT_NOT_FULLY_CONFIRMED_CHECK_DEVICE_STATE.text()); notifyObservers(); return;
                }
                String command=progress.next(state.state,fresh());
                if(progress.done()) {
                    planAbort=null; abortingPlan=false; manualMoving=manualTrigger=targetKnown=false;
                    if(discardDraftAfterAbort) { discardDraft(); setMessage(() -> UiText.PLAN_REMOVED_READY_FOR_NEW_PLAN.text()); }
                    else setMessage(() -> UiText.JOB_ABORTED_READY_FOR_NEW_PLAN.text());
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
        if (!state.positionSafe()) throw new IllegalArgumentException(UiText.ACTUAL_POSITION_OUTSIDE_SAFETY_LIMITS.text());
        if (state.plan == null || state.plan.mode != 0 || !(state.state.equals("PAUSED") || state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")))
            throw new IllegalArgumentException(UiText.COLUMN_CHANGES_REQUIRE_A_RUNNING_PAUSED_GRID.text());
        command(forward ? "FORWARD_COLUMNS" : "REWIND_COLUMNS", ""+state.runId, "1");
    }
    int[] pendingTiming;
    long pendingTimingRun;
    String timingSummary() {
        PanoramaPlan p=state.plan;
        if(p==null) return UiText.NO_DEVICE_PLAN_YET.text();
        if(pendingTiming!=null && (pendingTimingRun!=state.runId ||
            (p.settleMs==pendingTiming[0] && p.pulseMs==pendingTiming[1] && p.exposureMs==pendingTiming[2]))) pendingTiming=null;
        if(pendingTiming!=null && pendingTimingRun==state.runId &&
            (p.settleMs!=pendingTiming[0] || p.pulseMs!=pendingTiming[1] || p.exposureMs!=pendingTiming[2]) &&
            (state.state.equals("RUNNING") || state.state.equals("PAUSE_PENDING")))
            return UiText.QUEUED_AFTER_CURRENT_IMAGE_SETTLING.text()+pendingTiming[0]+UiText.MS_PULSE.text()+pendingTiming[1]+UiText.MS_EXPOSURE.text()+pendingTiming[2]+" ms";
        return UiText.ACTIVE_SETTLING.text()+p.settleMs+UiText.MS_PULSE.text()+p.pulseMs+UiText.MS_EXPOSURE.text()+p.exposureMs+" ms";
    }
    public void saveWarningTolerances(double pan,double tilt) {
        requireReady();
        if(!state.warningSettingsSupported || manualMoving || manualTrigger || jog.held || jog.stopping ||
            !(state.state.equals("IDLE") || state.state.equals("STOPPED")))
            throw new IllegalArgumentException(UiText.SAVE_WARNING_THRESHOLDS_IN_READY_OR_STOPPED_STATE.text());
        if(!Double.isFinite(pan) || !Double.isFinite(tilt) || pan<.044 || pan>10 || tilt<.044 || tilt>10)
            throw new IllegalArgumentException(UiText.WARNING_THRESHOLD_0_044_TO_10_DEGREES.text());
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
            pendingStability=null;setMessage(() -> UiText.STABILITY_LIMITS_PERMANENTLY_SAVED_AND_READ_BACK.text());
        }
    }
    public void readCaptureStability() {
        requireReady();
        if(state.captureStability==null) throw new IllegalArgumentException(UiText.THIS_FIRMWARE_DOES_NOT_SUPPORT_STABILITY_SETTINGS_IN.text());
        command("GET_CAPTURE_STABILITY");
    }
    public void saveCaptureStability(double span,double pan,double tilt,double seconds) {
        requireReady();
        if(!canConfigureStability()) throw new IllegalArgumentException(UiText.SAVE_STABILITY_LIMITS_WITH_THE_HEAD_STATIONARY_WITH.text());
        CaptureStabilitySettings request=CaptureStabilitySettings.fromUi(span,pan,tilt,seconds,state.motionConfigRevision);
        pendingStability=request;int id=generation;
        command("SET_CAPTURE_STABILITY",request.fields()).whenComplete((reply,error) -> {
            if(id!=generation) return;
            if(error!=null || !reply.accepted) {pendingStability=null;return;}
            if(request.sameValues(state.captureStability) && state.motionConfigRevision>=request.revision) {
                pendingStability=null;setMessage(() -> UiText.STABILITY_LIMITS_PERMANENTLY_SAVED_AND_READ_BACK.text());notifyObservers();
            } else if(pendingStability==request) {
                execute("GET_CAPTURE_STABILITY",new String[0],false,true);
                ui.postDelayed(() -> {
                    if(id==generation && pendingStability==request) {
                        pendingStability=null;setMessage(() -> UiText.SAVE_CONFIRMED_READBACK_MISSING_READ_STABILITY_LIMITS_AGAIN.text());notifyObservers();
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
            throw new IllegalArgumentException(UiText.PAUSED_JOB_HAS_CHANGED_SELECT_AGAIN.text());
        command("RETRY_COLUMN",""+state.runId,previous?"1":"0");
    }
    public void updateTiming(int settle, int pulse, int exposure) {
        requireReady();
        if (state.plan == null || settle < 0 || settle > 600000 || pulse < 1 || pulse > 60000 || exposure < 0 || exposure > 3600000)
            throw new IllegalArgumentException(UiText.INVALID_TIMING_PARAMETERS_OR_JOB.text());
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
            throw new IllegalArgumentException(UiText.TIMELAPSE_REQUIRES_IDLE_A_VALID_INTERVAL_AND_A.text());
        command("START_TIMELAPSE", ""+id, ""+interval, ""+pulse);
    }
    @Override public void onDestroy() { jog.finish(jog.token); ui.removeCallbacks(jogAlive); ++generation; ++actionGeneration;
        disconnect(); ui.removeCallbacksAndMessages(null); if(warningTone!=null) warningTone.release();
        exportServiceActive=false;
        captures.onExportChanged(key -> { }); if(livePapywizard!=null) livePapywizard.close();
        captures.close(); support.close(); observers.clear(); super.onDestroy();
    }
}
