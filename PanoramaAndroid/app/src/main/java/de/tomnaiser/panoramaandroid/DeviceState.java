package de.tomnaiser.panoramaandroid;

/** Validated firmware state in device millidegrees. Never inferred from button clicks. */
public final class DeviceState {
    boolean jogSupported;
    int jogMaximumVelocityMdeg;
    int jogMaximumPanVelocityMdeg;
    int runMaximumTempo=1000, warningPanMdeg, warningTiltMdeg;
    long motionConfigRevision;
    boolean warningSettingsSupported;
    CaptureStabilitySettings captureStability;
    boolean recoveryPauseSupported;
    public String firmware = "—", hardware = "—", state = "UNKNOWN", phase = "NONE", lastError = "—";
    public long runId, completed, total, lastStatusMs;
    long nextImageIndex, timingRunId, timingImageIndex, moveMaximumMs;
    long currentMoveMaximumMs() {
        return timingRunId==runId && timingImageIndex==nextImageIndex &&
            (phase.equals("MOVING") || phase.equals("SETTLING")) &&
            (state.equals("RUNNING") || state.equals("PAUSE_PENDING")) ? moveMaximumMs : 0;
    }
    public int pan, tilt, column, row, revision;
    public int panMin, panMax, tiltMin, tiltMax;
    public boolean limitsKnown, confirmed, compatible;
    public boolean tiltCalibrationKnown;
    public boolean inactiveNavigation;
    public boolean profiledMotion;
    public boolean manualStatusSupported;
    ManualActionStatus manualStatus;
    long manualStatusVersion, lastManualStatusMs, manualStatusExpectedMs;
    public int tiltZeroOffset, tiltAbsoluteMin, tiltAbsoluteMax;
    public PanoramaPlan plan;

