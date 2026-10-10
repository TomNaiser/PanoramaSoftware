package de.tomnaiser.panoramaandroid;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.os.*;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.*;

/** Mobile client: all device actions pass through the session service. */
public final class MainActivity extends Activity {
    private static final long PROCESS_TOKEN=new SecureRandom().nextLong();
    private PanoramaService service;
    private boolean bound;
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final List<CameraProfile> profiles = new ArrayList<>();
    private final Map<Button,BooleanSupplier> gates = new LinkedHashMap<>();
    private final Map<String,EditText> inputs = new LinkedHashMap<>();
    private SharedPreferences preferences;
    private Spinner devicePicker, profilePicker, modePicker, runPicker;
    private Spinner finePicker;
    private Button tiltRangeButton;
    private Button alignmentReset;
    private LinearLayout navigationBar;
    private final List<Button> tabs=new ArrayList<>();
    private TextView endpointSummary, alignmentHint, runProgress, activeProfile;
    private TextView tiltReference;
    private String displayedTiltLimits="";
    private ProgressBar captureProgress;
    private static final int INK=Color.rgb(25,43,59), ACCENT=Color.rgb(18,105,116), MUTED=Color.rgb(83,103,119);
    private int tiltRange;
    private TextView connection, live, message, planSummary, diagnostics;
    private LinearLayout[] pages;
    private PlanPreview preview, runPreview;
    private TextView plotSelection, runLegend, runTitle, planDeviceStatus;
    private final Handler plotHandler=new Handler(Looper.getMainLooper());
    private long plottedRun=-1;
    private String savedPlotKey;
    private CaptureStore.PlotSnapshot savedPlot;
    private boolean plotReady;
    private boolean planRangeInitialized;
    private boolean restoreEndpoints;
    private int observedDraftRevision=-1;
    private long fittedPlanRun=-1;
    private final Runnable recalculatePlot=() -> {
        try { calculate(); }
        catch(RuntimeException ex) { pendingPlan=null; preview.setPlan(null); planSummary.setText(!endpointSet("start") || !endpointSet("end")?"Noch kein Plan · Start und Ende ausrichten und merken.":"Plan ungültig: "+ex.getMessage()); render(); }
    };
    private PanoramaPlan pendingPlan;
    private CheckBox stopOnLoss;
    private int selectedPage;
    private String pendingExport;
    private String pendingExportName;
    private final PanoramaService.Observer observer = this::render;
    private final ServiceConnection binding = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((PanoramaService.LocalBinder)binder).service();
            if(observedDraftRevision<0) {
                observedDraftRevision=service.draftRevision;
                if(!restoreEndpoints && !service.draftEndpoints.isEmpty()) {
                    for(Map.Entry<String,String> value:service.draftEndpoints.entrySet()) inputs.get(value.getKey()).setText(value.getValue());
                    recalculatePlot.run();
                }
            }
            if(savedPlotKey!=null) try { savedPlot=service.captures.plotSnapshot(savedPlotKey); } catch(RuntimeException ignored) { savedPlotKey=null; }
            service.observe(observer);
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; render(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        preferences = getSharedPreferences("settings",MODE_PRIVATE);
        restoreEndpoints=state!=null && state.getLong("processToken") == PROCESS_TOKEN;
        observedDraftRevision=restoreEndpoints?state.getInt("draftRevision",-1):-1;
        migrateTiltSign();
        migrateTiltDirection();
        tiltRange=Math.max(0,Math.min(2,preferences.getInt("tiltRange",0)));
        if (state!=null) { pendingExportName=state.getString("exportName"); savedPlotKey=state.getString("savedPlotKey"); plottedRun=state.getLong("plottedRun",-1); }
        loadProfiles(); buildUi(); refreshDevices();
        plotReady=true; recalculatePlot.run(); preview.setEditMode(3); applyTiltRange();
        preview.restore(state,"planViewport"); runPreview.restore(state,"runViewport");
        planRangeInitialized=state!=null && state.getBoolean("limitsKnown",false);
        updateProposal();
        selectPage(state == null ? 0 : state.getInt("page",0));
    }
    @Override protected void onStart() {
        super.onStart(); bound = bindService(new Intent(this,PanoramaService.class),binding,BIND_AUTO_CREATE);
    }
    @Override protected void onStop() {
        plotHandler.removeCallbacks(recalculatePlot);
        saveInputs(); if (service != null) service.removeObserver(observer);
        if (bound) { unbindService(binding); bound = false; } service = null;
        super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putLong("processToken",PROCESS_TOKEN); state.putInt("page",selectedPage); state.putInt("draftRevision",observedDraftRevision); state.putBoolean("limitsKnown",service!=null && service.state.limitsKnown); state.putString("exportName",pendingExportName); state.putString("savedPlotKey",savedPlotKey); state.putLong("plottedRun",plottedRun); preview.save(state,"planViewport"); runPreview.save(state,"runViewport"); saveInputs(); super.onSaveInstanceState(state); }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
    private TextView text(String value,int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(size>=20?INK:MUTED); view.setPadding(0,dp(6),0,dp(6));
        if(size>=20) view.setTypeface(null,Typeface.BOLD); return view;
    }
    private LinearLayout vertical() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private GradientDrawable surface(int color) { GradientDrawable background=new GradientDrawable(); background.setColor(color); background.setCornerRadius(dp(14)); return background; }
    private LinearLayout card(LinearLayout parent,String title) {
        LinearLayout body=vertical(); body.setPadding(dp(12),dp(8),dp(12),dp(10)); body.setBackground(surface(Color.WHITE));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,dp(6),0,dp(8)); parent.addView(body,params);
        if(title!=null) body.addView(text(title,20)); return body;
    }
    private LinearLayout details(LinearLayout parent,String title) {
        LinearLayout body=vertical(); Button toggle=button(parent,"▸ "+title,() -> { },() -> true);
        parent.addView(body); body.setVisibility(View.GONE);
        toggle.setOnClickListener(v -> { boolean open=body.getVisibility()!=View.VISIBLE; body.setVisibility(open?View.VISIBLE:View.GONE); toggle.setText((open?"▾ ":"▸ ")+title); });
        return body;
    }
    private void primary(Button button) {
        button.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(218,227,231),ACCENT}));
        button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,Color.WHITE}));
    }
    private void buildUi() {
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = vertical(); root.setFitsSystemWindows(true); root.setPadding(dp(12),dp(4),dp(12),dp(4)); root.setBackgroundColor(Color.rgb(239,244,247)); setContentView(root);
        LinearLayout header=new LinearLayout(this); root.addView(header);
        LinearLayout identity=vertical(); header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
        TextView heading=text("PanoramaControl",landscape?16:20); identity.addView(heading);
        heading.setContentDescription("PanoramaControl Version "+BuildConfig.VERSION_NAME);
        connection = text("Nicht verbunden",15);
        message = text("Bereit",14);
        connection.setTextSize(12); connection.setPadding(0,0,0,dp(4)); identity.addView(connection);
        message.setMaxLines(landscape?1:2); message.setTextSize(13);
        Button emergency = button(header,"NOT-STOP",() -> service.emergencyStop(),() -> service != null && service.connected);
        emergency.setLayoutParams(new LinearLayout.LayoutParams(dp(112),dp(52)));
        emergency.setTextColor(Color.WHITE); emergency.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(160,20,30)));
        LinearLayout nav = new LinearLayout(this);
        if(landscape) { header.removeView(emergency); header.addView(nav,new LinearLayout.LayoutParams(0,dp(56),2)); header.addView(emergency); }
        else root.addView(nav);
        root.addView(message);
        String[] names = {"Gerät","Manuell","Plan","Lauf","Extras"};
        for (int i=0;i<names.length;i++) {
            final int index=i; Button tab = new Button(this); tab.setText(names[i]); tab.setTextSize(12); tab.setAllCaps(false); tab.setMinWidth(0); tab.setMinimumWidth(0); tab.setPadding(0,0,0,0); tabs.add(tab);
            tab.setOnClickListener(v -> selectPage(index)); nav.addView(tab,new LinearLayout.LayoutParams(0,dp(landscape?48:52),1));
        }
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        root.addView(container,new LinearLayout.LayoutParams(-1,0,1)); pages = new LinearLayout[5];
        for (int i=0;i<5;i++) {
            ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); pages[i]=vertical(); scroll.addView(pages[i]);
            container.addView(scroll); scroll.setTag(i);
        }
        LinearLayout device = pages[0]; device.addView(text("Verbindung",20));
        devicePicker = new Spinner(this); device.addView(devicePicker);
        button(device,"Geräte laden",this::refreshDevices,() -> service==null || !service.hasConnection());
        button(device,"Verbinden",this::connect,() -> service!=null && !service.hasConnection() && !devices.isEmpty());
        button(device,"Trennen",() -> service.disconnect(),() -> service!=null && service.hasConnection());
        device.addView(text("RNBT zuerst in Android koppeln. Eine bestehende PC-Verbindung vorher trennen.",14));
        live = text("Noch keine Statusdaten",16); device.addView(live);
        primary(button(device,"Weiter zum Panorama",() -> selectPage(2),() -> true));
        LinearLayout manual=pages[1]; manual.addView(text("Manuelle Bedienung",20));
        manual.addView(text("Pfeile fahren zum nächsten Ziel. Loslassen stoppt keine Fahrt; dafür Not-Stop verwenden.",14));
        TextView manualHint=text("",14); manualHint.setTag("alignmentHint"); manual.addView(manualHint);
        button(manual,"Gestoppten Auftrag zurücksetzen",this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        input(manual,"step","Eigene Schrittweite (°) · unten auswählen","1");
        input(manual,"pulse","Kamera-Puls (ms)","30");
        input(manual,"manualSpeed","Manuelles Fahrtempo (‰, 1–1000)","500");
        button(manual,"Kamera einmal auslösen",() -> service.trigger(integer("pulse")),() -> service!=null && service.manualAllowed() && !service.manualMoving && service.state.positionSafe());
        button(manual,"Bewegungsdiagnose",() -> service.command("GET_DIAGNOSTICS"),this::ready);
        button(pages[2],"Plan entfernen",this::removePlan,() -> service==null || !service.hasConnection() || (ready() && !service.manualMoving && !service.manualTrigger && (service.state.state.equals("IDLE") || PlanAbort.allowed(service.state.state))));
        LinearLayout camera=card(pages[2],"1 · Kamera wählen");
        profilePicker=new Spinner(this); camera.addView(profilePicker); updateProfilePicker();
        primary(button(camera,"Kameraprofil verwenden",this::applyProfile,() -> true));
        activeProfile=text("",13); camera.addView(activeProfile);
        LinearLayout profileDetails=details(camera,"Profil bearbeiten / importieren");
        input(profileDetails,"profileName","Profilname","Sony A7RII 400 mm");
        input(profileDetails,"focal","Brennweite (mm)","400"); input(profileDetails,"width","Sensorbreite (mm)","24"); input(profileDetails,"height","Sensorhöhe (mm)","35.9");
        button(profileDetails,"Profil speichern",this::saveProfile,() -> true);
        button(profileDetails,"Windows-Konfiguration importieren",this::importConfig,() -> true);
        LinearLayout plan=card(pages[2],"2 · Bildausschnitt ausrichten");
        alignmentHint=text("Punkt wählen → anfahren → Kamera prüfen → Start/Ende merken.",13); plan.addView(alignmentHint);
        alignmentReset=button(plan,"Auftrag zurücksetzen & Endpunkte ändern",this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        alignmentReset.setVisibility(View.GONE);
        LinearLayout graph=plan, steering=plan;
        if(landscape) {
            LinearLayout workspace=vertical();
            plan.addView(workspace,new LinearLayout.LayoutParams(-1,-2));
            graph=vertical(); workspace.addView(graph,new LinearLayout.LayoutParams(-1,-2));
            steering=vertical(); workspace.addView(steering);
        }
        LinearLayout selectionRow=new LinearLayout(this); graph.addView(selectionRow);
        plotSelection=text("Ansicht verschieben",14); selectionRow.addView(plotSelection,new LinearLayout.LayoutParams(0,-2,1));
        if(landscape) { plotSelection.setSingleLine(true); plotSelection.setEllipsize(android.text.TextUtils.TruncateAt.END); }
        tiltRangeButton=button(selectionRow,"Tilt ±25°",this::chooseTiltRange,() -> true);
        tiltRangeButton.setTextSize(12); tiltRangeButton.setLayoutParams(new LinearLayout.LayoutParams(dp(130),dp(landscape?48:56)));
        preview=new PlanPreview(this); graph.addView(preview,new LinearLayout.LayoutParams(-1,Math.max(dp(210),preview.alignmentHeight())));
        preview.setProposalListener(this::updateProposal);
        preview.setDriveAction(() -> safely(this::driveSelection));
        preview.setListener((start,pan,tilt) -> {
            String prefix=start?"start":"end";
            inputs.get(prefix+"Pan").setText(""+pan); inputs.get(prefix+"Tilt").setText(""+tilt);
            refreshEndpoints(); schedulePlot();
        });
        LinearLayout remember=new LinearLayout(this); steering.addView(remember);
        button(remember,"Start merken",() -> takePosition("start"),() -> positionReady() && service.state.positionSafe());
        button(remember,"Ende merken",() -> takePosition("end"),() -> positionReady() && service.state.positionSafe());
        endpointSummary=text("",14); plan.addView(endpointSummary);
        LinearLayout revisit=new LinearLayout(this); plan.addView(revisit);
        button(revisit,"Start anfahren",() -> driveEndpoint("start"),() -> positionReady() && endpointSet("start"));
        button(revisit,"Ende anfahren",() -> driveEndpoint("end"),() -> positionReady() && endpointSet("end"));
        if(landscape) {
            for(int rowIndex=0;rowIndex<steering.getChildCount();rowIndex++) {
                View row=steering.getChildAt(rowIndex);
                if(row instanceof LinearLayout) for(int child=0;child<((LinearLayout)row).getChildCount();child++) {
                    View control=((LinearLayout)row).getChildAt(child); control.getLayoutParams().height=dp(48);
                    if(control instanceof Button && ((Button)control).getText().length()>1) { ((Button)control).setTextSize(12); ((Button)control).setSingleLine(true); }
                }
            }
        }
        LinearLayout plotOptions=details(plan,"Grafikansicht & Zoom");
        LinearLayout graphMode=new LinearLayout(this); plotOptions.addView(graphMode);
        button(graphMode,"Punkt wählen",() -> preview.setEditMode(3),() -> true);
        button(graphMode,"Verschieben",() -> preview.setEditMode(0),() -> true);
        button(plotOptions,"Ganzer Pan-/Tilt-Bereich",() -> { tiltRange=2; applyTiltRange(); },() -> true);
        plotControls(plotOptions,preview);
        LinearLayout capture=card(pages[2],"3 · Prüfen & aufnehmen");
        planSummary=text("Noch kein Plan berechnet",15); capture.addView(planSummary);
        planDeviceStatus=text("Zum Übertragen mit dem Gerät verbinden",13); capture.addView(planDeviceStatus);
        primary(button(capture,"Plan übertragen",this::loadPlan,() -> !preview.selection.pending && pendingPlan!=null && positionReady() && service.state.positionSafe() && service.state.state.equals("IDLE")));
        primary(button(capture,"Aufnahme starten",this::startLoadedPlan,this::canStartLocalPlan));
        LinearLayout settings=details(capture,"Aufnahmeparameter & Endpunkte");
        input(settings,"overlap","Überlappung (%)","20");
        modePicker=new Spinner(this); modePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Raster · Spaltenrücklauf","Sphärisch · Tilt-Bänder"}));
        modePicker.setSelection(preferences.getInt("mode",0)); settings.addView(modePicker);
        input(settings,"startPan","Start Pan (°)",""); input(settings,"startTilt","Start Tilt (°)","");
        button(settings,"Start im Plan grafisch bearbeiten",() -> preview.setEditMode(1),() -> true);
        input(settings,"endPan","Ende Pan (°)",""); input(settings,"endTilt","Ende Tilt (°)","");
        button(settings,"Ende im Plan grafisch bearbeiten",() -> preview.setEditMode(2),() -> true);
        button(settings,"Grafische Planänderung übernehmen",preview::commitSelection,() -> preview.selection.pending && (preview.editMode()==1 || preview.editMode()==2));
        button(settings,"Zurück zum Ausrichten",() -> preview.setEditMode(3),() -> true);
        input(settings,"settle","Beruhigungszeit (ms)","700"); input(settings,"planPulse","Triggerdauer (ms)","30"); input(settings,"exposure","Belichtungswartezeit (ms)","500");
        input(settings,"speed","Fahrtempo im Aufnahmelauf (‰, 1–1000)","500");
        stopOnLoss=new CheckBox(this); stopOnLoss.setText("Bei Verbindungsverlust nach 5 s stoppen");
        stopOnLoss.setChecked(preferences.getBoolean("stopOnLoss",false)); settings.addView(stopOnLoss);
        settings.addView(text("Ohne Häkchen führt der Arduino den Auftrag auch bei getrennter Handy-Verbindung weiter aus.",14));
        button(settings,"Vorschau berechnen",this::calculate,() -> true);
        LinearLayout run=card(pages[3],null); runTitle=text("Aktueller Auftrag",20); run.addView(runTitle);
        runProgress=text("Noch keine Aufnahme gestartet",17); run.addView(runProgress);
        captureProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); run.addView(captureProgress,new LinearLayout.LayoutParams(-1,dp(8)));
        LinearLayout runActions=new LinearLayout(this); run.addView(runActions);
        primary(button(runActions,"Pause",() -> service.runCommand("PAUSE"),() -> savedPlot==null && ready() && service.state.state.equals("RUNNING")));
        primary(button(runActions,"Fortsetzen",() -> service.runCommand("RESUME"),() -> savedPlot==null && ready() && service.state.positionSafe() && service.state.state.equals("PAUSED") && !service.manualMoving && !service.manualTrigger));
        button(run,"Stoppen & Endpunkte ändern",this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        button(run,"Stop",() -> confirm("Auftrag stoppen?","Motoren und Kameraausgänge werden abgeschaltet. Es gibt keine Parkfahrt.",() -> service.runCommand("STOP")),() -> ready() && !service.state.state.equals("IDLE") && !service.state.state.startsWith("TIMELAPSE"));
        runPreview=new PlanPreview(this); run.addView(runPreview,new LinearLayout.LayoutParams(-1,dp(300)));
        plotControls(run,runPreview);
        runLegend=text("Blau: Sollpunkte · Violett: Fahrziel · Orange: Istposition\nGrün: gemessene Aufnahme · ×: rekonstruiertes Soll\nLetzte 512 Aufnahmen / Statuspunkte; graue Linie: empfangener Verlauf",13); run.addView(runLegend);
        button(run,"Aktuellen Gerätelauf anzeigen",() -> { savedPlotKey=null; savedPlot=null; plottedRun=-1; render(); },() -> service!=null);
        button(run,"Auftrag starten",this::startLoadedPlan,() -> savedPlot==null && canStartLoadedPlan());
        button(run,"Eine Spalte zurück",() -> service.columns(false),() -> savedPlot==null && ready());
        button(run,"Eine Spalte vor",() -> service.columns(true),() -> savedPlot==null && ready());
        button(run,"Zeiten aus Plan-Eingaben übernehmen",() -> service.updateTiming(integer("settle"),integer("planPulse"),integer("exposure")),() -> savedPlot==null && ready() && service.state.plan!=null);
        button(run,"Gerätestand neu einlesen",() -> service.command("GET_SUPPORT_BUNDLE"),this::ready);
        button(run,"Vorhandenen Auftrag löschen",() -> confirm("Auftrag löschen?","Der gespeicherte Geräteauftrag wird zurückgesetzt. Lokale Aufnahmedaten bleiben erhalten.",() -> service.runCommand("CLEAR_RUN")),this::ready);
        button(run,"Plan abbrechen / Neuer Plan",this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        TextView runLive=text("",16); run.addView(runLive); runLive.setTag("live");
        LinearLayout extras=pages[4]; extras.addView(text("Timelapse",20));
        input(extras,"interval","Timelapse-Intervall (ms)","5000");
        input(extras,"tlPulse","Timelapse-Pulsdauer (ms)","30");
        button(extras,"Timelapse starten",() -> confirm("Timelapse starten?","Die Kamera wird autonom im eingestellten Intervall ausgelöst.",() -> service.timelapse(newRunId(),integer("interval"),integer("tlPulse"))),() -> ready() && service.state.state.equals("IDLE") && !service.manualMoving && !service.manualTrigger);
        button(extras,"Timelapse stoppen",() -> service.runCommand("STOP_TIMELAPSE"),() -> ready() && service.state.state.equals("TIMELAPSE"));
        extras.addView(text("Aufnahmen und Export",20));
        runPicker=new Spinner(this); extras.addView(runPicker);
        button(extras,"Gespeicherte Läufe laden",this::refreshRuns,() -> service!=null);
        button(extras,"Gewählten Lauf grafisch anzeigen",() -> {
            CaptureStore.Run selected=(CaptureStore.Run)runPicker.getSelectedItem();
            if(selected==null) throw new IllegalArgumentException("Keine Aufnahmen gespeichert");
            savedPlotKey=selected.key; savedPlot=service.captures.plotSnapshot(selected.key);
            selectPage(3); render(); runPreview.fit();
        },() -> service!=null);
        button(extras,"PapyWizard XML speichern",() -> exportCaptures(true),() -> service!=null);
        button(extras,"Capture-Manifest CSV speichern",() -> exportCaptures(false),() -> service!=null);
        extras.addView(text("CSV kennzeichnet rekonstruierte Sollpositionen und fehlende historische Istwerte. XML verwendet bei rekonstruierten Bildern die Sollpositionen. Bei Verbindungslücken können manuelle Aufnahmen fehlen.",14));
        extras.addView(text("Diagnose und Grenzen",20));
        diagnostics=text("Noch keine Diagnose",15); extras.addView(diagnostics);
        button(extras,"Support-Bundle abfragen",() -> service.command("GET_SUPPORT_BUNDLE"),this::ready);
        button(extras,"Diagnose speichern",this::exportSupport,() -> service!=null);
        LinearLayout tiltSetup=card(extras,"Tilt-Nullpunkt & Limits");
        tiltReference=text("Zum Einstellen mit dem Gerät verbinden.",15); tiltSetup.addView(tiltReference);
        tiltSetup.addView(text("Kamera waagerecht ausrichten und diese Position als 0° setzen. Die Limits bleiben an denselben physischen Stellen; ihre angezeigten Winkel passen sich an.",14));
        button(tiltSetup,"Aktuelle Position als Tilt 0° setzen",() -> setTiltZero(false),this::tiltCalibrationReady);
        button(tiltSetup,"Tilt-Nullpunkt zurücksetzen",() -> setTiltZero(true),this::tiltCalibrationReady);
        input(tiltSetup,"tiltMin","Tilt-Untergrenze (°)","-45");
        input(tiltSetup,"tiltMax","Tilt-Obergrenze (°)","50");
        button(tiltSetup,"Vermessene Tilt-Grenzen speichern",this::setTiltLimits,this::tiltCalibrationReady);
        tiltSetup.addView(text("Grenzen erst nach mechanischer Vermessung mit montiertem Objektiv und Steckern ändern.",14));
        navigationBar=new LinearLayout(this); navigationBar.setTag("navigationBar");
        navigationBar.setPadding(0,dp(4),0,dp(4)); root.addView(navigationBar);
        finePicker=new Spinner(this); finePicker.setContentDescription("Schrittweite für Pfeile");
        finePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"0,1°","0,5°","1°","Eigene"}));
        finePicker.setSelection(Math.max(0,Math.min(3,preferences.getInt("fineStep",0))));
        navigationBar.addView(finePicker,new LinearLayout.LayoutParams(0,dp(56),1.3f));
        arrowButton(navigationBar,"←","Pan nach links",() -> fineMove(fineStep(),0));
        arrowButton(navigationBar,"→","Pan nach rechts",() -> fineMove(-fineStep(),0));
        arrowButton(navigationBar,"↑","Tilt erhöhen",() -> fineMove(0,fineStep()));
        arrowButton(navigationBar,"↓","Tilt verringern",() -> fineMove(0,-fineStep()));
        for(Map.Entry<String,EditText> entry:inputs.entrySet()) {
            String key=entry.getKey();
            if(key.equals("step") || key.equals("pulse") || key.equals("interval") || key.equals("tlPulse") || key.startsWith("tilt")) continue;
            entry.getValue().addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
                public void onTextChanged(CharSequence s,int start,int before,int count) { if(plotReady) { refreshEndpoints(); schedulePlot(); } }
                public void afterTextChanged(Editable value) { }
            });
        }
        modePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) { schedulePlot(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        stopOnLoss.setOnCheckedChangeListener((v,checked) -> schedulePlot());
        profilePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id) { if(plotReady) render(); }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        render();
    }
    private void plotControls(LinearLayout parent,PlanPreview view) {
        LinearLayout row=new LinearLayout(this); parent.addView(row);
        button(row,"−",() -> view.zoom(2),() -> true);
        button(row,"+",() -> view.zoom(.5),() -> true);
        button(row,"Einpassen",view::fit,() -> true);
    }
    private void chooseTiltRange() {
        new AlertDialog.Builder(this).setTitle("Tilt-Ausschnitt der Grafik")
            .setSingleChoiceItems(new String[]{"±25° · übliche Panoramen","±45° · größerer Tilt-Bereich","Gesamter Bereich · z. B. sphärisch"},tiltRange,(dialog,which) -> {
                tiltRange=which; applyTiltRange(); dialog.dismiss();
            }).setNegativeButton("Abbrechen",null).show();
    }
    private void applyTiltRange() {
        preview.alignmentRange(tiltRange==0?25:tiltRange==1?45:0);
        tiltRangeButton.setText(tiltRange==0?"Tilt ±25°":tiltRange==1?"Tilt ±45°":"Tilt gesamt");
        preferences.edit().putInt("tiltRange",tiltRange).apply();
    }
    private void schedulePlot() { if(!plotReady) return; plotHandler.removeCallbacks(recalculatePlot); plotHandler.postDelayed(recalculatePlot,180); }
    private void updateProposal() {
        if(preview==null || plotSelection==null) return;
        plotSelection.setText(proposalText());
        if(plotReady) render();
    }
    private String proposalText() {
        EndpointSelection p=preview.selection;
        if(p.pending) return String.format(Locale.GERMANY,"%s · Pan %.3f° · Tilt %.3f°\nTilt-Änderung %+.3f°%s",preview.editMode()==3?"Fahrziel":preview.editMode()==1?"Start-Vorschlag":"Ende-Vorschlag",p.pan,p.tilt,p.tilt-(service!=null && service.fresh()?service.state.tilt/1000.0:p.baseTilt),p.limited?" · Gerätegrenze":"");
        if(service!=null && service.fresh()) return String.format(Locale.GERMANY,"Aktuell · Pan %.3f° · Tilt %.3f°\n%s",-service.state.pan/1000.0,service.state.tilt/1000.0,preview.editMode()==0?"Ansicht verschieben":"Punkt im Diagramm wählen");
        return "Punkt wählen · Start S blau / Ende E rot\nZum Anfahren mit dem Gerät verbinden";
    }
    private boolean positionReady() { return manualReady() && !service.manualMoving && !service.manualTrigger; }
    private boolean canStartLoadedPlan() { return ready() && !service.manualMoving && !service.manualTrigger && service.state.positionSafe() && service.state.state.equals("PLAN_READY") && service.state.plan!=null; }
    private boolean canStartLocalPlan() { return canStartLoadedPlan() && !preview.selection.pending && pendingPlan!=null && pendingPlan.sameRecipe(service.state.plan); }
    private void startLoadedPlan() {
        if(!canStartLoadedPlan()) throw new IllegalArgumentException("Zuerst einen Plan übertragen und dessen Bestätigung abwarten");
        if(selectedPage==2) { calculate(); if(!canStartLocalPlan()) throw new IllegalArgumentException("Plan geändert: Geräteauftrag zurücksetzen und aktuellen Plan erneut übertragen"); }
        long run=service.state.runId;
        confirm("Geladenen Auftrag starten?",service.state.plan.total()+" Aufnahmen · Geräteauftrag "+run+". Das Gerät fährt und löst die Kamera aus. Gestartet wird der bereits übertragene Plan; spätere lokale Änderungen müssen zuvor neu übertragen werden.",() -> {
            if(!canStartLoadedPlan() || service.state.runId!=run) throw new IllegalArgumentException("Geladener Auftrag hat sich geändert · erneut prüfen");
            service.runCommand("START"); selectPage(3);
        });
    }
    private void abortLoadedPlan() {
        confirm("Auftrag stoppen & Endpunkte ändern?","Das Gerät stoppt. Danach wird der Geräteauftrag zurückgesetzt und das Ausrichten freigegeben. Start, Ende und bisherige Aufnahmen bleiben auf dem Handy erhalten. Anschließend den geänderten Plan übertragen und separat starten.",() -> { service.abortPlan(); preview.setEditMode(3); selectPage(2); ((ScrollView)pages[2].getParent()).smoothScrollTo(0,0); });
    }
    private void removePlan() {
        if(service!=null && service.hasConnection()) {
            if(!ready()) throw new IllegalArgumentException("Aktuellen Gerätestand abwarten");
            if(!service.state.state.equals("IDLE")) { service.abortPlan(true); return; }
            service.discardDraft(); observedDraftRevision=service.draftRevision;
        }
        clearLocalPlan(); Toast.makeText(this,"Plan entfernt",Toast.LENGTH_SHORT).show();
    }
    private void clearLocalPlan() {
        for(String key:new String[]{"startPan","startTilt","endPan","endTilt"}) inputs.get(key).setText("");
        pendingPlan=null; preview.setPlan(null); preview.setEndpointAvailability(false,false); preview.setEditMode(3);
        planSummary.setText("Noch kein Plan · Start und Ende ausrichten und merken."); saveInputs();
    }
    private boolean endpointSet(String prefix) {
        try { number(prefix+"Pan"); number(prefix+"Tilt"); return true; } catch(RuntimeException ex) { return false; }
    }
    private double fineStep() { int selected=finePicker.getSelectedItemPosition(); return selected==3?stepSize():new double[]{.1,.5,1}[selected]; }
    private void fineMove(double pan,double tilt) { service.setManualTempo(integer("manualSpeed")); preview.discardSelection(); service.move(pan,tilt); }
    private void driveSelection() {
        if(!preview.selection.pending || preview.editMode()!=3) throw new IllegalArgumentException("Zuerst einen Fahrpunkt wählen");
        driveUiPoint(preview.selection.pan,preview.selection.tilt);
    }
    private void driveEndpoint(String prefix) { driveUiPoint(number(prefix+"Pan"),number(prefix+"Tilt")); }
    private void driveUiPoint(double pan,double tilt) {
        if(!positionReady()) throw new IllegalArgumentException("Zuerst aktuelle Zielankunft abwarten");
        service.setManualTempo(integer("manualSpeed")); service.moveToUi(pan,tilt); preview.discardSelection();
    }
    private void refreshEndpoints() {
        try {
            boolean start=endpointSet("start"),end=endpointSet("end");
            preview.setEndpoints(start?number("startPan"):0,start?number("startTilt"):0,end?number("endPan"):0,end?number("endTilt"):0);
            preview.setEndpointAvailability(start,end);
        }
        catch(RuntimeException ignored) { /* An incomplete numeric edit is rejected by calculation. */ }
    }
    private Button button(LinearLayout parent,String title,Runnable action,BooleanSupplier enabled) {
        Button button=new Button(this); button.setText(title); button.setMinHeight(dp(48)); button.setAllCaps(false); button.setTextSize(14); button.setMinWidth(0); button.setMinimumWidth(0); button.setPadding(dp(8),0,dp(8),0);
        button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(119,133,144),INK}));
        button.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(235,239,241),Color.rgb(226,238,241)}));
        button.setOnClickListener(v -> safely(action)); gates.put(button,enabled);
        if (parent.getOrientation()==LinearLayout.HORIZONTAL) parent.addView(button,new LinearLayout.LayoutParams(0,dp(56),1)); else parent.addView(button);
        return button;
    }
    private void arrowButton(LinearLayout parent,String symbol,String description,Runnable action) {
        Button arrow=button(parent,symbol,action,this::manualReady);
        arrow.setTextSize(24); arrow.setContentDescription(description);
    }
    private EditText input(LinearLayout parent,String key,String label,String fallback) {
        parent.addView(text(label,14)); EditText view=new EditText(this); view.setSingleLine(true);
        view.setInputType(key.equals("profileName") ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        boolean endpoint=key.startsWith("start") || key.startsWith("end");
        view.setText(endpoint && !restoreEndpoints?"":preferences.getString(key,fallback)); inputs.put(key,view); parent.addView(view); return view;
    }
    private boolean ready() { return service!=null && service.fresh() && !service.busy && !service.abortingPlan; }
    private boolean manualReady() { return service!=null && service.navigationAllowed() && selectedPage!=3; }
    private void selectPage(int index) {
        selectedPage=Math.max(0,Math.min(4,index));
        navigationBar.setVisibility(selectedPage==1 || selectedPage==2?View.VISIBLE:View.GONE);
        for (int i=0;i<pages.length;i++) ((View)pages[i].getParent()).setVisibility(i==selectedPage ? View.VISIBLE : View.GONE);
        for(int i=0;i<tabs.size();i++) { Button tab=tabs.get(i); tab.setSelected(i==selectedPage); tab.setTextColor(i==selectedPage?Color.WHITE:MUTED); tab.setBackgroundTintList(ColorStateList.valueOf(i==selectedPage?ACCENT:Color.rgb(226,234,239))); }
        if (selectedPage==4 && service!=null) refreshRuns();
        render();
    }
    private String statusText() {
        if (service==null) return "Keine Geräteverbindung";
        DeviceState s=service.state;
        String result=String.format(Locale.GERMANY,"Firmware: %s · %s\nStatus: %s · %s\nPosition Pan %.3f° · Tilt %.3f°\nAufnahmen: %d / %d · Spalte %d · Zeile %d\nLauf: %d · Revision %d\n",s.firmware,s.hardware,s.state,s.phase,-s.pan/1000.0,s.tilt/1000.0,s.completed,s.total,s.column,s.row,s.runId,s.revision);
        if (!service.fresh()) result+="Statusdaten fehlen oder sind veraltet.\n";
        if (s.limitsKnown) result+=String.format(Locale.GERMANY,"Grenzen %s\nPan %.3f° bis %.3f° · Tilt %.3f° bis %.3f°\n",s.confirmed ? "bestätigt" : "NICHT bestätigt",-s.panMax/1000.0,-s.panMin/1000.0,s.tiltMin/1000.0,s.tiltMax/1000.0);
        if (s.limitsKnown && s.confirmed && !s.positionSafe()) result+="Istposition außerhalb der Grenze: betroffene Achse in Richtung des erlaubten Bereichs zurückfahren.\n";
        if (service.manualMoving) result+="Manuelle Fahrt aktiv\n";
        if (service.manualTrigger) result+="Kameraauslösung aktiv/noch nicht abgeschlossen\n";
        if (s.plan!=null) result+="Geräteplan: "+s.plan.total()+" Bilder · Beruhigung "+s.plan.settleMs+" ms · Puls "+s.plan.pulseMs+" ms · Belichtung "+s.plan.exposureMs+" ms\n";
        return result;
    }
    private void render() {
        if (connection==null) return;
        EditText manualTempo=inputs.get("manualSpeed");
        if(manualTempo!=null) manualTempo.setEnabled(service==null || !service.connected || service.state.profiledMotion);
        if(service!=null && observedDraftRevision>=0 && observedDraftRevision!=service.draftRevision) {
            observedDraftRevision=service.draftRevision; clearLocalPlan();
        }
        connection.setText(service==null ? "Nicht verbunden" : service.connection);
        getWindow().setFlags(service!=null && service.connected?android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON:0,android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        message.setText(service==null ? "Bereit" : service.message);
        boolean loadedForAlignment=service!=null && service.fresh() && service.state.state.equals("PLAN_READY");
        if(alignmentReset!=null) alignmentReset.setVisibility(service!=null && service.fresh() && PlanAbort.allowed(service.state.state)?View.VISIBLE:View.GONE);
        if(planDeviceStatus!=null) planDeviceStatus.setText(service==null || !service.fresh()?"Zum Übertragen mit dem Gerät verbinden":loadedForAlignment?(canStartLocalPlan()?"✓ Plan bestätigt · Aufnahme jetzt starten":"Plan auf dem Handy geändert oder Auswahl offen. Auftrag zurücksetzen und aktuellen Plan übertragen."):service.state.state.equals("IDLE")?"Erst übertragen, Bestätigung abwarten, dann Aufnahme starten.":"Gerätezustand: "+service.state.state+" · Auftrag vor dem neuen Plan zurücksetzen");
        if(alignmentHint!=null) alignmentHint.setText(alignmentHelp());
        if(activeProfile!=null && inputs.containsKey("profileName")) {
            String name=inputs.get("profileName").getText().toString();
            CameraProfile selected=(CameraProfile)profilePicker.getSelectedItem();
            activeProfile.setText("Aktiv: "+name+(selected!=null && !selected.name.equals(name)?"\nAuswahl oben mit „Kameraprofil verwenden“ übernehmen.":""));
        }
        TextView manualHint=pages[1].findViewWithTag("alignmentHint"); if(manualHint!=null) manualHint.setText(alignmentHelp());
        if(endpointSummary!=null) endpointSummary.setText(endpointLabel("start","Start")+"\n"+endpointLabel("end","Ende"));
        if(runProgress!=null) {
            long completed=savedPlot!=null?savedPlot.model.captures.size():service==null?0:service.state.completed;
            long total=savedPlot!=null && savedPlot.plan!=null?savedPlot.plan.total():service==null?0:service.state.total;
            String state=savedPlot!=null?"Gespeichert":service==null || !service.fresh()?"Status ausstehend":friendlyState(service.state.state);
            runProgress.setText(state+" · "+completed+" / "+total+" Aufnahmen"); captureProgress.setMax((int)Math.max(1,total)); captureProgress.setProgress((int)completed);
        }
        live.setText(statusText());
        TextView runLive=pages[3].findViewWithTag("live"); if (runLive!=null) runLive.setText(statusText());
        diagnostics.setText(service==null ? "Noch keine Diagnose" : service.diagnostics);
        if(tiltReference!=null) {
            DeviceState s=service==null?null:service.state;
            tiltReference.setText(s==null || !service.fresh()?"Zum Einstellen mit dem Gerät verbinden.":!s.tiltCalibrationKnown?"Tilt-Nullpunkt benötigt Firmware 2.3.5 oder neuer.":String.format(Locale.GERMANY,"Aktueller Tilt: %.3f°\nNullpunkt: %s",s.tilt/1000.0,s.tiltZeroOffset==0?"Werkreferenz":"im Gerät gespeichert"));
            if(s!=null && service.fresh() && s.limitsKnown) {
                String limits=s.tiltMin+"|"+s.tiltMax+"|"+s.tiltZeroOffset;
                if(!limits.equals(displayedTiltLimits)) {
                    displayedTiltLimits=limits;
                    inputs.get("tiltMin").setText(""+(s.tiltMin/1000.0)); inputs.get("tiltMax").setText(""+(s.tiltMax/1000.0));
                }
            }
        }
        if(plotReady) plotSelection.setText(proposalText());
        if(preview!=null) { preview.setLive(service==null?null:service.state,service==null?null:service.plot,service!=null && service.fresh()); preview.setDriveReady(positionReady()); }
        if(preview!=null && service!=null && service.state.limitsKnown && !planRangeInitialized) { planRangeInitialized=true; applyTiltRange(); }
        if(runPreview!=null) {
            runTitle.setText(savedPlot!=null?"Gespeicherter Lauf "+savedPlot.model.runId:"Aktueller Auftrag");
            runPreview.setPlan(savedPlot!=null?savedPlot.plan:service==null?null:service.state.plan);
            runPreview.setLive(savedPlot!=null?null:service==null?null:service.state,savedPlot!=null?savedPlot.model:service==null?null:service.plot,savedPlot==null && service!=null && service.fresh());
            runLegend.setText((savedPlot!=null?"Gespeicherter Lauf "+savedPlot.model.runId+" · keine Live-Position\n":"Aktueller Gerätelauf\n")+"Blau: Sollpunkte · Violett: Fahrziel · Orange: Istposition\nGrün: gemessene Aufnahme · ×: rekonstruiertes Soll\nLetzte 512 Aufnahmen / Statuspunkte; graue Linie: empfangener Verlauf");
            if(savedPlot==null && service!=null && service.fresh() && plottedRun!=service.state.runId) { plottedRun=service.state.runId; runPreview.fit(); }
            if(savedPlot==null && service!=null && service.fresh() && service.state.plan!=null && fittedPlanRun!=service.state.runId) { fittedPlanRun=service.state.runId; runPreview.fit(); }
        }
        devicePicker.setEnabled(service==null || !service.hasConnection());
        for (Map.Entry<Button,BooleanSupplier> entry:gates.entrySet()) entry.getKey().setEnabled(entry.getValue().getAsBoolean());
    }
    private String endpointLabel(String prefix,String title) {
        return endpointSet(prefix)?String.format(Locale.GERMANY,"%s  Pan %.3f° · Tilt %.3f°",title,number(prefix+"Pan"),number(prefix+"Tilt")):title+" noch nicht gesetzt";
    }
    private String alignmentHelp() {
        if(service==null || !service.fresh()) return "Zum Ausrichten zuerst unter Gerät verbinden.";
        if(!service.state.confirmed) return "Fahrt gesperrt: Gerätegrenzen sind noch nicht bestätigt.";
        if(!service.state.positionSafe()) return "Istposition außerhalb der Grenze. Nur in Richtung des erlaubten Bereichs zurückfahren.";
        if(service.abortingPlan) return "Auftrag wird gestoppt und zurückgesetzt … bitte Bestätigung abwarten.";
        if(!service.state.navigation(SystemClock.elapsedRealtime())) return service.state.state.equals("RUNNING") || service.state.state.equals("PAUSE_PENDING")?"Aufnahme läuft · Pfeile gesperrt. Zum Ausrichten erst pausieren oder stoppen.":service.state.state.equals("TIMELAPSE")?"Timelapse läuft · Pfeile gesperrt.":"Fahrt hier nicht freigegeben · geladene/beendete Aufträge benötigen Firmware 2.3.6.";
        if(service.manualMoving) return "Kamera fährt … Zielankunft abwarten, dann Start oder Ende merken.";
        return "Punkt antippen → Anfahren direkt am Punkt → Kamera prüfen → Start oder Ende merken. Pfeile unten fahren direkt; der Auftrag bleibt erhalten. ↑ hebt das Objektiv.";
    }
    private String friendlyState(String state) {
        switch(state) { case "IDLE": return "Bereit"; case "PLAN_READY": return "Plan geladen"; case "RUNNING": return "Aufnahme läuft"; case "PAUSE_PENDING": return "Pause wird vorbereitet"; case "PAUSED": return "Pausiert"; case "STOPPED": return "Gestoppt"; case "COMPLETED": return "Abgeschlossen"; case "FAULT": return "Gerätefehler"; default: return state; }
    }
    private void safely(Runnable action) {
        try { action.run(); } catch (RuntimeException ex) { Toast.makeText(this,ex.getMessage()==null ? "Aktion nicht möglich" : ex.getMessage(),Toast.LENGTH_LONG).show(); }
        render();
    }
    private void confirm(String title,String detail,Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setNegativeButton("Abbrechen",null).setPositiveButton("Ja",(d,w) -> safely(action)).show();
    }
    private double number(String key) { double n=Double.parseDouble(inputs.get(key).getText().toString().trim().replace(',','.')); if (!Double.isFinite(n)) throw new IllegalArgumentException("Ungültige Zahl"); return n; }
    private double stepSize() { double n=number("step"); if (n<.001 || n>360) throw new IllegalArgumentException("Schrittweite zwischen 0,001° und 360° erforderlich"); return n; }
    private int integer(String key) { double n=number(key); if (n!=Math.rint(n) || n<Integer.MIN_VALUE || n>Integer.MAX_VALUE) throw new IllegalArgumentException("Ganze Millisekunden/Zahlen erforderlich"); return (int)n; }
    private void saveInputs() {
        SharedPreferences.Editor editor=preferences.edit(); for (Map.Entry<String,EditText> input:inputs.entrySet()) editor.putString(input.getKey(),input.getValue().getText().toString());
        editor.putInt("mode",modePicker.getSelectedItemPosition()).putInt("fineStep",finePicker.getSelectedItemPosition()).putBoolean("stopOnLoss",stopOnLoss.isChecked()).apply();
        if(service!=null) for(String key:new String[]{"startPan","startTilt","endPan","endTilt"}) service.draftEndpoints.put(key,inputs.get(key).getText().toString());
    }
    private boolean permission() {
        if (Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},41); return false;
        }
        return true;
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(code,permissions,grants);
        if (code==41 && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) refreshDevices();
    }
    private void refreshDevices() {
        if (!permission()) return;
        BluetoothAdapter adapter=getSystemService(BluetoothManager.class).getAdapter(); devices.clear();
        try { if (adapter!=null && adapter.isEnabled()) devices.addAll(adapter.getBondedDevices()); }
        catch (SecurityException ex) { message.setText("Bluetooth-Berechtigung fehlt"); return; }
        devices.sort(Comparator.comparing(this::deviceName));
        List<String> names=new ArrayList<>(); int selected=0; String saved=preferences.getString("device","");
        for (int i=0;i<devices.size();i++) { BluetoothDevice d=devices.get(i); String name=deviceName(d); names.add(name+" ("+d.getAddress()+")"); if (d.getAddress().equals(saved) || (saved.isEmpty() && name.startsWith("RNBT"))) selected=i; }
        devicePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names)); devicePicker.setSelection(selected); render();
    }
    private String deviceName(BluetoothDevice device) {
        try { String name=device.getName(); return name==null ? "Unbenannt" : name; }
        catch (SecurityException ex) { return "Bluetooth-Berechtigung fehlt"; }
    }
    private void connect() {
        if (!permission() || devicePicker.getSelectedItemPosition()<0 || service==null || service.hasConnection()) return;
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);
        Intent intent=new Intent(this,PanoramaService.class).putExtra("address",devices.get(devicePicker.getSelectedItemPosition()).getAddress());
        startForegroundService(intent);
    }
    private void takePosition(String prefix) { if (!positionReady() || !service.state.positionSafe()) throw new IllegalArgumentException("Position erst nach sicherer Zielankunft übernehmen"); preview.discardSelection(); inputs.get(prefix+"Pan").setText(""+(-service.state.pan/1000.0)); inputs.get(prefix+"Tilt").setText(""+(service.state.tilt/1000.0)); Toast.makeText(this,prefix.equals("start")?"Startposition gespeichert":"Endposition gespeichert",Toast.LENGTH_SHORT).show(); }
    private void migrateTiltSign() {
        if(!preferences.getBoolean("tiltPositiveUp",false)) return;
        SharedPreferences.Editor editor=preferences.edit();
        for(String key:new String[]{"startTilt","endTilt"}) if(preferences.contains(key)) {
            try { editor.putString(key,""+(-Double.parseDouble(preferences.getString(key,"0").replace(',','.')))); }
            catch(NumberFormatException ignored) { }
        }
        try {
            String low=preferences.getString("tiltMin","-45"),high=preferences.getString("tiltMax","45");
            editor.putString("tiltMin",""+(-Double.parseDouble(high.replace(',','.'))));
            editor.putString("tiltMax",""+(-Double.parseDouble(low.replace(',','.'))));
        } catch(NumberFormatException ignored) { }
        editor.putBoolean("tiltPositiveUp",false).apply();
    }
    private static long newRunId() { return new SecureRandom().nextInt(Integer.MAX_VALUE-1)+1L; }
    private CameraProfile editedProfile() { return new CameraProfile(inputs.get("profileName").getText().toString().trim(),number("focal"),number("width"),number("height"),integer("settle"),integer("exposure")); }
    private void calculate() {
        refreshEndpoints();
        CameraProfile profile=editedProfile();
        pendingPlan=PanoramaPlan.create(newRunId(),modePicker.getSelectedItemPosition()==1,number("startPan"),number("startTilt"),number("endPan"),number("endTilt"),profile.fovX(),profile.fovY(),number("overlap"),integer("settle"),integer("planPulse"),integer("exposure"),integer("speed"),stopOnLoss.isChecked());
        if (service!=null && service.state.limitsKnown) pendingPlan.validate(service.state);
        preview.setPlan(pendingPlan);
        double seconds=pendingPlan.total()*(pendingPlan.settleMs+pendingPlan.pulseMs+pendingPlan.exposureMs)/1000.0;
        planSummary.setText(String.format(Locale.GERMANY,"Bildwinkel %.3f° × %.3f°\n%d Aufnahmen · %d Spalten/max. Bandbilder · %d Zeilen/Bänder\nMindestens %.1f min plus Fahrzeiten (Schätzung)",profile.fovX(),profile.fovY(),pendingPlan.total(),pendingPlan.columns,pendingPlan.rows,seconds/60)); saveInputs();
        render();
    }
    private void loadPlan() {
        if(preview.selection.pending) throw new IllegalArgumentException("Grafische Auswahl zuerst übernehmen oder verwerfen");
        calculate(); PanoramaPlan plan=pendingPlan;
        confirm("Plan übertragen?",plan.total()+" Aufnahmen mit "+editedProfile().name+". Nach der Bestätigung „Aufnahme starten“ drücken. Übertragen allein fährt das Gerät nicht.",() -> service.loadPlan(plan));
    }
    private void loadProfiles() {
        try {
            JSONArray data=new JSONArray(preferences.getString("profiles","[]"));
            for (int i=0;i<data.length();i++) { JSONObject p=data.getJSONObject(i); profiles.add(new CameraProfile(p.getString("name"),p.getDouble("focal"),p.getDouble("width"),p.getDouble("height"),p.getInt("settle"),p.getInt("exposure"))); }
        } catch (JSONException|IllegalArgumentException ignored) { profiles.clear(); }
        if (profiles.isEmpty()) {
            for (int f:new int[]{400,200,135,35,28}) profiles.add(new CameraProfile("Sony A7RII "+f+" mm",f,24,35.9,f==28?300:700,f==28?300:500));
            profiles.add(new CameraProfile("Lumix FZ200 24×",600,26.25,35,250,700));
            profiles.add(new CameraProfile("Lumix FZ200 12×",350,26.25,35,300,700));
            profiles.add(new CameraProfile("Canon G9 12×",420,26.25,35,300,300));
        }
    }
    private void updateProfilePicker() { profilePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,profiles)); String name=preferences.getString("profileName","Sony A7RII 400 mm"); for(int i=0;i<profiles.size();i++) if(profiles.get(i).name.equals(name)) { profilePicker.setSelection(i); break; } }
    private void applyProfile() {
        CameraProfile p=profiles.get(profilePicker.getSelectedItemPosition());
        inputs.get("profileName").setText(p.name); inputs.get("focal").setText(""+p.focal); inputs.get("width").setText(""+p.width); inputs.get("height").setText(""+p.height);
        inputs.get("settle").setText(""+p.settleMs); inputs.get("exposure").setText(""+p.exposureMs);
        saveInputs(); Toast.makeText(this,"Profil aktiv: "+p.name,Toast.LENGTH_SHORT).show();
    }
    private void saveProfile() {
        CameraProfile p=editedProfile(); int index=-1; for (int i=0;i<profiles.size();i++) if (profiles.get(i).name.equals(p.name)) index=i;
        if (index>=0) profiles.set(index,p); else profiles.add(p); persistProfiles(); updateProfilePicker();
    }
    private void persistProfiles() {
        try { JSONArray data=new JSONArray(); for (CameraProfile p:profiles) data.put(new JSONObject().put("name",p.name).put("focal",p.focal).put("width",p.width).put("height",p.height).put("settle",p.settleMs).put("exposure",p.exposureMs)); preferences.edit().putString("profiles",data.toString()).apply(); }
        catch (JSONException ex) { throw new IllegalArgumentException("Profile konnten nicht gespeichert werden",ex); }
    }
    private void importConfig() { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),61); }
    private void parseConfig(InputStream input) throws Exception {
        org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser(); parser.setInput(input,"UTF-8"); Map<String,String> values=new HashMap<>();
        int event; while ((event=parser.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) if (event==org.xmlpull.v1.XmlPullParser.START_TAG) {
            String tag=parser.getName(); if (tag.matches("(FocalLength|ChipWidth|ChipHeight|VibrationDelay|ExposureDelay)_[1-9]")) values.put(tag,parser.nextText().trim());
        }
        if (!values.containsKey("FocalLength_4")) throw new IllegalArgumentException("Keine Windows-Kameraprofile gefunden");
        int[] indices={4,5,6,7,8,values.containsKey("FocalLength_9")?9:1,2,3}; List<CameraProfile> imported=new ArrayList<>();
        for (int i=0;i<indices.length;i++) {
            int n=indices[i]; CameraProfile old=profiles.get(Math.min(i,profiles.size()-1));
            double focal=Double.parseDouble(values.getOrDefault("FocalLength_"+n,""+old.focal));
            imported.add(new CameraProfile(old.name,focal,Double.parseDouble(values.getOrDefault("ChipWidth_"+n,""+old.width)),Double.parseDouble(values.getOrDefault("ChipHeight_"+n,""+old.height)),
                (int)Math.round(Double.parseDouble(values.getOrDefault("VibrationDelay_"+n,""+(old.settleMs/1000.0)))*1000),
                (int)Math.round(Double.parseDouble(values.getOrDefault("ExposureDelay_"+n,""+(old.exposureMs/1000.0)))*1000)));
        }
        profiles.clear(); profiles.addAll(imported); persistProfiles(); updateProfilePicker();
    }
    private void refreshRuns() { if (service!=null) runPicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,service.captures.runs())); }
    private void exportCaptures(boolean xml) {
        CaptureStore.Run run=(CaptureStore.Run)runPicker.getSelectedItem(); if (run==null) throw new IllegalArgumentException("Keine Aufnahmen gespeichert; Läufe laden");
        pendingExport=service.captures.export(run.key,xml);
        prepareExport();
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(xml?"application/xml":"text/csv").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Panorama-"+run.key+(xml?".xml":".csv")),60);
    }
    private void exportSupport() {
        try { pendingExport="PanoramaControl "+BuildConfig.VERSION_NAME+"\n"+statusText()+"\n"+service.diagnostics+"\n"+service.support.snapshot(); }
        catch (IOException ex) { throw new IllegalArgumentException(ex.getMessage(),ex); }
        prepareExport();
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Panorama-Diagnose.txt"),60);
    }
    private void prepareExport() {
        try {
            File file=File.createTempFile("export-",".snapshot",getCacheDir());
            try (OutputStream out=new FileOutputStream(file)) { out.write(pendingExport.getBytes(StandardCharsets.UTF_8)); }
            pendingExportName=file.getName(); pendingExport=null;
        } catch (IOException ex) { throw new IllegalArgumentException("Exportsnapshot nicht speicherbar",ex); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if (result!=RESULT_OK || data==null || data.getData()==null) return;
        try {
            if (request==60 && pendingExportName!=null) {
                File snapshot=new File(getCacheDir(),pendingExportName);
                try (OutputStream output=getContentResolver().openOutputStream(data.getData(),"wt")) {
                    if (output==null) throw new IOException("Datei nicht schreibbar");
                    try (InputStream source=new FileInputStream(snapshot)) {
                        byte[] buffer=new byte[8192]; int count;
                        while ((count=source.read(buffer))!=-1) output.write(buffer,0,count);
                    }
                }
                if (!snapshot.delete()) snapshot.deleteOnExit();
                pendingExportName=null; Toast.makeText(this,"Datei gespeichert",Toast.LENGTH_SHORT).show();
            } else if (request==61) try (InputStream input=getContentResolver().openInputStream(data.getData())) { if (input==null) throw new IOException("Datei nicht lesbar"); parseConfig(input); }
        } catch (Exception ex) { Toast.makeText(this,"Dateifehler: "+ex.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private void migrateTiltDirection() {
        if(preferences.getBoolean("tiltDevicePositiveUp",false)) return;
        SharedPreferences.Editor editor=preferences.edit();
        for(String key:new String[]{"startTilt","endTilt"}) if(preferences.contains(key)) {
            try { editor.putString(key,""+(-Double.parseDouble(preferences.getString(key,"").replace(',','.')))); }
            catch(NumberFormatException ignored) { editor.putString(key,""); }
        }
        try {
            String low=preferences.getString("tiltMin","-45"),high=preferences.getString("tiltMax","45");
            editor.putString("tiltMin",""+(-Double.parseDouble(high.replace(',','.'))));
            editor.putString("tiltMax",""+(-Double.parseDouble(low.replace(',','.'))));
        } catch(NumberFormatException ignored) { editor.remove("tiltMin").remove("tiltMax"); }
        editor.putBoolean("tiltDevicePositiveUp",true).apply();
    }
    private boolean tiltCalibrationReady() {
        return ready() && service.state.tiltCalibrationKnown && service.state.positionSafe() && service.state.state.equals("IDLE") && !service.manualMoving && !service.manualTrigger;
    }
    private void setTiltZero(boolean reset) {
        if(!tiltCalibrationReady()) throw new IllegalArgumentException("Tilt-Nullpunkt nur im Stillstand ohne Geräteauftrag setzen");
        confirm(reset?"Tilt-Werkreferenz wiederherstellen?":"Aktuelle Tiltposition als 0° setzen?",
            (reset?"Der gespeicherte Tilt-Nullpunkt wird zurückgesetzt.":"Kamera waagerecht ausgerichtet? Die aktuelle Position wird als Tilt 0° gespeichert.")+" Das Gerät fährt dabei nicht. Die Limits bleiben an denselben physischen Stellen. Der Planentwurf wird geleert.",() -> service.setTiltZero(reset));
    }
    private void setTiltLimits() {
        int low=DeviceState.toTiltDevice(number("tiltMin")), high=DeviceState.toTiltDevice(number("tiltMax"));
        if (low>=high || service.state.tilt<low || service.state.tilt>high) throw new IllegalArgumentException("Grenzen müssen die aktuelle Tiltposition einschließen");
        if (!(service.state.state.equals("IDLE")||service.state.state.equals("STOPPED")||service.state.state.equals("COMPLETED")) || service.manualMoving || service.manualTrigger)
            throw new IllegalArgumentException("Grenzen nur im sicheren Stillstand ändern");
        if(service.state.tiltCalibrationKnown && (low<service.state.tiltAbsoluteMin || high>service.state.tiltAbsoluteMax))
            throw new IllegalArgumentException("Grenzen außerhalb des vermessenen Gerätebereichs");
        confirm("Vermessene Grenzen übernehmen?","Diese Werte müssen mit montiertem Objektiv und Steckern mechanisch geprüft sein.",() -> service.command("SET_TILT_LIMITS",""+low,""+high));
    }
}
