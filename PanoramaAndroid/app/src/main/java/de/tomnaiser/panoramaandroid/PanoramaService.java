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
    private BluetoothSession session;
    private BluetoothDevice device;
    private String plotDevice="";
    private int generation, actionGeneration;
    private PowerManager.WakeLock wakeLock;
    public DeviceState state = new DeviceState();
    final RunPlotModel plot = new RunPlotModel();
    public String connection = "Nicht verbunden", message = "Bereit", diagnostics = "Noch keine Diagnose";
    public boolean connected, busy, manualMoving, manualTrigger, uncertain;
    public boolean abortingPlan;
    private PlanAbort planAbort;
    private boolean discardDraftAfterAbort;
    public int draftRevision;
    public final Map<String,String> draftEndpoints=new LinkedHashMap<>();
    private String draftDevice="";
    private DeviceSession deviceSession;
    private boolean sessionIdentityReceived;
    private int targetPan, targetTilt;
    private boolean targetKnown;
    private boolean manualPreparing, preparationAcknowledged;
    private int manualBaseline = -1;
    private Runnable pendingManual;
    private ManualMoveProgress manualProgress;
    private int newestDebugSequence;
    private boolean debugPending;
    private long nextDiagnosticMs, openedMs;
    private long lastStatusLoggedMs;
    public CaptureStore captures;
    public SupportLog support;
    private final Runnable retry = () -> open();
    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            if (connected && session != null) {
                long now = SystemClock.elapsedRealtime();
                if (now - (state.lastStatusMs == 0 ? openedMs : state.lastStatusMs) > 5000) {
                    session.fail("Keine aktuellen Statusdaten seit 5 Sekunden");
                } else if (manualMoving && !manualPreparing && !debugPending && !busy && now > nextDiagnosticMs) {
                    nextDiagnosticMs = now + 1000;
                    execute("GET_DEBUG_LOG", new String[]{""+newestDebugSequence}, false, true);
                }
            }
            notifyObservers(); ui.postDelayed(this, 500);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        captures = new CaptureStore(this); support = new SupportLog(getFilesDir());
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
        int id = ++generation; ++actionGeneration;
        String address=device.getAddress();
        if(!draftDevice.isEmpty() && !draftDevice.equals(address)) discardDraft();
        draftDevice=address;
        deviceSession=new DeviceSession(getSharedPreferences("settings",MODE_PRIVATE).getLong("session."+address,-1));
        sessionIdentityReceived=false;
        state = new DeviceState(); connected = busy = manualMoving = manualTrigger = targetKnown = uncertain = false;
        cancelPreparation(); manualProgress = null;
        debugPending = false; newestDebugSequence = 0;
        connection = "Bluetooth-Verbindung wird aufgebaut …";
        session = new BluetoothSession(new BluetoothSession.Listener() {
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
                            discardDraft(); targetKnown=false; state.lastStatusMs=0;
                            message="Tilt-Nullpunkt geändert · Plan geleert";
                        }
                        settings.edit().putInt(key,state.tiltZeroOffset).apply();
                    }
                    if(frame.type.equals("DEVICE_SESSION")) {
                        if(frame.fields.length!=1) throw new IllegalArgumentException("Gerätesitzung unvollständig");
                        long boot=DeviceState.unsigned(frame.fields[0]);
                        boolean reset=deviceSession.accept(boot);
                        getSharedPreferences("settings",MODE_PRIVATE).edit().putLong("session."+device.getAddress(),boot).apply();
                        if(reset) {
                            discardDraft(); message="Gerät neu gestartet · Plan geleert";
                            state.lastStatusMs=0; state.plan=null; targetKnown=false;
                            cancelPreparation(); manualProgress=null; manualMoving=manualTrigger=false;
                            planAbort=null; abortingPlan=discardDraftAfterAbort=false;
                            if(sessionIdentityReceived) {
                                ++actionGeneration; busy=false; uncertain=false;
                                session.fail("Gerät neu gestartet · Plan geleert");
                                notifyObservers(); return;
                            }
                        }
                        sessionIdentityReceived=true;
                    }
                    if (frame.type.equals("STATUS") && !previousState.equals(state.state)) targetKnown = false;
                    captures.accept(device.getAddress(), state, frame);
                    if(frame.type.equals("STATUS")) plot.status(frame,SystemClock.elapsedRealtime());
                    if(frame.type.equals("IMAGE_COMPLETED") || frame.type.equals("MANUAL_IMAGE_TRIGGERED") ||
                        (frame.type.equals("STATUS") && (previousRun!=state.runId || previousCount!=state.completed || !previousState.equals(state.state))))
                        plot.captures=captures.plotCaptures(device.getAddress(),state.runId);
                    if (frame.type.equals("STATUS") && SystemClock.elapsedRealtime()-lastStatusLoggedMs>=5000) {
                        lastStatusLoggedMs=SystemClock.elapsedRealtime(); support.append("STATUS " + String.join("|",frame.fields));
                    }
                    if (!frame.type.equals("STATUS")) support.append("RX " + P2Codec.encode(Math.max(1, frame.sequence), frame.type, frame.fields).trim());
                    if (frame.type.equals("STATUS") && !targetKnown) { targetPan = state.pan; targetTilt = state.tilt; targetKnown = true; }
                    if (frame.type.equals("STATUS") && state.state.equals("FAULT")) { cancelPreparation(); manualProgress = null; manualMoving = manualTrigger = false; message = "Gerätefehler: " + state.lastError; }
                    if (frame.type.equals("MANUAL_IMAGE_TRIGGERED")) { manualTrigger = false; message = "Manuelle Aufnahme abgeschlossen"; }
                    if (frame.type.equals("DIAGNOSTICS")) readDiagnostics(frame.fields);
                    if (frame.type.equals("DEBUG_LOG_END")) {
                        newestDebugSequence = Integer.parseInt(frame.fields[0]); debugPending = false;
                        if (manualPreparing) { manualBaseline = newestDebugSequence; finishPreparation(); }
                    }
                    if (frame.type.equals("DEBUG_EVENT")) acceptManualEvent(frame.fields);
                } catch (IllegalArgumentException | IOException ex) { message = "Daten-/Speicherfehler: " + ex.getMessage(); }
                notifyObservers();
            }
            @Override public void onDisconnected(String reason) {
                if (id != generation) return;
                plot.disconnected();
                session = null; connected = busy = false; ++generation; ++actionGeneration;
                manualMoving = manualTrigger = targetKnown = false;
                cancelPreparation(); manualProgress = null;
                debugPending = false;
                state = new DeviceState();
                int attempt = reconnect.nextAttempt();
                if (attempt > 0) { connection = reason + " · Neuer Versuch " + attempt + "/3 in 3 s"; ui.postDelayed(retry, 3000); }
                else { disconnect(); connection = reason + " · Erneut verbinden"; }
                notifyObservers();
            }
        });
        session.connect(device); notifyObservers();
    }
    private void readDiagnostics(String[] f) {
        if (f.length < 21) throw new IllegalArgumentException("Diagnose unvollständig");
        int stable = Integer.parseInt(f[5]), required = Integer.parseInt(f[6]);
        diagnostics = "Pan-Fehler: " + f[11] + " m° · Tilt-Fehler: " + f[12] + " m°\nStabile Samples: "
            + stable + "/" + required + " · RAM: " + f[19] + " / Minimum " + f[20];
    }
    void acceptManualEvent(String[] fields) {
        if (manualMoving && manualProgress != null && manualProgress.accept(fields)) {
            manualMoving = false; manualProgress = null; message = "Manuelles Ziel erreicht";
        }
    }
    void commandFeedback(String text, boolean background, boolean success) {
        if (!background || !success) message = text;
    }
    public boolean fresh() { return connected && !uncertain && state.fresh(SystemClock.elapsedRealtime()); }
    public boolean manualAllowed() { return fresh() && state.limitsKnown && state.confirmed && !busy && !abortingPlan && !debugPending && !manualPreparing && !manualTrigger && state.manual(SystemClock.elapsedRealtime()); }
    public boolean navigationAllowed() { return fresh() && state.limitsKnown && state.confirmed && !busy && !abortingPlan && !debugPending && !manualPreparing && !manualTrigger && state.navigation(SystemClock.elapsedRealtime()); }
    public boolean hasConnection() { return device != null; }
    public void disconnect() {
        planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        plot.disconnected();
        ui.removeCallbacks(retry); reconnect.stop(); ++generation; ++actionGeneration;
        BluetoothSession old = session; session = null;
        if (old != null) old.close();
        device = null; connected = busy = manualMoving = manualTrigger = targetKnown = false;
        cancelPreparation(); manualProgress = null;
        debugPending = false;
        state = new DeviceState(); connection = "Nicht verbunden";
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); notifyObservers();
    }
    private void requireReady() {
        if (!fresh() || session == null) throw new IllegalArgumentException("Aktuelle Geräteverbindung fehlt");
        if (busy) throw new IllegalArgumentException("Befehl wird noch bestätigt");
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
        int sessionId = generation, actionId = ++actionGeneration;
        if (type.equals("GET_DEBUG_LOG")) {
            debugPending = true;
            ui.postDelayed(() -> {
                if (sessionId == generation && debugPending && actionId == actionGeneration) {
                    debugPending = false; uncertain = true;
                    message = "Gerätelog unvollständig; erneut verbinden"; notifyObservers();
                }
            },5000);
        }
        busy = true; commandFeedback(actionName(type) + " wird gesendet …", background, true); support.append("TX " + type + " " + String.join("|", fields));
        CompletableFuture<V2Client.Result> wireResult = emergency ? session.emergencyStop(""+state.runId) : session.send(type, fields);
        CompletableFuture<V2Client.Result> result = new CompletableFuture<>();
        wireResult.whenComplete((reply, error) -> ui.post(() -> {
            if (sessionId != generation || actionId != actionGeneration) {
                result.completeExceptionally(new IOException("Alte Aktion verworfen")); return;
            }
            busy = false;
            if (error != null) uncertain = true;
            if (type.equals("GET_DEBUG_LOG") && (error != null || !reply.accepted)) debugPending = false;
            commandFeedback(error != null ? "Keine eindeutige Bestätigung; erneut verbinden oder Not-Stop senden."
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
            case "MANUAL_MOVE": return "Fahrziel"; case "TRIGGER_ONCE": return "Kameraauslösung";
            case "LOAD_PLAN": return "Panoramaauftrag"; case "START": return "Start";
            case "PAUSE": return "Pause"; case "RESUME": return "Fortsetzen";
            case "STOP": return "Stop"; case "ESTOP": return "Not-Stop";
            case "START_TIMELAPSE": return "Timelapse-Start"; case "STOP_TIMELAPSE": return "Timelapse-Stop";
            case "CLEAR_RUN": return "Auftrag zurücksetzen"; case "UPDATE_TIMING": return "Zeitänderung";
            case "REWIND_COLUMNS": return "Spaltenrücksprung"; case "FORWARD_COLUMNS": return "Spaltenvorsprung";
            case "SET_TILT_LIMITS": return "Tilt-Grenzen";
            case "SET_TILT_ZERO": return "Tilt-Nullpunkt"; case "RESET_TILT_ZERO": return "Tilt-Werkreferenz";
            default: return "Geräteabfrage";
        }
    }
    private static String resultName(String code) {
        switch (code) {
            case "OK": return "Geräteabfrage angenommen"; case "MOVING": return "Fahrziel angenommen";
            case "TRIGGERED": return "Kamera-Puls gestartet"; case "PLAN_ACCEPTED": return "Plan übernommen";
            case "STARTED": return "Auftrag gestartet"; case "PAUSED": return "Pausiert";
            case "PAUSE_PENDING": return "Pause nach dem aktuellen Bild angefordert"; case "RESUMED": return "Auftrag fortgesetzt";
            case "STOPPED": return "Gestoppt"; case "CLEARED": return "Auftrag gelöscht";
            case "TIMING_UPDATED": return "Neue Zeiten übernommen"; case "TIMING_QUEUED": return "Neue Zeiten vorgemerkt";
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
        planAbort=null; abortingPlan=false; discardDraftAfterAbort=false;
        if (!connected || session == null) throw new IllegalArgumentException("Keine Bluetooth-Verbindung für Not-Stop");
        cancelPreparation(); manualProgress = null; debugPending = false;
        execute("ESTOP", new String[]{""+state.runId, "ANDROID_ESTOP"}, true);
    }
    public void setTiltZero(boolean reset) {
        requireReady();
        if(!state.tiltCalibrationKnown || !state.positionSafe() || !state.state.equals("IDLE") || manualMoving || manualTrigger)
            throw new IllegalArgumentException("Tilt-Nullpunkt nur im Stillstand ohne Geräteauftrag setzen");
        int id=generation;
        command(reset?"RESET_TILT_ZERO":"SET_TILT_ZERO").whenComplete((reply,error) -> {
            if(id==generation && error==null && reply.accepted) {
                discardDraft(); targetKnown=false; state.lastStatusMs=0;
                message="Tilt-Nullpunkt gespeichert · Plan geleert"; notifyObservers();
                // A cached ACK after a retry does not repeat calibration metadata.
                execute("GET_SUPPORT_BUNDLE",new String[0],false,true);
            }
        });
    }
    public void move(double panDelta, double tiltDelta) {
        requireReady();
        if (!navigationAllowed()) throw new IllegalArgumentException("Fahrt während der Aufnahme gesperrt; geladene/beendete Aufträge benötigen Firmware 2.3.6");
        int dx = DeviceState.toDevice(panDelta), dy = DeviceState.toTiltDevice(tiltDelta);
        int x = ManualTargets.step(targetKnown && state.positionSafe() ? targetPan : state.pan, state.pan, dx);
        int y = ManualTargets.step(targetKnown && state.positionSafe() ? targetTilt : state.tilt, state.tilt, dy);
        x=DeviceState.manualStep(state.pan,x,state.panMin,state.panMax);
        y=DeviceState.manualStep(state.tilt,y,state.tiltMin,state.tiltMax);
        if(x==state.pan && y==state.tilt) throw new IllegalArgumentException("Grenze erreicht · in Gegenrichtung zurückfahren");
        prepareMove(x,y);
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
        int id = generation;
        manualPreparing = true; preparationAcknowledged = false; manualBaseline = -1;
        message = "Fahrt wird vorbereitet …";
        pendingManual = () -> {
            if (!navigationAllowed()) { message = "Fahrt nicht gestartet: aktuelle Freigabe fehlt"; return; }
            state.validateManualTarget(x,y);
            CompletableFuture<V2Client.Result> move = command("MANUAL_MOVE", ""+x, ""+y);
            int action = actionGeneration;
            move.whenComplete((reply,error) -> {
                if (id == generation && action == actionGeneration && error == null && reply.accepted) {
                    targetPan = x; targetTilt = y; targetKnown = true; manualMoving = true;
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
    private void cancelPreparation() { pendingManual = null; manualPreparing = false; preparationAcknowledged = false; }
    private void finishPreparation() {
        if (pendingManual == null || !preparationAcknowledged || manualBaseline < 0 || busy) return;
        Runnable action = pendingManual; cancelPreparation(); action.run();
    }
    public void trigger(int pulse) {
        requireReady();
        if (!manualAllowed() || !state.positionSafe() || manualMoving || pulse < 1 || pulse > 60000)
            throw new IllegalArgumentException("Kamera-Test erst im Stillstand; Puls 1–60000 ms");
        manualTrigger = true;
        int id = generation;
        command("TRIGGER_ONCE", ""+pulse).whenComplete((reply, error) -> ui.post(() -> {
            if (id == generation && error == null && !reply.accepted) manualTrigger = false;
        }));
    }
    public void loadPlan(PanoramaPlan plan) {
        requireReady();
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
    public void discardDraft() { draftEndpoints.clear(); draftRevision++; }
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
                        if(planAbort!=progress) return;
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
    public void updateTiming(int settle, int pulse, int exposure) {
        requireReady();
        if (state.plan == null || settle < 0 || settle > 600000 || pulse < 1 || pulse > 60000 || exposure < 0 || exposure > 3600000)
            throw new IllegalArgumentException("Zeitparameter oder Auftrag ungültig");
        command("UPDATE_TIMING", ""+state.runId, ""+settle, ""+pulse, ""+exposure);
    }
    public void timelapse(long id, int interval, int pulse) {
        requireReady();
        if (!state.state.equals("IDLE") || manualMoving || manualTrigger || id < 1 || id > 4294967295L
                || interval < 100 || interval > 3600000 || pulse < 1 || pulse > 60000 || pulse >= interval)
            throw new IllegalArgumentException("Timelapse benötigt IDLE, gültiges Intervall und kürzeren Puls");
        command("START_TIMELAPSE", ""+id, ""+interval, ""+pulse);
    }
    @Override public void onDestroy() {
        disconnect(); ui.removeCallbacksAndMessages(null); captures.close(); support.close(); observers.clear(); super.onDestroy();
    }
}