    public void accept(P2Codec.Frame frame, long now) {
        String[] f = frame.fields;
        switch (frame.type) {
            case "DEVICE_INFO":
                if (f.length != 4) throw new IllegalArgumentException(UiText.INCOMPLETE_DEVICE_INFORMATION.text());
                firmware = f[0]; hardware = f[3]; compatible = f[2].equals("2"); break;
            case "CAPABILITIES":
                if (f.length != 5) throw new IllegalArgumentException(UiText.INCOMPLETE_LIMITS.text());
                int p0 = Integer.parseInt(f[0]), p1 = Integer.parseInt(f[1]);
                int t0 = Integer.parseInt(f[2]), t1 = Integer.parseInt(f[3]);
                if (p0 >= p1 || t0 >= t1 || !(f[4].equals("0") || f[4].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_LIMITS.text());
                panMin = p0; panMax = p1; tiltMin = t0; tiltMax = t1;
                confirmed = f[4].equals("1"); limitsKnown = true; break;
            case "TILT_CALIBRATION":
                if(f.length!=3) throw new IllegalArgumentException(UiText.INCOMPLETE_TILT_REFERENCE.text());
                int zero=Integer.parseInt(f[0]), absoluteMinimum=Integer.parseInt(f[1]), absoluteMaximum=Integer.parseInt(f[2]);
                if(absoluteMinimum>=absoluteMaximum) throw new IllegalArgumentException(UiText.INVALID_TILT_REFERENCE.text());
                tiltZeroOffset=zero; tiltAbsoluteMin=absoluteMinimum; tiltAbsoluteMax=absoluteMaximum; tiltCalibrationKnown=true; break;
            case "NAVIGATION_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_NAVIGATION_CAPABILITY.text());
                inactiveNavigation=f[0].equals("1"); break;
            case "PLAN":
                // Older firmware publishes an empty recipe after ESTOP without a run.
                if(f.length==16 && f[0].equals("0") && f[1].equals("1") && f[2].equals("0")) {
                    for(int i=3;i<f.length;i++) Integer.parseInt(f[i]);
                    plan=null;
                } else plan=PanoramaPlan.parse(f);
                break;
            case "MOTION_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_MOTION_PROFILE_CAPABILITY.text());
                profiledMotion=f[0].equals("1"); break;
            case "STATUS":
                if (f.length < 18) throw new IllegalArgumentException(UiText.INCOMPLETE_STATUS.text());
                long id = unsigned(f[0]), count = unsigned(f[3]), maximum = unsigned(f[4]);
                int x = Integer.parseInt(f[9]), y = Integer.parseInt(f[10]);
                int c = Integer.parseInt(f[5]), r = Integer.parseInt(f[6]), rev = Integer.parseInt(f[15]);
                if (c < 0 || r < 0 || rev < 0) throw new IllegalArgumentException(UiText.INVALID_GRID_STATUS.text());
                if ((plan != null && plan.runId != id) || f[1].equals("IDLE")) plan = null;
                runId = id; completed = count; total = maximum; pan = x; tilt = y;
                nextImageIndex=unsigned(f[14]);
                state = f[1]; phase = f[2]; column = c; row = r; revision = rev;
                lastError = f[13]; lastStatusMs = now; break;
            case "STEP_TIMING":
                if(f.length!=3) throw new IllegalArgumentException(UiText.INCOMPLETE_MOVEMENT_TIME.text());
                long timingRun=unsigned(f[0]),timingImage=unsigned(f[1]),maximumMs=unsigned(f[2]);
                if(maximumMs>120000) throw new IllegalArgumentException(UiText.MOVEMENT_TIME_OUTSIDE_LIMITS.text());
                timingRunId=timingRun;timingImageIndex=timingImage;moveMaximumMs=maximumMs;break;
            case "RUN_SPEED_CAPABILITIES":
                if(f.length!=1) throw new IllegalArgumentException(UiText.INCOMPLETE_RUN_SPEED_CAPABILITY.text());
                int runTempoMaximum=Integer.parseInt(f[0]);
                if(runTempoMaximum<1000 || runTempoMaximum>2000) throw new IllegalArgumentException(UiText.INVALID_RUN_SPEED_CAPABILITY.text());
                runMaximumTempo=runTempoMaximum;break;
            case "CAPTURE_RECOVERY_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_RETRY_CAPABILITY.text());
                recoveryPauseSupported=f[0].equals("1");break;
            case "CAPTURE_STABILITY_SETTINGS":
                CaptureStabilitySettings stability=CaptureStabilitySettings.parse(f);
                captureStability=stability;motionConfigRevision=stability.revision;break;
            case "CAPTURE_WARNING_SETTINGS":
                if(f.length!=3) throw new IllegalArgumentException(UiText.INCOMPLETE_WARNING_THRESHOLDS.text());
                int wp=Integer.parseInt(f[0]),wt=Integer.parseInt(f[1]); long wr=unsigned(f[2]);
                if(wp<44 || wp>10000 || wt<44 || wt>10000) throw new IllegalArgumentException(UiText.INVALID_WARNING_THRESHOLDS.text());
                warningPanMdeg=wp;warningTiltMdeg=wt;motionConfigRevision=wr;warningSettingsSupported=true;break;
            case "MANUAL_STATUS_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_MANUAL_STATUS_CAPABILITY.text());
                if(!manualStatusSupported && f[0].equals("1")) manualStatusExpectedMs=now;
                manualStatusSupported=f[0].equals("1"); break;
            case "MANUAL_JOG_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException(UiText.INVALID_JOG_CAPABILITY.text());
                jogSupported=f[0].equals("1"); break;
            case "MANUAL_JOG_VELOCITY_CAPABILITIES":
                if(f.length!=1) throw new IllegalArgumentException(UiText.INVALID_JOG_SPEED.text());
                int jogMaximum=Integer.parseInt(f[0]);
                if(jogMaximum!=0 && (jogMaximum<500 || jogMaximum>10000)) throw new IllegalArgumentException(UiText.INVALID_JOG_SPEED.text());
                jogMaximumVelocityMdeg=jogMaximum; break;
            case "MANUAL_JOG_PAN_VELOCITY_CAPABILITIES":
                if(f.length!=1) throw new IllegalArgumentException(UiText.INVALID_PAN_JOG_SPEED.text());
                int panJogMaximum=Integer.parseInt(f[0]);
                if(panJogMaximum!=0 && (panJogMaximum<500 || panJogMaximum>20000)) throw new IllegalArgumentException(UiText.INVALID_PAN_JOG_SPEED.text());
                jogMaximumPanVelocityMdeg=panJogMaximum; break;
            case "MANUAL_STATUS":
                ManualActionStatus manual=ManualActionStatus.parse(f);
                manualStatus=manual; manualStatusVersion++; lastManualStatusMs=now; break;
            default: break;
        }
    }
    public boolean fresh(long now) { return compatible && lastStatusMs > 0 && now - lastStatusMs < 2500; }
    boolean manualStatusFresh(long now) { return manualStatus!=null && lastManualStatusMs>0 && now-lastManualStatusMs<2500; }
    public boolean manual(long now) { return fresh(now) && (state.equals("IDLE") || state.equals("PAUSED")); }
    public boolean navigation(long now) {
        return manual(now) || fresh(now) && inactiveNavigation &&
            (state.equals("PLAN_READY") || state.equals("STOPPED") || state.equals("COMPLETED"));
    }
    public boolean positionSafe() {
        return limitsKnown && confirmed && pan >= panMin && pan <= panMax && tilt >= tiltMin && tilt <= tiltMax;
    }
    public void validateTarget(int x, int y) {
        if (!limitsKnown || !confirmed) throw new IllegalArgumentException(UiText.SAFETY_LIMITS_NOT_CONFIRMED.text());
        if (x < panMin || x > panMax || y < tiltMin || y > tiltMax)
            throw new IllegalArgumentException(UiText.TARGET_EXCEEDS_DEVICE_LIMITS.text());
    }
    public void validateManualTarget(int x,int y) {
        if(!limitsKnown || !confirmed) throw new IllegalArgumentException(UiText.SAFETY_LIMITS_NOT_CONFIRMED.text());
        if(!manualAxisAllowed(pan,x,panMin,panMax) || !manualAxisAllowed(tilt,y,tiltMin,tiltMax))
            throw new IllegalArgumentException(UiText.LIMIT_REACHED_MOVE_BACK_INTO_THE_ALLOWED_RANGE.text());
        if(!positionSafe()) {
            boolean panOutside=pan<panMin || pan>panMax,tiltOutside=tilt<tiltMin || tilt>tiltMax;
            if((!panOutside && x!=pan) || (!tiltOutside && y!=tilt) || (x==pan && y==tilt))
                throw new IllegalArgumentException(UiText.FIRST_MOVE_THE_AXIS_OUTSIDE_THE_LIMIT_BACK.text());
        }
    }
    private static boolean manualAxisAllowed(int actual,int target,int min,int max) {
        return actual<min?target>=actual && target<=max:actual>max?target<=actual && target>=min:target>=min && target<=max;
    }
    static int manualStep(int actual,int candidate,int min,int max) {
        // Clamp ordinary steps at the boundary; preserve small inward recovery steps.
        return actual>=min && actual<=max?Math.max(min,Math.min(max,candidate)):candidate;
    }
    static long unsigned(String text) {
        long value = Long.parseLong(text);
        if (value < 0 || value > 4294967295L) throw new IllegalArgumentException(UiText.VALUE_OUTSIDE_UINT32_RANGE.text());
        return value;
    }
    public static int toDevice(double degrees) {
        if (!Double.isFinite(degrees) || Math.abs(degrees * 1000) > Integer.MAX_VALUE)
            throw new IllegalArgumentException(UiText.INVALID_ANGLE.text());
        return (int)(-Math.copySign(Math.floor(Math.abs(degrees * 1000) + .5), degrees));
    }
    /** On this mount increasing encoder tilt raises the lens; positive UI tilt is up. */
    public static int toTiltDevice(double degrees) { return toDevice(-degrees); }
}
