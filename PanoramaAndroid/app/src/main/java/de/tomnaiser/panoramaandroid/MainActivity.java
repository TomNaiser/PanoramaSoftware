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
    private Button tiltRangeButton;
    private Button alignmentReset;
    private Button recoveryAction, referenceReview;
    private Button restRecoveryAction;
    private Button columnRetryAction;
    private TextView captureStabilityActive;
    private String displayedCaptureStability="";
    private AlertDialog incidentDialog;
    private AlertDialog restDialog;
    private long dialogIncidentId=-1;
    private LinearLayout navigationBar;
    private TextView jogTempoLabel;
    private LinearLayout endpointBar,runActionBar;
    private PlotWorkspace planWorkspace,runWorkspace;
    private View[] pageViews;
    private LinearLayout headerBar,tabBar;
    private Button emergencyButton;
    private android.view.ViewGroup emergencyHome;
    private android.view.ViewGroup.LayoutParams emergencyHomeParams;
    private int emergencyHomeIndex;
    private final List<Button> tabs=new ArrayList<>();
    private TextView endpointSummary, alignmentHint, runProgress, activeProfile;
    private TextView tiltReference;
    private String displayedTiltLimits="";
    private ProgressBar captureProgress;
    private static final int INK=Color.rgb(25,43,59), ACCENT=Color.rgb(18,105,116), MUTED=Color.rgb(83,103,119);
    private int tiltRange;
    private TextView connection, live, message, planSummary, diagnostics, papywizardStatus;
    private LinearLayout[] pages;
    private PlanPreview preview, runPreview;
    private TextView plotSelection, runLegend, runTitle, planDeviceStatus;
    private TextView recoveryHint;
    private Button recoveryCheck;
    private long recoveryOfferRun;
    private Button warningPoints;
    private final Handler plotHandler=new Handler(Looper.getMainLooper());
    private long plottedRun=-1;
    private String savedPlotKey;
    private CaptureStore.PlotSnapshot savedPlot;
    private boolean plotReady;
    private boolean planRangeInitialized;
    private boolean restoreEndpoints;
    private int observedDraftRevision=-1;
    private final Runnable recalculatePlot=() -> {
        try { calculate(); }
        catch(RuntimeException ex) { pendingPlan=null; preview.setPlan(null); planSummary.setText(!endpointSet("start") || !endpointSet("end")?UiText.NO_PLAN_YET_ALIGN_AND_REMEMBER_START_AND.text():UiText.INVALID_PLAN.text()+ex.getMessage()); render(); }
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
    @Override protected void attachBaseContext(Context base) {
        UiText.select(base.getSharedPreferences("settings",MODE_PRIVATE).getString(UiText.PREFERENCE,"de"));
        android.content.res.Configuration config=new android.content.res.Configuration(base.getResources().getConfiguration());
        config.setLocale(UiText.locale());
        super.attachBaseContext(base.createConfigurationContext(config));
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        preferences = getSharedPreferences("settings",MODE_PRIVATE);
        restoreEndpoints=true;
        observedDraftRevision=state==null?-1:state.getInt("draftRevision",-1);
        migrateTiltSign();
        migrateTiltDirection();
        tiltRange=Math.max(0,Math.min(2,preferences.getInt("tiltRange",0)));
        if (state!=null) { pendingExportName=state.getString("exportName"); savedPlotKey=state.getString("savedPlotKey"); plottedRun=state.getLong("plottedRun",-1); }
        loadProfiles(); buildUi(); refreshDevices();
        plotReady=true; recalculatePlot.run(); preview.setEditMode(3); applyTiltRange();
        preview.restore(state,"planViewport"); runPreview.restore(state,"runViewport");
        if(state!=null) { planWorkspace.setExpanded(state.getBoolean("planPlotExpanded")); runWorkspace.setExpanded(state.getBoolean("runPlotExpanded")); }
        updatePlotSizeLabels();
        planRangeInitialized=state!=null && state.getBoolean("limitsKnown",false);
        updateProposal();
        selectPage(state == null ? 0 : state.getInt("page",0));
    }
    @Override protected void onStart() {
        super.onStart(); bound = bindService(new Intent(this,PanoramaService.class),binding,BIND_AUTO_CREATE);
    }
    @Override protected void onStop() {
        if(service!=null) service.endJog();
        if(incidentDialog!=null) { incidentDialog.dismiss(); incidentDialog=null; }
        if(restDialog!=null) { restDialog.dismiss();restDialog=null; }
        plotHandler.removeCallbacks(recalculatePlot);
        saveInputs(); if (service != null) service.removeObserver(observer);
        if (bound) { unbindService(binding); bound = false; } service = null;
        super.onStop();
    }
    @Override protected void onPause() { if(service!=null) service.endJog(); super.onPause(); }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused); if(!focused && service!=null) service.endJog();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putLong("processToken",PROCESS_TOKEN); state.putInt("page",selectedPage); state.putBoolean("planPlotExpanded",planWorkspace.expanded()); state.putBoolean("runPlotExpanded",runWorkspace.expanded()); state.putInt("draftRevision",observedDraftRevision); state.putBoolean("limitsKnown",service!=null && service.state.limitsKnown); state.putString("exportName",pendingExportName); state.putString("savedPlotKey",savedPlotKey); state.putLong("plottedRun",plottedRun); preview.save(state,"planViewport"); runPreview.save(state,"runViewport"); saveInputs(); super.onSaveInstanceState(state); }
    private AlertDialog chooseLanguage() {
        UiText.Language[] languages=UiText.Language.values();
        String[] labels=new String[languages.length];
        for(int i=0;i<labels.length;i++) labels[i]=languages[i].label;
        AlertDialog picker=new AlertDialog.Builder(this).setTitle(UiText.LANGUAGE_SPRACHE.text())
            .setSingleChoiceItems(labels,UiText.language().ordinal(),(dialog,index) -> {
                dialog.dismiss();
                UiText.Language language=languages[index];
                if(language==UiText.language()) return;
                saveInputs();
                preferences.edit().putString(UiText.PREFERENCE,language.tag).apply();
                UiText.select(language.tag);
                if(service!=null) service.languageChanged();
                recreate();
            }).setNegativeButton(UiText.CANCEL.text(),null).create();
        picker.show();return picker;
    }
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
        LinearLayout header=new LinearLayout(this); headerBar=header; root.addView(header);
        LinearLayout identity=vertical(); header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
        TextView heading=text("PanoramaControl",landscape?16:20); identity.addView(heading);
        heading.setContentDescription(UiText.PANORAMACONTROL_VERSION.text()+BuildConfig.VERSION_NAME);
        connection = text(UiText.NOT_CONNECTED.text(),15);
        message = text(UiText.READY.text(),14);
        connection.setTextSize(12); connection.setPadding(0,0,0,dp(4)); identity.addView(connection);
        message.setMaxLines(landscape?1:2); message.setTextSize(13);
        Button emergency = button(header,UiText.E_STOP.text(),() -> service.emergencyStop(),() -> service != null && service.connected);
        emergencyButton=emergency;
        recoveryAction=button(root,UiText.CHECK_DEVICE_STATE_ACKNOWLEDGE_ERROR.text(),() -> {
            if(service.incident.needsAcknowledgement()) showIncident(); else service.recoverDevice();
        },() -> service!=null && !service.recovering);
        recoveryAction.setVisibility(View.GONE);
        emergency.setLayoutParams(new LinearLayout.LayoutParams(dp(112),dp(52)));
        emergency.setTextColor(Color.WHITE); emergency.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(160,20,30)));
        LinearLayout nav = new LinearLayout(this); tabBar=nav;
        if(landscape) { header.removeView(emergency); header.addView(nav,new LinearLayout.LayoutParams(0,dp(56),2)); header.addView(emergency); }
        else root.addView(nav);
        emergencyHome=(android.view.ViewGroup)emergency.getParent(); emergencyHomeIndex=emergencyHome.indexOfChild(emergency); emergencyHomeParams=emergency.getLayoutParams();
        root.addView(message);
        String[] names = {UiText.DEVICE.text(),UiText.MANUAL.text(),"Plan",UiText.RUN_2.text(),"Extras"};
        for (int i=0;i<names.length;i++) {
            final int index=i; Button tab = new Button(this); tab.setText(names[i]); tab.setTextSize(12); tab.setAllCaps(false); tab.setMinWidth(0); tab.setMinimumWidth(0); tab.setPadding(0,0,0,0); tabs.add(tab);
            tab.setOnClickListener(v -> selectPage(index)); nav.addView(tab,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        root.addView(container,new LinearLayout.LayoutParams(-1,0,1)); pages = new LinearLayout[5]; pageViews=new View[5];
        for (int i=0;i<5;i++) {
            ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); pages[i]=vertical(); scroll.addView(pages[i]);
            container.addView(scroll); scroll.setTag(i); pageViews[i]=scroll;
        }
        LinearLayout device = pages[0]; device.addView(text(UiText.CONNECTION.text(),20));
        devicePicker = new Spinner(this); device.addView(devicePicker);
        button(device,UiText.LOAD_DEVICES.text(),this::refreshDevices,() -> service==null || !service.hasConnection());
        button(device,UiText.CONNECT.text(),this::connect,() -> service!=null && !service.hasConnection() && !devices.isEmpty());
        button(device,UiText.DISCONNECT.text(),() -> service.disconnect(),() -> service!=null && service.hasConnection());
        device.addView(text(UiText.PAIR_RNBT_IN_ANDROID_FIRST_DISCONNECT_ANY_EXISTING.text(),14));
        live = text(UiText.NO_STATUS_DATA_YET.text(),16); device.addView(live);
        primary(button(device,UiText.CONTINUE_TO_PANORAMA.text(),() -> selectPage(2),() -> true));
        LinearLayout manual=pages[1]; manual.addView(text(UiText.MANUAL_CONTROL.text(),20));
        manual.addView(text(UiText.HOLD_AN_ARROW_TO_MOVE_SLOWLY_RELEASE_TO.text(),14));
        TextView manualHint=text("",14); manualHint.setTag("alignmentHint"); manual.addView(manualHint);
        button(manual,UiText.RESET_STOPPED_JOB.text(),this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        input(manual,"pulse",UiText.CAMERA_PULSE_MS.text(),"30");
        input(manual,"manualPanSpeed",UiText.PAN_TARGET_MOVES_SPEED_1_1000.text(),"500");
        manual.addView(text(UiText.PAN_500_FOR_THE_TESTED_CONTINUOUS_MOVEMENT_APPROX.text(),14));
        input(manual,"manualSpeed",UiText.TILT_TARGET_MOVES_SPEED_1_1000.text(),"500");
        input(manual,"jogSpeed",UiText.TILT_JOG_MAXIMUM_SPEED_S.text(),"5");
        input(manual,"panJogSpeed",UiText.PAN_JOG_MAXIMUM_SPEED_S.text(),"10");
        input(manual,"rememberSettleMs",UiText.SETTLING_BEFORE_REMEMBERING_POSITION_MS_0_5000.text(),"1000");
        button(manual,UiText.TRIGGER_CAMERA_ONCE.text(),() -> service.trigger(integer("pulse")),() -> service!=null && service.manualAllowed() && !service.manualMoving && service.state.positionSafe());
        button(manual,UiText.MOTION_DIAGNOSTICS.text(),() -> service.command("GET_DIAGNOSTICS"),this::ready);
        button(pages[2],UiText.REMOVE_PLAN.text(),this::removePlan,() -> service==null || !service.hasConnection() || (ready() && !service.manualMoving && !service.manualTrigger && (service.state.state.equals("IDLE") || PlanAbort.allowed(service.state.state))));
        LinearLayout camera=card(pages[2],UiText.TEXT_1_CHOOSE_CAMERA.text());
        referenceReview=button(pages[2],UiText.CHECK_AND_CONFIRM_DRAFT_REFERENCE.text(),() -> confirm(UiText.DRAFT_REFERENCE_CHECKED.text(),
            UiText.START_AND_END_POSITIONS_HAVE_BEEN_CHECKED_AFTER.text(),
            () -> service.confirmDraftReference()),() -> service!=null && service.fresh());
        referenceReview.setVisibility(View.GONE);
        profilePicker=new Spinner(this); camera.addView(profilePicker); updateProfilePicker();
        primary(button(camera,UiText.USE_CAMERA_PROFILE.text(),this::applyProfile,() -> true));
        activeProfile=text("",13); camera.addView(activeProfile);
        LinearLayout profileDetails=details(camera,UiText.EDIT_IMPORT_PROFILE.text());
        input(profileDetails,"profileName",UiText.PROFILE_NAME.text(),"Sony A7RII 400 mm");
        input(profileDetails,"focal",UiText.FOCAL_LENGTH_MM.text(),"400"); input(profileDetails,"width",UiText.SENSOR_WIDTH_MM.text(),"24"); input(profileDetails,"height",UiText.SENSOR_HEIGHT_MM.text(),"35.9");
        button(profileDetails,UiText.SAVE_PROFILE.text(),this::saveProfile,() -> true);
        button(profileDetails,UiText.IMPORT_WINDOWS_CONFIGURATION.text(),this::importConfig,() -> true);
        LinearLayout plan=card(pages[2],UiText.ALIGNMENT_ENDPOINTS.text());
        alignmentHint=text(UiText.SELECT_POINT_MOVE_CHECK_CAMERA_REMEMBER_START_END.text(),13); plan.addView(alignmentHint);
        alignmentReset=button(plan,UiText.RESET_JOB_CHANGE_ENDPOINTS.text(),this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        alignmentReset.setVisibility(View.GONE);
        LinearLayout graph=vertical(), steering=plan;
        graph.setBackground(surface(Color.WHITE)); graph.setPadding(dp(4),0,dp(4),0);
        LinearLayout selectionRow=new LinearLayout(this); graph.addView(selectionRow);
        plotSelection=text(UiText.PAN_VIEW.text(),14); selectionRow.addView(plotSelection,new LinearLayout.LayoutParams(0,-2,1));
        plotSelection.setTextSize(12); plotSelection.setSingleLine(true); plotSelection.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tiltRangeButton=button(selectionRow,"Tilt ±25°",this::chooseTiltRange,() -> true);
        tiltRangeButton.setTextSize(12); tiltRangeButton.setLayoutParams(new LinearLayout.LayoutParams(dp(106),dp(48)));
        preview=new PlanPreview(this); plotControls(graph,preview);
        graph.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
        preview.setProposalListener(this::updateProposal);
        preview.setDriveAction(() -> safely(this::driveSelection));
        preview.setListener((start,pan,tilt) -> {
            String prefix=start?"start":"end";
            inputs.get(prefix+"Pan").setText(""+pan); inputs.get(prefix+"Tilt").setText(""+tilt);
            refreshEndpoints(); schedulePlot();
        });
        LinearLayout remember=new LinearLayout(this); steering.addView(remember);
        button(remember,UiText.REMEMBER_START.text(),() -> takePosition("start"),() -> positionReady() && service.state.positionSafe() && service.endpointCaptureAllowed());
        button(remember,UiText.REMEMBER_END.text(),() -> takePosition("end"),() -> positionReady() && service.state.positionSafe() && service.endpointCaptureAllowed());
        endpointSummary=text("",14); plan.addView(endpointSummary);
        LinearLayout revisit=new LinearLayout(this); plan.addView(revisit);
        button(revisit,UiText.MOVE_TO_START.text(),() -> driveEndpoint("start"),() -> positionReady() && endpointSet("start"));
        button(revisit,UiText.MOVE_TO_END.text(),() -> driveEndpoint("end"),() -> positionReady() && endpointSet("end"));
        if(landscape) {
            for(int rowIndex=0;rowIndex<steering.getChildCount();rowIndex++) {
                View row=steering.getChildAt(rowIndex);
                if(row instanceof LinearLayout) for(int child=0;child<((LinearLayout)row).getChildCount();child++) {
                    View control=((LinearLayout)row).getChildAt(child); control.getLayoutParams().height=dp(48);
                    if(control instanceof Button && ((Button)control).getText().length()>1) { ((Button)control).setTextSize(12); ((Button)control).setSingleLine(true); }
                }
            }
        }
        LinearLayout plotOptions=details(plan,UiText.CHART_VIEW_ZOOM.text());
        LinearLayout graphMode=new LinearLayout(this); plotOptions.addView(graphMode);
        button(graphMode,UiText.SELECT_POINT.text(),() -> preview.setEditMode(3),() -> true);
        button(graphMode,UiText.PAN_VIEW_2.text(),() -> preview.setEditMode(0),() -> true);
        button(plotOptions,UiText.FULL_PAN_TILT_RANGE.text(),() -> { tiltRange=2; applyTiltRange(); },() -> true);
        plotOptions.addView(text(UiText.TAP_SELECT_TARGET_DRAG_PAN_VIEW_TWO_FINGERS.text(),13));
        LinearLayout capture=card(pages[2],UiText.TEXT_3_CHECK_CAPTURE.text());
        planSummary=text(UiText.NO_PLAN_CALCULATED_YET.text(),15); capture.addView(planSummary);
        planDeviceStatus=text(UiText.CONNECT_TO_THE_DEVICE_TO_UPLOAD.text(),13); capture.addView(planDeviceStatus);
        primary(button(capture,UiText.UPLOAD_PLAN.text(),this::loadPlan,() -> !preview.selection.pending && pendingPlan!=null && positionReady() && service.state.positionSafe() && service.state.state.equals("IDLE")));
        primary(button(capture,UiText.START_CAPTURE.text(),this::startLoadedPlan,this::canStartLocalPlan));
        LinearLayout settings=details(capture,UiText.CAPTURE_SETTINGS_ENDPOINTS.text());
        input(settings,"overlap",UiText.OVERLAP.text(),"20");
        modePicker=new Spinner(this); modePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{UiText.GRID_COLUMN_RETURN.text(),UiText.SPHERICAL_TILT_BANDS.text()}));
        modePicker.setSelection(preferences.getInt("mode",0)); settings.addView(modePicker);
        input(settings,"startPan",UiText.START_PAN.text(),""); input(settings,"startTilt",UiText.START_TILT.text(),"");
        button(settings,UiText.EDIT_START_POINT_ON_CHART.text(),() -> preview.setEditMode(1),() -> true);
        input(settings,"endPan",UiText.END_PAN.text(),""); input(settings,"endTilt",UiText.END_TILT.text(),"");
        button(settings,UiText.EDIT_END_POINT_ON_CHART.text(),() -> preview.setEditMode(2),() -> true);
        button(settings,UiText.APPLY_GRAPHICAL_PLAN_CHANGES.text(),preview::commitSelection,() -> preview.selection.pending && (preview.editMode()==1 || preview.editMode()==2));
        button(settings,UiText.BACK_TO_ALIGNMENT.text(),() -> preview.setEditMode(3),() -> true);
        input(settings,"settle",UiText.SETTLING_TIME_MS.text(),"700"); input(settings,"planPulse",UiText.TRIGGER_DURATION_MS.text(),"30"); input(settings,"exposure",UiText.EXPOSURE_WAIT_MS.text(),"500");
        input(settings,"speed",UiText.CAPTURE_RUN_SPEED_1_2000.text(),"500");
        settings.addView(text(UiText.TEXT_1000_PREVIOUS_FULL_SPEED_1500_150_2000_200.text(),13));
        input(settings,"warningPan",UiText.PAN_WARNING_THRESHOLD_0_044_10.text(),"0.5");
        input(settings,"warningTilt",UiText.TILT_WARNING_THRESHOLD_0_044_10.text(),"0.5");
        button(settings,UiText.SAVE_WARNING_THRESHOLDS_TO_DEVICE.text(),() -> service.saveWarningTolerances(number("warningPan"),number("warningTilt")),
            () -> ready() && service.state.warningSettingsSupported && !service.manualMoving && !service.manualTrigger &&
                (service.state.state.equals("IDLE") || service.state.state.equals("STOPPED")));
        settings.addView(text(UiText.WARNING_THRESHOLDS_ONLY_AFFECT_MARKERS_AND_THE_ALERT.text(),13));
        LinearLayout stability=details(settings,UiText.POSITION_MONITORING_DURING_CAPTURE.text());
        captureStabilityActive=text(UiText.CONNECT_WITH_FIRMWARE_2_4_71_OR_LATER.text(),13);stability.addView(captureStabilityActive);
        input(stability,"captureSpan",UiText.MAXIMUM_ANGULAR_VARIATION_OF_BOTH_AXES_0_044.text(),"0.25");
        input(stability,"capturePanDrift",UiText.ALLOWED_PAN_DRIFT_DURING_CAPTURE_0_044_1.text(),"0.25");
        input(stability,"captureTiltDrift",UiText.ALLOWED_TILT_DRIFT_DURING_CAPTURE_0_044_1.text(),"0.4");
        input(stability,"captureExtraWait",UiText.MAXIMUM_EXTRA_WAIT_FOR_STABILITY_S_1_120.text(),"15");
        button(stability,UiText.READ_STABILITY_LIMITS.text(),() -> service.readCaptureStability(),
            () -> ready() && service.state.captureStability!=null);
        button(stability,UiText.SAVE_STABILITY_LIMITS_TO_DEVICE.text(),() -> service.saveCaptureStability(number("captureSpan"),number("capturePanDrift"),number("captureTiltDrift"),number("captureExtraWait")),
            () -> service!=null && service.canConfigureStability());
        stability.addView(text(UiText.THE_SAME_LIMITS_APPLY_BEFORE_AND_DURING_CAPTURE.text(),13));
        CheckBox qualityPause=new CheckBox(this);qualityPause.setText(UiText.PAUSE_AFTER_THE_CURRENT_IMAGE_IF_TARGET_DEVIATION.text());
        qualityPause.setChecked(preferences.getBoolean("qualityPause",true));
        qualityPause.setOnCheckedChangeListener((button,checked) -> preferences.edit().putBoolean("qualityPause",checked).apply());settings.addView(qualityPause);
        settings.addView(text(UiText.THE_APP_REQUESTS_A_PAUSE_REQUIRING_ACKNOWLEDGEMENT_WHEN.text(),13));
        stopOnLoss=new CheckBox(this); stopOnLoss.setText(UiText.STOP_AFTER_5_S_IF_CONNECTION_IS_LOST.text());
        stopOnLoss.setChecked(preferences.getBoolean("stopOnLoss",false)); settings.addView(stopOnLoss);
        settings.addView(text(UiText.WHEN_UNCHECKED_THE_ARDUINO_CONTINUES_THE_JOB_EVEN.text(),14));
        button(settings,UiText.CALCULATE_PREVIEW.text(),this::calculate,() -> true);
        LinearLayout run=card(pages[3],null),runGraph=vertical(); runGraph.setBackground(surface(Color.WHITE)); runGraph.setPadding(dp(4),0,dp(4),0);
        runTitle=text(UiText.CURRENT_JOB.text(),14); runTitle.setSingleLine(true); runTitle.setPadding(0,0,0,0); runGraph.addView(runTitle);
        runProgress=text(UiText.NO_CAPTURE_STARTED_YET.text(),13); runProgress.setSingleLine(true); runProgress.setPadding(0,0,0,dp(4)); runGraph.addView(runProgress);
        captureProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); runGraph.addView(captureProgress,new LinearLayout.LayoutParams(-1,dp(8)));
        LinearLayout runActions=new LinearLayout(this); run.addView(runActions);
        primary(button(runActions,"Pause",() -> service.runCommand("PAUSE"),() -> savedPlot==null && ready() && service.state.state.equals("RUNNING")));
        primary(button(runActions,UiText.RESUME.text(),() -> service.runCommand("RESUME"),() -> savedPlot==null && ready() && service.state.positionSafe() && service.state.state.equals("PAUSED") && !service.manualMoving && !service.manualTrigger));
        recoveryHint=text("",14);recoveryHint.setTag("recoveryHint");run.addView(recoveryHint);
        recoveryCheck=button(run,UiText.ACKNOWLEDGE_ERROR_CHECK_DEVICE.text(),() -> {
            if(service.incident.needsAcknowledgement()) showIncident();else service.recoverDevice();
        },() -> service!=null && !service.recoveryInProgress() && !service.busy);
        recoveryCheck.setTag("recoveryCheck");
        restRecoveryAction=button(run,UiText.RECOVER_PANORAMA.text(),this::prepareRest,() -> savedPlot==null && service!=null && service.canPrepareRest());
        restRecoveryAction.setTag("restRecovery");
        columnRetryAction=button(run,UiText.REPEAT_COLUMN.text(),this::showColumnRetry,() -> savedPlot==null && service!=null && service.canRetryColumn());
        columnRetryAction.setTag("columnRetry");
        button(run,UiText.STOP_CHANGE_ENDPOINTS.text(),this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        button(runActions,"Stop",() -> confirm(UiText.STOP_JOB.text(),UiText.MOTORS_AND_CAMERA_OUTPUTS_WILL_BE_SWITCHED_OFF.text(),() -> service.runCommand("STOP")),() -> ready() && !service.state.state.equals("IDLE") && !service.state.state.startsWith("TIMELAPSE"));
        runPreview=new PlanPreview(this); plotControls(runGraph,runPreview);
        runPreview.setWarningListener(this::reviewCaptureWarning);
        warningPoints=new Button(this);warningPoints.setTag("warningPoints");warningPoints.setAllCaps(false);warningPoints.setTextSize(13);
        warningPoints.setMinHeight(0);warningPoints.setMinimumHeight(0);warningPoints.setPadding(dp(6),0,dp(6),0);
        warningPoints.setOnClickListener(v -> showCaptureWarnings());runGraph.addView(warningPoints,new LinearLayout.LayoutParams(-1,dp(36)));
        runGraph.addView(runPreview,new LinearLayout.LayoutParams(-1,0,1));
        runLegend=text(UiText.BLUE_TARGETS_PURPLE_MOVEMENT_TARGET_ORANGE_ACTUAL_POSITION.text(),13); run.addView(runLegend);
        button(run,UiText.SHOW_CURRENT_DEVICE_RUN.text(),() -> { savedPlotKey=null; savedPlot=null; plottedRun=-1; render(); },() -> service!=null);
        button(run,UiText.START_JOB.text(),this::startLoadedPlan,() -> savedPlot==null && canStartLoadedPlan());
        button(run,UiText.BACK_ONE_COLUMN.text(),() -> service.columns(false),() -> savedPlot==null && ready());
        button(run,UiText.FORWARD_ONE_COLUMN.text(),() -> service.columns(true),() -> savedPlot==null && ready());
        button(runActions,UiText.TIMING.text(),this::editRunTiming,() -> savedPlot==null && timingReady());
        button(run,UiText.REFRESH_DEVICE_STATE.text(),() -> service.command("GET_SUPPORT_BUNDLE"),this::ready);
        button(run,UiText.CLEAR_EXISTING_JOB.text(),() -> confirm(UiText.CLEAR_JOB.text(),UiText.THE_STORED_DEVICE_JOB_WILL_BE_RESET_LOCAL.text(),() -> service.runCommand("CLEAR_RUN")),this::ready);
        button(run,UiText.ABORT_PLAN_NEW_PLAN.text(),this::abortLoadedPlan,() -> ready() && PlanAbort.allowed(service.state.state));
        TextView runLive=text("",16); run.addView(runLive); runLive.setTag("live");
        LinearLayout extras=pages[4];
        button(extras,UiText.LANGUAGE_SPRACHE.text()+": "+UiText.language().label,this::chooseLanguage,() -> true).setTag("appLanguage");
        extras.addView(text("Timelapse",20));
        input(extras,"interval",UiText.TIMELAPSE_INTERVAL_MS.text(),"5000");
        input(extras,"tlPulse",UiText.TIMELAPSE_PULSE_DURATION_MS.text(),"30");
        button(extras,UiText.START_TIMELAPSE_2.text(),() -> confirm(UiText.START_TIMELAPSE.text(),UiText.THE_CAMERA_WILL_TRIGGER_AUTONOMOUSLY_AT_THE_CONFIGURED.text(),() -> service.timelapse(newRunId(),integer("interval"),integer("tlPulse"))),() -> ready() && service.state.state.equals("IDLE") && !service.manualMoving && !service.manualTrigger);
        button(extras,UiText.STOP_TIMELAPSE.text(),() -> service.runCommand("STOP_TIMELAPSE"),() -> ready() && service.state.state.equals("TIMELAPSE"));
        extras.addView(text(UiText.CAPTURES_AND_EXPORT.text(),20));
        papywizardStatus=text("",14);extras.addView(papywizardStatus);
        extras.addView(text(UiText.PAPYWIZARD_IS_BACKED_UP_AUTOMATICALLY_IN_THE_BACKGROUND.text(),14));
        button(extras,UiText.RETRY_AUTOMATIC_PAPYWIZARD_BACKUPS.text(),() -> service.rebuildPapywizard(),() -> service!=null);
        runPicker=new Spinner(this); extras.addView(runPicker);
        button(extras,UiText.LOAD_SAVED_RUNS.text(),this::refreshRuns,() -> service!=null);
        button(extras,UiText.SHOW_SELECTED_RUN_ON_CHART.text(),() -> {
            CaptureStore.Run selected=(CaptureStore.Run)runPicker.getSelectedItem();
            if(selected==null) throw new IllegalArgumentException(UiText.NO_CAPTURES_SAVED.text());
            savedPlotKey=selected.key; savedPlot=service.captures.plotSnapshot(selected.key);
            selectPage(3); render(); runPreview.wholeRange();
        },() -> service!=null);
        button(extras,UiText.SAVE_PAPYWIZARD_XML.text(),() -> exportCaptures(true),() -> service!=null);
        button(extras,UiText.SAVE_CAPTURE_MANIFEST_CSV.text(),() -> exportCaptures(false),() -> service!=null);
        button(extras,UiText.SAVE_INTERRUPTIONS_CSV.text(),this::exportInterruptions,() -> service!=null);
        extras.addView(text(UiText.RECOVERED_PARTIAL_JOBS_ARE_EXPORTED_TOGETHER_REPEATED_TARGETS.text(),14));
        extras.addView(text(UiText.CSV_IDENTIFIES_RECONSTRUCTED_TARGET_POSITIONS_AND_MISSING_HISTORICAL.text(),14));
        extras.addView(text(UiText.DIAGNOSTICS_AND_LIMITS.text(),20));
        CheckBox warningSound=new CheckBox(this);warningSound.setText(UiText.SOUND_ALERT_FOR_TARGET_CAPTURE_WARNINGS.text());
        warningSound.setChecked(preferences.getBoolean("captureWarningSound",true));
        warningSound.setOnCheckedChangeListener((button,checked) -> preferences.edit().putBoolean("captureWarningSound",checked).apply());extras.addView(warningSound);
        diagnostics=text(UiText.NO_DIAGNOSTICS_YET.text(),15); extras.addView(diagnostics);
        button(extras,UiText.REQUEST_SUPPORT_BUNDLE.text(),() -> service.command("GET_SUPPORT_BUNDLE"),this::ready);
        button(extras,UiText.SAVE_DIAGNOSTICS.text(),this::exportSupport,() -> service!=null);
        LinearLayout tiltSetup=card(extras,UiText.TILT_ZERO_POINT_LIMITS.text());
        tiltReference=text(UiText.CONNECT_TO_THE_DEVICE_TO_CONFIGURE.text(),15); tiltSetup.addView(tiltReference);
        tiltSetup.addView(text(UiText.LEVEL_THE_CAMERA_AND_SET_THIS_POSITION_AS.text(),14));
        button(tiltSetup,UiText.SET_CURRENT_POSITION_AS_TILT_0.text(),() -> setTiltZero(false),this::tiltCalibrationReady);
        button(tiltSetup,UiText.RESET_TILT_ZERO_POINT.text(),() -> setTiltZero(true),this::tiltCalibrationReady);
        input(tiltSetup,"tiltMin",UiText.TILT_LOWER_LIMIT.text(),"-45");
        input(tiltSetup,"tiltMax",UiText.TILT_UPPER_LIMIT.text(),"50");
        button(tiltSetup,UiText.SAVE_MEASURED_TILT_LIMITS.text(),this::setTiltLimits,this::tiltCalibrationReady);
        tiltSetup.addView(text(UiText.CHANGE_LIMITS_ONLY_AFTER_MECHANICAL_MEASUREMENT_WITH_THE.text(),14));
        // Keep endpoint capture beside the persistent arrows, even when the
        // camera profile or large plot has pushed alignment controls offscreen.
        steering.removeView(remember); endpointBar=remember;
        LinearLayout footer=new LinearLayout(this); footer.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL); footer.setGravity(android.view.Gravity.CENTER_VERTICAL); root.addView(footer);
        footer.addView(endpointBar,new LinearLayout.LayoutParams(landscape?0:-1,-2,landscape?2:0));
        if(steering.getChildCount()==0 && steering.getParent()!=null) ((android.view.ViewGroup)steering.getParent()).removeView(steering);
        navigationBar=new LinearLayout(this); navigationBar.setTag("navigationBar");
        navigationBar.setPadding(0,dp(4),0,dp(4)); footer.addView(navigationBar,new LinearLayout.LayoutParams(landscape?0:-1,-2,landscape?4:0));
        jogTempoLabel=text(UiText.HOLD_5_S.text(),12);
        navigationBar.addView(jogTempoLabel,new LinearLayout.LayoutParams(0,dp(48),1.3f));
        arrowButton(navigationBar,"←",UiText.PAN_LEFT.text(),0,-1);
        arrowButton(navigationBar,"→",UiText.PAN_RIGHT.text(),0,1);
        arrowButton(navigationBar,"↑",UiText.INCREASE_TILT.text(),1,1);
        arrowButton(navigationBar,"↓",UiText.DECREASE_TILT.text(),1,-1);
        run.removeView(runActions); runActionBar=runActions;
        footer.addView(runActionBar,new LinearLayout.LayoutParams(landscape?0:-1,-2,landscape?1:0));
        planWorkspace=installPlotWorkspace(container,2,graph);
        runWorkspace=installPlotWorkspace(container,3,runGraph);
        for(Map.Entry<String,EditText> entry:inputs.entrySet()) {
            String key=entry.getKey();
            if(key.equals("step") || key.equals("pulse") || key.equals("interval") || key.equals("tlPulse") || key.startsWith("tilt") || key.startsWith("warning")) continue;
            entry.getValue().addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
                public void onTextChanged(CharSequence s,int start,int before,int count) { if(plotReady) { refreshEndpoints(); schedulePlot(); } }
                public void afterTextChanged(Editable value) { }
            });
        }
        modePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) { preferences.edit().putInt("mode",position).apply();schedulePlot(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        stopOnLoss.setOnCheckedChangeListener((v,checked) -> { preferences.edit().putBoolean("stopOnLoss",checked).apply();schedulePlot(); });
        profilePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id) { if(plotReady) render(); }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        render();
    }
    private PlotWorkspace installPlotWorkspace(android.widget.FrameLayout container,int page,LinearLayout diagram) {
        ScrollView settings=(ScrollView)pages[page].getParent(); container.removeView(settings);
        PlotWorkspace workspace=new PlotWorkspace(this,diagram,settings);
        container.addView(workspace,new android.widget.FrameLayout.LayoutParams(-1,-1)); pageViews[page]=workspace;
        return workspace;
    }
    private void plotControls(LinearLayout parent,PlanPreview view) {
        LinearLayout row=new LinearLayout(this); row.setTag("plotTools"); parent.addView(row,0);
        Button reset=button(row,"Pan ±180°",() -> { if(view==preview) applyTiltRange(); else view.wholeRange(); },() -> true); reset.setTag("plotReset"); reset.setContentDescription(UiText.DEFAULT_VIEW_PAN_MINUS_180_TO_PLUS_180.text());
        Button fit=button(row,UiText.FIT.text(),view::fit,() -> true); fit.setTag("plotFit"); fit.setContentDescription(UiText.FIT_TO_PLAN_AND_CAPTURES.text());
        button(row,"−",() -> view.zoom(2),() -> true).setContentDescription(UiText.ZOOM_CHART_OUT.text());
        button(row,"+",() -> view.zoom(.5),() -> true).setContentDescription(UiText.ZOOM_CHART_IN.text());
        Button large=button(row,UiText.LARGE.text(),() -> togglePlotWorkspace(view),() -> true); large.setTag("plotLarge"); large.setContentDescription(UiText.ENLARGE_CHART_OR_RETURN_TO_NORMAL_VIEW.text());
        for(int i=0;i<row.getChildCount();i++) { Button control=(Button)row.getChildAt(i); control.setTextSize(12); control.setPadding(dp(2),0,dp(2),0); control.setMaxLines(2); }
    }
    private void togglePlotWorkspace(PlanPreview view) {
        PlotWorkspace workspace=view==preview?planWorkspace:runWorkspace;
        workspace.setExpanded(!workspace.expanded()); updatePlotSizeLabels();
    }
    private void updatePlotSizeLabels() {
        if(planWorkspace!=null) ((Button)planWorkspace.findViewWithTag("plotLarge")).setText(planWorkspace.expanded()?"Normal":UiText.LARGE.text());
        if(runWorkspace!=null) ((Button)runWorkspace.findViewWithTag("plotLarge")).setText(runWorkspace.expanded()?"Normal":UiText.LARGE.text());
        syncPlotChrome();
    }
    private void syncPlotChrome() {
        PlotWorkspace workspace=selectedPage==2?planWorkspace:selectedPage==3?runWorkspace:null;
        boolean large=workspace!=null && workspace.expanded();
        android.view.ViewGroup destination=large?(android.view.ViewGroup)workspace.findViewWithTag("plotTools"):emergencyHome;
        android.view.ViewGroup current=(android.view.ViewGroup)emergencyButton.getParent();
        if(current!=destination) {
            current.removeView(emergencyButton);
            if(large) destination.addView(emergencyButton,new LinearLayout.LayoutParams(0,dp(48),1));
            else destination.addView(emergencyButton,Math.min(emergencyHomeIndex,destination.getChildCount()),emergencyHomeParams);
        }
        emergencyButton.setTextSize(large?12:14); emergencyButton.setPadding(dp(large?2:8),0,dp(large?2:8),0);
        headerBar.setVisibility(large?View.GONE:View.VISIBLE); tabBar.setVisibility(large?View.GONE:View.VISIBLE); message.setVisibility(large?View.GONE:View.VISIBLE);
    }
    @Override public void onBackPressed() {
        PlotWorkspace workspace=selectedPage==2?planWorkspace:selectedPage==3?runWorkspace:null;
        if(workspace!=null && workspace.expanded()) { workspace.setExpanded(false); updatePlotSizeLabels(); return; }
        super.onBackPressed();
    }
    private void chooseTiltRange() {
        new AlertDialog.Builder(this).setTitle(UiText.CHART_TILT_RANGE.text())
            .setSingleChoiceItems(new String[]{UiText.TEXT_25_TYPICAL_PANORAMAS.text(),UiText.TEXT_45_WIDER_TILT_RANGE.text(),UiText.FULL_RANGE_E_G_SPHERICAL.text()},tiltRange,(dialog,which) -> {
                tiltRange=which; applyTiltRange(); dialog.dismiss();
            }).setNegativeButton(UiText.CANCEL.text(),null).show();
    }
    private void applyTiltRange() {
        preview.alignmentRange(tiltRange==0?25:tiltRange==1?45:0);
        tiltRangeButton.setText(tiltRange==0?"Tilt ±25°":tiltRange==1?"Tilt ±45°":UiText.FULL_TILT.text());
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
        if(p.pending) return String.format(UiText.locale(),UiText.S_PAN_3F_TILT_3F_TILT_CHANGE_3F.text(),preview.editMode()==3?UiText.MOVEMENT_TARGET.text():preview.editMode()==1?UiText.START_PROPOSAL.text():UiText.END_PROPOSAL.text(),p.pan,p.tilt,p.tilt-(service!=null && service.fresh()?service.state.tilt/1000.0:p.baseTilt),p.limited?UiText.DEVICE_LIMIT.text():"");
        if(service!=null && service.fresh()) return String.format(UiText.locale(),UiText.CURRENT_PAN_3F_TILT_3F_S.text(),-service.state.pan/1000.0,service.state.tilt/1000.0,preview.editMode()==0?UiText.PAN_VIEW.text():UiText.SELECT_POINT_ON_CHART.text());
        return UiText.SELECT_POINT_START_S_BLUE_END_E_RED.text();
    }
    private boolean positionReady() { return manualReady() && !service.manualMoving && !service.manualTrigger; }
    private boolean canStartLoadedPlan() { return ready() && !service.manualMoving && !service.manualTrigger && service.state.positionSafe() && service.state.state.equals("PLAN_READY") && service.state.plan!=null; }
    private boolean canStartLocalPlan() { return canStartLoadedPlan() && !preview.selection.pending && pendingPlan!=null && pendingPlan.sameRecipe(service.state.plan); }
    private void startLoadedPlan() {
        if(!canStartLoadedPlan()) throw new IllegalArgumentException(UiText.FIRST_UPLOAD_A_PLAN_AND_WAIT_FOR_CONFIRMATION.text());
        if(selectedPage==2) { calculate(); if(!canStartLocalPlan()) throw new IllegalArgumentException(UiText.PLAN_CHANGED_RESET_DEVICE_JOB_AND_UPLOAD_THE.text()); }
        long run=service.state.runId;
        confirm(UiText.START_LOADED_JOB.text(),service.state.plan.total()+UiText.CAPTURES_DEVICE_JOB.text()+run+UiText.THE_DEVICE_WILL_MOVE_AND_TRIGGER_THE_CAMERA.text(),() -> {
            if(!canStartLoadedPlan() || service.state.runId!=run) throw new IllegalArgumentException(UiText.LOADED_JOB_HAS_CHANGED_CHECK_AGAIN.text());
            service.runCommand("START"); selectPage(3);
        });
    }
    private void abortLoadedPlan() {
        confirm(UiText.STOP_JOB_CHANGE_ENDPOINTS.text(),UiText.THE_DEVICE_WILL_STOP_THE_DEVICE_JOB_IS.text(),() -> { service.abortPlan(); preview.setEditMode(3); selectPage(2); ((ScrollView)pages[2].getParent()).smoothScrollTo(0,0); });
    }
    private void prepareRest() {
        CaptureStore.Checkpoint checkpoint=service.restCheckpoint();RasterRecovery raster=checkpoint.raster;
        long run=raster.source.runId;int index=raster.nextIndex;final int[] choice={0};
        String[] choices={UiText.FROM.text()+raster.unit()+" "+(raster.startColumn(false)+1)+UiText.REPEAT.text()+raster.remaining(newRunId(),false).total()+UiText.CAPTURES.text(),
            UiText.START_EARLIER.text()+raster.unit()+" "+(raster.startColumn(true)+1)+" · "+raster.remaining(newRunId(),true).total()+UiText.CAPTURES.text()};
        TextView detail=text(UiText.EXISTING_CAPTURES_ARE_RETAINED_LOADING_DOES_NOT_START.text()+
            (raster.source.mode==1?UiText.A_COMPLETE_TILT_BAND_IS_REPEATED_TO_AVOID.text():""),13);
        detail.setPadding(dp(24),dp(8),dp(24),0);
        restDialog=new AlertDialog.Builder(this).setTitle(UiText.RECOVER_PANORAMA_2.text())
            .setView(detail)
            .setSingleChoiceItems(choices,0,(dialog,which) -> choice[0]=which)
            .setPositiveButton(UiText.LOAD_RECOVERY_JOB.text(),(dialog,which) -> safely(() -> {
                service.prepareRest(choice[0]==1,run,index);savedPlot=null;savedPlotKey=null;plottedRun=-1;selectPage(3);
            })).setNegativeButton(UiText.CANCEL.text(),null).show();
    }
    private void removePlan() {
        if(service!=null && service.hasConnection()) {
            if(!ready()) throw new IllegalArgumentException(UiText.WAIT_FOR_CURRENT_DEVICE_STATE.text());
            if(!service.state.state.equals("IDLE")) { service.abortPlan(true); return; }
            service.discardDraft(); observedDraftRevision=service.draftRevision;
        }
        clearLocalPlan(); Toast.makeText(this,UiText.PLAN_REMOVED.text(),Toast.LENGTH_SHORT).show();
    }
    private void clearLocalPlan() {
        for(String key:new String[]{"startPan","startTilt","endPan","endTilt"}) inputs.get(key).setText("");
        pendingPlan=null; preview.setPlan(null); preview.setEndpointAvailability(false,false); preview.setEditMode(3);
        planSummary.setText(UiText.NO_PLAN_YET_ALIGN_AND_REMEMBER_START_AND.text()); saveInputs();
    }
    private boolean endpointSet(String prefix) {
        try { number(prefix+"Pan"); number(prefix+"Tilt"); return true; } catch(RuntimeException ex) { return false; }
    }
    private void driveSelection() {
        if(!preview.selection.pending || preview.editMode()!=3) throw new IllegalArgumentException(UiText.SELECT_A_MOVEMENT_TARGET_FIRST.text());
        driveUiPoint(preview.selection.pan,preview.selection.tilt);
    }
    private void driveEndpoint(String prefix) { driveUiPoint(number(prefix+"Pan"),number(prefix+"Tilt")); }
    private void driveUiPoint(double pan,double tilt) {
        if(!positionReady()) throw new IllegalArgumentException(UiText.WAIT_FOR_CURRENT_TARGET_ARRIVAL_FIRST.text());
        service.setManualTempos(integer("manualPanSpeed"),integer("manualSpeed")); service.moveToUi(pan,tilt); preview.discardSelection();
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
        Button button=new Button(this); button.setText(title); button.setMinHeight(dp(48)); button.setMinimumHeight(dp(48)); button.setAllCaps(false); button.setTextSize(14); button.setMinWidth(0); button.setMinimumWidth(0); button.setPadding(dp(8),0,dp(8),0);
        button.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(119,133,144),INK}));
        button.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{Color.rgb(235,239,241),Color.rgb(226,238,241)}));
        button.setOnClickListener(v -> safely(action)); gates.put(button,enabled);
        if (parent.getOrientation()==LinearLayout.HORIZONTAL) parent.addView(button,new LinearLayout.LayoutParams(0,dp(48),1)); else parent.addView(button);
        return button;
    }
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void arrowButton(LinearLayout parent,String symbol,String description,int axis,int direction) {
        Runnable start=() -> safely(() -> { service.setJogSpeed(axis,number(axis==0?"panJogSpeed":"jogSpeed")); service.setRememberSettleMillis(integer("rememberSettleMs")); preview.discardSelection(); preview.followCurrentPosition();
            service.beginJog(axis,direction); });
        Runnable stop=() -> { if(service!=null && service.jogHeldFor(axis,direction)) service.endJog(); };
        // Accessibility click is a bounded hold; touch and keyboard release
        // cancel immediately, including while the start ACK is outstanding.
        Button arrow=button(parent,symbol,() -> {
            start.run(); long token=service==null ? 0 : service.jog.token;
            plotHandler.postDelayed(() -> { if(service!=null && service.jog.token==token) stop.run(); },1000);
        },
            () -> service!=null && (service.jogHeldFor(axis,direction) || service.jogAllowed()));
        arrow.setTextSize(24); arrow.setContentDescription(description+UiText.HOLD_RELEASE_TO_STOP.text());
        arrow.setOnTouchListener((view,event) -> {
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN) { view.setPressed(true); start.run(); }
            else if(action==android.view.MotionEvent.ACTION_UP || action==android.view.MotionEvent.ACTION_CANCEL ||
                    action==android.view.MotionEvent.ACTION_POINTER_DOWN || action==android.view.MotionEvent.ACTION_POINTER_UP ||
                    action==android.view.MotionEvent.ACTION_MOVE &&
                    (event.getX()<0 || event.getY()<0 || event.getX()>=view.getWidth() || event.getY()>=view.getHeight())) {
                view.setPressed(false); stop.run();
            }
            return true;
        });
        arrow.setOnKeyListener((view,key,event) -> {
            if(key!=android.view.KeyEvent.KEYCODE_ENTER && key!=android.view.KeyEvent.KEYCODE_DPAD_CENTER && key!=android.view.KeyEvent.KEYCODE_SPACE) return false;
            if(event.getAction()==android.view.KeyEvent.ACTION_DOWN && event.getRepeatCount()==0) start.run();
            else if(event.getAction()==android.view.KeyEvent.ACTION_UP) stop.run();
            return true;
        });
    }
    private EditText input(LinearLayout parent,String key,String label,String fallback) {
        parent.addView(text(label,14)); EditText view=new EditText(this); view.setSingleLine(true);
        view.setInputType(key.equals("profileName") ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        boolean endpoint=key.startsWith("start") || key.startsWith("end");
        view.setText(endpoint && !restoreEndpoints?"":preferences.getString(key,fallback)); inputs.put(key,view); parent.addView(view);
        view.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { preferences.edit().putString(key,s.toString()).apply(); }
            public void afterTextChanged(Editable value) { }
        });
        return view;
    }
    private boolean timingReady() {
        return ready() && service.state.plan!=null && Arrays.asList("PLAN_READY","RUNNING","PAUSE_PENDING","PAUSED").contains(service.state.state);
    }
    private AlertDialog editRunTiming() {
        if(!timingReady()) throw new IllegalArgumentException(UiText.CURRENT_DEVICE_PLAN_MISSING_OR_ACTION_NOT_YET.text());
        long run=service.state.runId; PanoramaPlan p=service.state.plan;
        int[] values=service.pendingTiming!=null && service.pendingTimingRun==run ? service.pendingTiming : new int[]{p.settleMs,p.pulseMs,p.exposureMs};
        LinearLayout body=vertical();body.setPadding(dp(20),dp(8),dp(20),dp(8));
        body.addView(text(service.timingSummary()+UiText.CHANGES_APPLY_AFTER_THE_CURRENT_IMAGE_OR_BEFORE.text(),14));
        String[] labels={UiText.SETTLING_DURATION_MS.text(),UiText.TRIGGER_DURATION_MS.text(),UiText.EXPOSURE_WAIT_MS.text()};
        EditText[] fields=new EditText[3];
        for(int i=0;i<3;i++) { body.addView(text(labels[i],14));fields[i]=new EditText(this);fields[i].setSingleLine(true);
            fields[i].setInputType(InputType.TYPE_CLASS_NUMBER);fields[i].setTag("runTiming"+i);fields[i].setText(""+values[i]);body.addView(fields[i]); }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(UiText.CHANGE_RUN_TIMING.text()).setView(body)
            .setNegativeButton(UiText.CANCEL.text(),null).setPositiveButton(UiText.APPLY.text(),null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> safely(() -> {
            if(service==null || service.state.runId!=run) throw new IllegalArgumentException(UiText.DEVICE_RUN_HAS_CHANGED.text());
            int settle=Integer.parseInt(fields[0].getText().toString()),pulse=Integer.parseInt(fields[1].getText().toString()),exposure=Integer.parseInt(fields[2].getText().toString());
            service.updateTiming(settle,pulse,exposure);
            inputs.get("settle").setText(""+settle);inputs.get("planPulse").setText(""+pulse);inputs.get("exposure").setText(""+exposure);
            dialog.dismiss();
        })));
        dialog.show();return dialog;
    }
    private boolean ready() { return service!=null && service.fresh() && !service.uncertain && !service.recovering && !service.incident.needsAcknowledgement() && !service.busy && !service.abortingPlan && !service.preparingRest && !service.jog.held && !service.jog.stopping; }
    private boolean manualReady() { return service!=null && service.navigationAllowed() && selectedPage!=3; }
    private void selectPage(int index) {
        selectedPage=Math.max(0,Math.min(4,index));
        navigationBar.setVisibility(selectedPage==1 || selectedPage==2?View.VISIBLE:View.GONE);
        endpointBar.setVisibility(selectedPage==2?View.VISIBLE:View.GONE);
        runActionBar.setVisibility(selectedPage==3?View.VISIBLE:View.GONE);
        for (int i=0;i<pages.length;i++) pageViews[i].setVisibility(i==selectedPage ? View.VISIBLE : View.GONE);
        for(int i=0;i<tabs.size();i++) { Button tab=tabs.get(i); tab.setSelected(i==selectedPage); tab.setTextColor(i==selectedPage?Color.WHITE:MUTED); tab.setBackgroundTintList(ColorStateList.valueOf(i==selectedPage?ACCENT:Color.rgb(226,234,239))); }
        if (selectedPage==4 && service!=null) refreshRuns();
        syncPlotChrome(); render();
    }
    private String statusText() {
        if (service==null) return UiText.NO_DEVICE_CONNECTION.text();
        DeviceState s=service.state;
        String result=String.format(UiText.locale(),UiText.FIRMWARE_S_S_STATUS_S_S_POSITION_PAN.text(),s.firmware,s.hardware,s.state,s.phase,-s.pan/1000.0,s.tilt/1000.0,s.completed,s.total,s.column,s.row,s.runId,s.revision);
        if(s.currentMoveMaximumMs()>0) result+=String.format(UiText.locale(),UiText.MOVEMENT_TIME_LIMIT_FOR_THIS_STEP_2F_S.text(),s.currentMoveMaximumMs()/1000.0);
        if (!service.fresh()) result+=UiText.STATUS_DATA_IS_MISSING_OR_STALE.text();
        if (s.limitsKnown) result+=String.format(UiText.locale(),UiText.LIMITS_S_PAN_3F_TO_3F_TILT_3F.text(),s.confirmed ? UiText.CONFIRMED.text() : UiText.NOT_CONFIRMED.text(),-s.panMax/1000.0,-s.panMin/1000.0,s.tiltMin/1000.0,s.tiltMax/1000.0);
        if (s.limitsKnown && s.confirmed && !s.positionSafe()) result+=UiText.ACTUAL_POSITION_OUTSIDE_LIMIT_MOVE_THE_AFFECTED_AXIS.text();
        if (service.manualMoving) result+=UiText.MANUAL_MOVEMENT_ACTIVE.text();
        if (service.manualTrigger) result+=UiText.CAMERA_TRIGGER_ACTIVE_NOT_YET_COMPLETED.text();
        if (s.plan!=null) result+=UiText.DEVICE_PLAN.text()+s.plan.total()+UiText.IMAGES.text()+service.timingSummary()+"\n";
        if(s.warningSettingsSupported) result+=String.format(UiText.locale(),UiText.ACTIVE_WARNING_THRESHOLDS_PAN_3F_TILT_3F_DEVICE.text(),s.warningPanMdeg/1000.0,s.warningTiltMdeg/1000.0,s.motionConfigRevision);
        return result;
    }
    private void render() {
        if (connection==null) return;
        if(recoveryAction!=null) recoveryAction.setVisibility(service!=null && (service.incident.needsAcknowledgement() || service.uncertain || service.recovering || service.state.state.equals("FAULT") || (service.hasConnection() && !service.fresh()))?View.VISIBLE:View.GONE);
        if(referenceReview!=null) referenceReview.setVisibility(service!=null && service.draftNeedsReview?View.VISIBLE:View.GONE);
        boolean recoveryVisible=savedPlot==null && service!=null && service.hasRecoveryCheckpoint();
        if(restRecoveryAction!=null) restRecoveryAction.setVisibility(recoveryVisible?View.VISIBLE:View.GONE);
        if(recoveryHint!=null) {recoveryHint.setVisibility(recoveryVisible?View.VISIBLE:View.GONE);if(recoveryVisible) recoveryHint.setText(service.recoveryGuidance());}
        if(recoveryCheck!=null) recoveryCheck.setVisibility(recoveryVisible && !service.canPrepareRest()?View.VISIBLE:View.GONE);
        if(recoveryOfferRun!=0 && service!=null && service.state.runId==recoveryOfferRun && service.canPrepareRest()) {
            recoveryOfferRun=0;
            recoveryHint.post(() -> {if(!isFinishing() && service!=null && service.canPrepareRest() && (restDialog==null || !restDialog.isShowing())) prepareRest();});
        }
        if(columnRetryAction!=null) columnRetryAction.setVisibility(service!=null && service.state.recoveryPauseSupported && service.state.state.equals("PAUSED") && service.state.plan!=null && service.state.plan.mode==0?View.VISIBLE:View.GONE);
        if(captureStabilityActive!=null) {
            CaptureStabilitySettings s=service==null?null:service.state.captureStability;
            captureStabilityActive.setText(s==null?UiText.NO_CONFIRMED_DEVICE_VALUES_YET_FIRMWARE_2_4.text():
                String.format(UiText.locale(),UiText.ACTIVE_ON_DEVICE_VARIATION_3F_PAN_3F_TILT.text(),s.spanMdeg/1000.0,s.panDriftMdeg/1000.0,s.tiltDriftMdeg/1000.0,s.extraWaitMs/1000.0,s.revision));
            String values=s==null?"":s.revision+"|"+s.spanMdeg+"|"+s.panDriftMdeg+"|"+s.tiltDriftMdeg+"|"+s.extraWaitMs;
            if(s!=null && !values.equals(displayedCaptureStability)) {
                displayedCaptureStability=values;
                inputs.get("captureSpan").setText(""+(s.spanMdeg/1000.0));inputs.get("capturePanDrift").setText(""+(s.panDriftMdeg/1000.0));
                inputs.get("captureTiltDrift").setText(""+(s.tiltDriftMdeg/1000.0));inputs.get("captureExtraWait").setText(""+(s.extraWaitMs/1000.0));
            }
        }
        EditText manualTempo=inputs.get("manualSpeed");
        if(manualTempo!=null) manualTempo.setEnabled(service==null || !service.connected || (service.state.profiledMotion && !service.jog.held && !service.jog.stopping));
        EditText panManualTempo=inputs.get("manualPanSpeed");
        if(panManualTempo!=null) panManualTempo.setEnabled(manualTempo==null || manualTempo.isEnabled());
        EditText jogTempo=inputs.get("jogSpeed");
        if(jogTempo!=null) jogTempo.setEnabled(service==null || !service.connected || (service.state.jogMaximumVelocityMdeg>0 && !service.jog.held && !service.jog.stopping));
        EditText rememberTempo=inputs.get("rememberSettleMs");
        if(rememberTempo!=null) rememberTempo.setEnabled(service==null || (!service.jog.held && !service.jog.stopping));
        EditText panJogTempo=inputs.get("panJogSpeed");
        if(panJogTempo!=null) panJogTempo.setEnabled(service==null || !service.connected || (service.state.jogMaximumVelocityMdeg>0 && !service.jog.held && !service.jog.stopping));
        if(jogTempoLabel!=null && jogTempo!=null) {
            String tempo=service!=null && service.connected && service.state.jogMaximumVelocityMdeg==0 ? manualTempo.getText()+"‰" : "P"+panJogTempo.getText()+" · T"+jogTempo.getText()+"°/s";
            String label=UiText.HOLD.text()+tempo;
            if(!jogTempoLabel.getText().toString().equals(label)) jogTempoLabel.setText(label);
        }
        if(service!=null && observedDraftRevision>=0 && observedDraftRevision!=service.draftRevision) {
            observedDraftRevision=service.draftRevision; clearLocalPlan();
        }
        connection.setText(service==null ? UiText.NOT_CONNECTED.text() : service.connectionText());
        int keepOn=android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        boolean keepScreen=service!=null && service.connected;
        if(((getWindow().getAttributes().flags&keepOn)!=0)!=keepScreen) getWindow().setFlags(keepScreen?keepOn:0,keepOn);
        message.setText(service==null ? UiText.READY.text() : service.messageText());
        boolean loadedForAlignment=service!=null && service.fresh() && service.state.state.equals("PLAN_READY");
        if(alignmentReset!=null) alignmentReset.setVisibility(service!=null && service.fresh() && PlanAbort.allowed(service.state.state)?View.VISIBLE:View.GONE);
        if(planDeviceStatus!=null) planDeviceStatus.setText(service==null || !service.fresh()?UiText.CONNECT_TO_THE_DEVICE_TO_UPLOAD.text():loadedForAlignment?(canStartLocalPlan()?UiText.PLAN_CONFIRMED_START_CAPTURE_NOW.text():UiText.PLAN_CHANGED_ON_PHONE_OR_SELECTION_PENDING_RESET.text()):service.state.state.equals("IDLE")?UiText.UPLOAD_FIRST_WAIT_FOR_CONFIRMATION_THEN_START_CAPTURE.text():UiText.DEVICE_STATE.text()+service.state.state+UiText.RESET_JOB_BEFORE_UPLOADING_A_NEW_PLAN.text());
        if(alignmentHint!=null) alignmentHint.setText(alignmentHelp());
        if(activeProfile!=null && inputs.containsKey("profileName")) {
            String name=inputs.get("profileName").getText().toString();
            CameraProfile selected=(CameraProfile)profilePicker.getSelectedItem();
            activeProfile.setText(UiText.ACTIVE.text()+name+(selected!=null && !selected.name.equals(name)?UiText.APPLY_THE_SELECTION_ABOVE_USING_USE_CAMERA_PROFILE.text():""));
        }
        TextView manualHint=pages[1].findViewWithTag("alignmentHint"); if(manualHint!=null) manualHint.setText(alignmentHelp());
        if(endpointSummary!=null) endpointSummary.setText(endpointLabel("start","Start")+"\n"+endpointLabel("end",UiText.END.text()));
        if(runProgress!=null) {
            long completed=savedPlot!=null?savedPlot.model.captures.size():service==null?0:service.state.completed;
            long total=savedPlot!=null && savedPlot.plan!=null?savedPlot.plan.total():service==null?0:service.state.total;
            String state=savedPlot!=null?UiText.SAVED.text():service==null || !service.fresh()?UiText.STATUS_PENDING.text():friendlyState(service.state.state);
            runProgress.setText(state+" · "+completed+" / "+total+UiText.CAPTURES.text()); captureProgress.setMax((int)Math.max(1,total)); captureProgress.setProgress((int)completed);
        }
        live.setText(statusText());
        TextView runLive=pages[3].findViewWithTag("live"); if (runLive!=null) runLive.setText(statusText());
        diagnostics.setText(service==null ? UiText.NO_DIAGNOSTICS_YET.text() : service.diagnosticsText());
        papywizardStatus.setText(service==null?UiText.PREPARING_PAPYWIZARD_BACKUP.text():service.papywizardStatus());
        if(tiltReference!=null) {
            DeviceState s=service==null?null:service.state;
            tiltReference.setText(s==null || !service.fresh()?UiText.CONNECT_TO_THE_DEVICE_TO_CONFIGURE.text():!s.tiltCalibrationKnown?UiText.TILT_ZERO_POINT_REQUIRES_FIRMWARE_2_3_5.text():String.format(UiText.locale(),UiText.CURRENT_TILT_3F_ZERO_POINT_S.text(),s.tilt/1000.0,s.tiltZeroOffset==0?UiText.FACTORY_REFERENCE.text():UiText.SAVED_ON_DEVICE.text()));
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
            runTitle.setText(savedPlot!=null?UiText.SAVED_RUN.text()+savedPlot.model.runId:UiText.CURRENT_JOB.text());
            runPreview.setPlan(savedPlot!=null?savedPlot.plan:service==null?null:service.state.plan);
            runPreview.setLive(savedPlot!=null?null:service==null?null:service.state,savedPlot!=null?savedPlot.model:service==null?null:service.plot,savedPlot==null && service!=null && service.fresh());
            runLegend.setText((savedPlot!=null?UiText.SAVED_RUN.text()+savedPlot.model.runId+UiText.NO_LIVE_POSITION.text():UiText.CURRENT_DEVICE_RUN.text())+UiText.BLUE_TARGET_GREEN_CAPTURE_ORANGE_ACTUAL_POSITION_RED.text());
            int warnings=warningCaptures().size();warningPoints.setText("⚠ "+warnings+UiText.WARNING_POINTS_SHOW_REVISIT.text());warningPoints.setVisibility(warnings==0?View.GONE:View.VISIBLE);
            // New status/plan data never silently replace the chosen zoom.
            if(savedPlot==null && service!=null && service.fresh()) plottedRun=service.state.runId;
        }
        devicePicker.setEnabled(service==null || !service.hasConnection());
        for (Map.Entry<Button,BooleanSupplier> entry:gates.entrySet()) entry.getKey().setEnabled(entry.getValue().getAsBoolean());
        showIncident();
    }
    private List<RunPlotModel.Capture> warningCaptures() {
        List<RunPlotModel.Capture> result=new ArrayList<>();
        RunPlotModel model=savedPlot!=null?savedPlot.model:service==null?null:service.plot;
        if(model!=null) for(RunPlotModel.Capture capture:model.captures) if(capture.warningFlags!=0) result.add(capture);
        return result;
    }
    private void showCaptureWarnings() {
        List<RunPlotModel.Capture> warnings=warningCaptures();String[] labels=new String[warnings.size()];
        for(int i=0;i<labels.length;i++) labels[i]=UiText.CAPTURE.text()+warnings.get(i).ordinal+" · "+CaptureWarning.describe(warnings.get(i).warningFlags);
        new AlertDialog.Builder(this).setTitle(UiText.WARNING_POINTS_RUN_CONTINUES.text()).setItems(labels,(dialog,index) -> reviewCaptureWarning(warnings.get(index))).setNegativeButton(UiText.CLOSE.text(),null).show();
    }
    private AlertDialog reviewCaptureWarning(RunPlotModel.Capture capture) {
        String detail=CaptureWarning.describe(capture.warningFlags)+String.format(UiText.locale(),UiText.TARGET_PAN_3F_TILT_3F.text(),-capture.targetPan/1000.0,capture.targetTilt/1000.0);
        detail+=capture.reconstructed?UiText.ACTUAL_POSITION_NOT_TRANSMITTED.text():String.format(UiText.locale(),UiText.ACTUAL_PAN_3F_TILT_3F_DEVIATION_PAN_3F.text(),-capture.pan/1000.0,capture.tilt/1000.0,(capture.targetPan-capture.pan)/1000.0,(capture.tilt-capture.targetTilt)/1000.0);
        boolean drive=service!=null && service.navigationAllowed() && (savedPlot==null || service.connectedTo(savedPlot.device));
        if(drive) try {service.state.validateManualTarget(capture.targetPan,capture.targetTilt);} catch(IllegalArgumentException ex) {drive=false;}
        detail+=drive?UiText.REVISITING_DOES_NOT_TRIGGER_A_CAPTURE_TRIGGER_MANUALLY.text():UiText.TO_REVISIT_PAUSE_OR_END_THE_RUN_AND.text();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(UiText.WARNING_POINT_CAPTURE.text()+capture.ordinal).setMessage(detail).setNegativeButton(UiText.CLOSE.text(),null)
            .setPositiveButton(UiText.MOVE_TO_TARGET.text(),(d,which) -> safely(() -> service.moveToUi(-capture.targetPan/1000.0,capture.targetTilt/1000.0))).create();
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(drive);
        return dialog;
    }
    private String endpointLabel(String prefix,String title) {
        return endpointSet(prefix)?String.format(UiText.locale(),"%s  Pan %.3f° · Tilt %.3f°",title,number(prefix+"Pan"),number(prefix+"Tilt")):title+UiText.NOT_YET_SET.text();
    }
    private String alignmentHelp() {
        if(service==null) return UiText.CONNECT_TO_THE_DEVICE_TO_ALIGN.text();
        String reason=service.navigationReason(); if(!reason.isEmpty()) return reason;
        if(!service.state.positionSafe()) return UiText.ACTUAL_POSITION_OUTSIDE_LIMIT_ONLY_MOVE_BACK_TOWARDS.text();
        if(service.manualMoving) return UiText.CAMERA_MOVING_WAIT_FOR_ARRIVAL_THEN_REMEMBER_START.text();
        if(!service.endpointCaptureAllowed()) return UiText.POSITION_SETTLING_WAIT_FOR_A_FRESH_ACTUAL_POSITION.text();
        return UiText.HOLD_ARROWS_MOVE_THE_ORANGE_CURRENT_POSITION_RELEASE.text();
    }
    private void showColumnRetry() {
        if(service==null || !service.canRetryColumn()) return;
        long run=service.state.runId,index=service.state.nextImageIndex;
        int column=(int)(index/service.state.plan.rows)+1;
        int[] choice={0};
        restDialog=new AlertDialog.Builder(this).setTitle(UiText.PAUSED_GRID_COLUMN.text()+column)
            .setSingleChoiceItems(new String[]{UiText.REPEAT_AFFECTED_COLUMN.text(),UiText.START_ONE_COLUMN_EARLIER.text()},0,(d,which) -> choice[0]=which)
            .setNegativeButton(UiText.CANCEL.text(),null)
            .setPositiveButton(UiText.PREPARE_REPEAT.text(),(d,which) -> safely(() -> service.retryColumn(choice[0]==1,run,index))).create();
        restDialog.show();
    }
    private void showIncident() {
        if(service==null || !service.incident.needsAcknowledgement() || isFinishing()) return;
        long id=service.incident.id;
        if(incidentDialog!=null && incidentDialog.isShowing() && dialogIncidentId==id) {
            incidentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(service.connected); return;
        }
        if(incidentDialog!=null) incidentDialog.dismiss();
        dialogIncidentId=id;
        PanoramaService owner=service;
        boolean paused=RunRecoveryPause.active(owner.state);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(paused || owner.hasRecoveryCheckpoint()?UiText.CAPTURE_INTERRUPTED_JOB_RETAINED.text():UiText.ACTION_INTERRUPTED.text())
            .setMessage(owner.incidentDetail()+(paused?UiText.ACKNOWLEDGEMENT_RETAINS_THE_PAUSED_JOB_AND_DOES_NOT.text():UiText.ACKNOWLEDGEMENT_CHECKS_THE_DEVICE_STATE_THE_PANORAMA_CAN.text()))
            .setCancelable(false).setPositiveButton(UiText.ACKNOWLEDGE_CHECK.text(),(dialog,which) -> safely(() -> {
                if(owner.hasRecoveryCheckpoint()) {recoveryOfferRun=owner.state.runId;savedPlot=null;savedPlotKey=null;selectPage(3);}
                owner.acknowledgeIncident(id);
            }));
        builder.setNeutralButton(UiText.E_STOP.text(),(dialog,which) -> safely(owner::emergencyStop));
        incidentDialog=builder.create(); incidentDialog.setCanceledOnTouchOutside(false);
        // Not-Stop remains reachable without dismissing the required acknowledgement.
        incidentDialog.setOnShowListener(dialog -> {
            incidentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(owner.connected);
            incidentDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> safely(owner::emergencyStop));
        });
        incidentDialog.show();
    }
    private String friendlyState(String state) {
        switch(state) { case "IDLE": return UiText.READY.text(); case "PLAN_READY": return UiText.PLAN_LOADED.text(); case "RUNNING": return UiText.CAPTURING.text(); case "PAUSE_PENDING": return UiText.PREPARING_PAUSE.text(); case "PAUSED": return UiText.PAUSED.text(); case "STOPPED": return UiText.STOPPED.text(); case "COMPLETED": return UiText.COMPLETED.text(); case "FAULT": return UiText.DEVICE_ERROR.text(); default: return state; }
    }
    private void safely(Runnable action) {
        try { action.run(); } catch (RuntimeException ex) { Toast.makeText(this,ex.getMessage()==null ? UiText.ACTION_UNAVAILABLE.text() : ex.getMessage(),Toast.LENGTH_LONG).show(); }
        render();
    }
    private void confirm(String title,String detail,Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setNegativeButton(UiText.CANCEL.text(),null).setPositiveButton(UiText.YES.text(),(d,w) -> safely(action)).show();
    }
    private double number(String key) { double n=Double.parseDouble(inputs.get(key).getText().toString().trim().replace(',','.')); if (!Double.isFinite(n)) throw new IllegalArgumentException(UiText.INVALID_NUMBER.text()); return n; }
    private int integer(String key) { double n=number(key); if (n!=Math.rint(n) || n<Integer.MIN_VALUE || n>Integer.MAX_VALUE) throw new IllegalArgumentException(UiText.WHOLE_MILLISECONDS_NUMBERS_REQUIRED.text()); return (int)n; }
    private void saveInputs() {
        SharedPreferences.Editor editor=preferences.edit(); for (Map.Entry<String,EditText> input:inputs.entrySet()) editor.putString(input.getKey(),input.getValue().getText().toString());
        editor.putInt("mode",modePicker.getSelectedItemPosition()).putBoolean("stopOnLoss",stopOnLoss.isChecked()).apply();
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
        catch (SecurityException ex) { message.setText(UiText.BLUETOOTH_PERMISSION_MISSING.text()); return; }
        devices.sort(Comparator.comparing(this::deviceName));
        List<String> names=new ArrayList<>(); int selected=0; String saved=preferences.getString("device","");
        for (int i=0;i<devices.size();i++) { BluetoothDevice d=devices.get(i); String name=deviceName(d); names.add(name+" ("+d.getAddress()+")"); if (d.getAddress().equals(saved) || (saved.isEmpty() && name.startsWith("RNBT"))) selected=i; }
        devicePicker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names)); devicePicker.setSelection(selected); render();
    }
    private String deviceName(BluetoothDevice device) {
        try { String name=device.getName(); return name==null ? UiText.UNNAMED.text() : name; }
        catch (SecurityException ex) { return UiText.BLUETOOTH_PERMISSION_MISSING.text(); }
    }
    private void connect() {
        if (!permission() || devicePicker.getSelectedItemPosition()<0 || service==null || service.hasConnection()) return;
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);
        Intent intent=new Intent(this,PanoramaService.class).putExtra("address",devices.get(devicePicker.getSelectedItemPosition()).getAddress());
        startForegroundService(intent);
    }
    private void takePosition(String prefix) { if (!positionReady() || !service.state.positionSafe() || !service.endpointCaptureAllowed()) throw new IllegalArgumentException(UiText.REMEMBER_POSITION_ONLY_AFTER_SETTLING_AND_RECEIVING_FRESH.text()); preview.discardSelection(); inputs.get(prefix+"Pan").setText(""+(-service.state.pan/1000.0)); inputs.get(prefix+"Tilt").setText(""+(service.state.tilt/1000.0)); Toast.makeText(this,prefix.equals("start")?UiText.START_POSITION_SAVED.text():UiText.END_POSITION_SAVED.text(),Toast.LENGTH_SHORT).show(); }
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
        planSummary.setText(String.format(UiText.locale(),UiText.FIELD_OF_VIEW_3F_3F_D_CAPTURES_D.text(),profile.fovX(),profile.fovY(),pendingPlan.total(),pendingPlan.columns,pendingPlan.rows,seconds/60)); saveInputs();
        render();
    }
    private void loadPlan() {
        if(preview.selection.pending) throw new IllegalArgumentException(UiText.APPLY_OR_DISCARD_THE_CHART_SELECTION_FIRST.text());
        calculate(); PanoramaPlan plan=pendingPlan;
        confirm(UiText.UPLOAD_PLAN_2.text(),plan.total()+UiText.CAPTURES_WITH.text()+editedProfile().name+UiText.AFTER_CONFIRMATION_PRESS_START_CAPTURE_UPLOADING_ALONE_DOES.text(),() -> service.loadPlan(plan));
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
        saveInputs(); Toast.makeText(this,UiText.PROFILE_ACTIVE.text()+p.name,Toast.LENGTH_SHORT).show();
    }
    private void saveProfile() {
        CameraProfile p=editedProfile(); int index=-1; for (int i=0;i<profiles.size();i++) if (profiles.get(i).name.equals(p.name)) index=i;
        if (index>=0) profiles.set(index,p); else profiles.add(p); persistProfiles(); updateProfilePicker();
    }
    private void persistProfiles() {
        try { JSONArray data=new JSONArray(); for (CameraProfile p:profiles) data.put(new JSONObject().put("name",p.name).put("focal",p.focal).put("width",p.width).put("height",p.height).put("settle",p.settleMs).put("exposure",p.exposureMs)); preferences.edit().putString("profiles",data.toString()).apply(); }
        catch (JSONException ex) { throw new IllegalArgumentException(UiText.COULD_NOT_SAVE_PROFILES.text(),ex); }
    }
    private void importConfig() { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),61); }
    private void parseConfig(InputStream input) throws Exception {
        org.xmlpull.v1.XmlPullParser parser=android.util.Xml.newPullParser(); parser.setInput(input,"UTF-8"); Map<String,String> values=new HashMap<>();
        int event; while ((event=parser.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) if (event==org.xmlpull.v1.XmlPullParser.START_TAG) {
            String tag=parser.getName(); if (tag.matches("(FocalLength|ChipWidth|ChipHeight|VibrationDelay|ExposureDelay)_[1-9]")) values.put(tag,parser.nextText().trim());
        }
        if (!values.containsKey("FocalLength_4")) throw new IllegalArgumentException(UiText.NO_WINDOWS_CAMERA_PROFILES_FOUND.text());
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
        CaptureStore.Run run=(CaptureStore.Run)runPicker.getSelectedItem(); if (run==null) throw new IllegalArgumentException(UiText.NO_CAPTURES_SAVED_LOAD_RUNS.text());
        if(service.captures.hasUncertainExposure(run.key)) {
            confirm(UiText.CHECK_CAMERA_FILES.text(), UiText.AN_EXPOSURE_ERROR_MAY_HAVE_CREATED_EXTRA_PHOTOS.text(),() -> saveCaptureExport(run,xml));
        } else saveCaptureExport(run,xml);
    }
    private void saveCaptureExport(CaptureStore.Run run,boolean xml) {
        pendingExport=service.captures.exportLinked(run.key,xml);
        prepareExport();
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(xml?"application/xml":"text/csv").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Panorama-"+run.key+(xml?".xml":".csv")),60);
    }
    private void exportInterruptions() {
        CaptureStore.Run run=(CaptureStore.Run)runPicker.getSelectedItem();if(run==null) throw new IllegalArgumentException(UiText.LOAD_SAVED_RUNS_FIRST.text());
        pendingExport=service.captures.exportInterruptions(run.key);prepareExport();
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Panorama-Unterbrechungen-"+run.key+".csv"),60);
    }
    private void exportSupport() {
        try { pendingExport="PanoramaControl "+BuildConfig.VERSION_NAME+"\n"+statusText()+"\n"+service.diagnosticsText()+"\n"+service.support.snapshot(); }
        catch (IOException ex) { throw new IllegalArgumentException(ex.getMessage(),ex); }
        prepareExport();
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"Panorama-Diagnose.txt"),60);
    }
    private void prepareExport() {
        try {
            File file=File.createTempFile("export-",".snapshot",getCacheDir());
            try (OutputStream out=new FileOutputStream(file)) { out.write(pendingExport.getBytes(StandardCharsets.UTF_8)); }
            pendingExportName=file.getName(); pendingExport=null;
        } catch (IOException ex) { throw new IllegalArgumentException(UiText.CANNOT_SAVE_EXPORT_SNAPSHOT.text(),ex); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data); if (result!=RESULT_OK || data==null || data.getData()==null) return;
        try {
            if (request==60 && pendingExportName!=null) {
                File snapshot=new File(getCacheDir(),pendingExportName);
                try (OutputStream output=getContentResolver().openOutputStream(data.getData(),"wt")) {
                    if (output==null) throw new IOException(UiText.FILE_IS_NOT_WRITABLE.text());
                    try (InputStream source=new FileInputStream(snapshot)) {
                        byte[] buffer=new byte[8192]; int count;
                        while ((count=source.read(buffer))!=-1) output.write(buffer,0,count);
                    }
                }
                if (!snapshot.delete()) snapshot.deleteOnExit();
                pendingExportName=null; Toast.makeText(this,UiText.FILE_SAVED.text(),Toast.LENGTH_SHORT).show();
            } else if (request==61) try (InputStream input=getContentResolver().openInputStream(data.getData())) { if (input==null) throw new IOException(UiText.FILE_IS_NOT_READABLE.text()); parseConfig(input); }
        } catch (Exception ex) { Toast.makeText(this,UiText.FILE_ERROR.text()+ex.getMessage(),Toast.LENGTH_LONG).show(); }
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
        if(!tiltCalibrationReady()) throw new IllegalArgumentException(UiText.SET_TILT_ZERO_POINT_ONLY_WHILE_STATIONARY_WITH.text());
        confirm(reset?UiText.RESTORE_FACTORY_TILT_REFERENCE.text():UiText.SET_CURRENT_TILT_POSITION_AS_0.text(),
            (reset?UiText.THE_SAVED_TILT_ZERO_POINT_WILL_BE_RESET.text():UiText.IS_THE_CAMERA_LEVEL_THE_CURRENT_POSITION_WILL.text())+UiText.THE_DEVICE_WILL_NOT_MOVE_THE_LIMITS_REMAIN.text(),() -> service.setTiltZero(reset));
    }
    private void setTiltLimits() {
        int low=DeviceState.toTiltDevice(number("tiltMin")), high=DeviceState.toTiltDevice(number("tiltMax"));
        if (low>=high || service.state.tilt<low || service.state.tilt>high) throw new IllegalArgumentException(UiText.LIMITS_MUST_INCLUDE_THE_CURRENT_TILT_POSITION.text());
        if (!(service.state.state.equals("IDLE")||service.state.state.equals("STOPPED")||service.state.state.equals("COMPLETED")) || service.manualMoving || service.manualTrigger)
            throw new IllegalArgumentException(UiText.CHANGE_LIMITS_ONLY_WHEN_SAFELY_STATIONARY.text());
        if(service.state.tiltCalibrationKnown && (low<service.state.tiltAbsoluteMin || high>service.state.tiltAbsoluteMax))
            throw new IllegalArgumentException(UiText.LIMITS_OUTSIDE_THE_MEASURED_DEVICE_RANGE.text());
        confirm(UiText.APPLY_MEASURED_LIMITS.text(),UiText.THESE_VALUES_MUST_HAVE_BEEN_CHECKED_MECHANICALLY_WITH.text(),() -> service.command("SET_TILT_LIMITS",""+low,""+high));
    }
}
