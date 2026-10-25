package de.tomnaiser.panoramaandroid;

/** Only acquisition problems whose firmware contract preserves the loaded run. */
final class RunRecoveryPause {
    static boolean reason(String error) {
        return "EXPOSURE_UNSTABLE".equals(error) || "CAPTURE_NOT_STABLE".equals(error) ||
            "MOTION_STALLED".equals(error) || "MOTION_SETTLE_TIMEOUT".equals(error) ||
            "MOTION_FINE_TIMEOUT".equals(error) || "MOVEMENT_TIMEOUT".equals(error) || "MOTION_NOT_QUIET".equals(error);
    }
    static boolean active(DeviceState s) {
        return s.recoveryPauseSupported && s.state.equals("PAUSED") && s.runId!=0 && reason(s.lastError);
    }
    static String detail(DeviceState s) {
        String reason=s.lastError.equals("EXPOSURE_UNSTABLE")?
            UiText.POSITION_CHANGED_DURING_EXPOSURE_THE_IMAGE_WAS_NOT.text():
            s.lastError.equals("CAPTURE_NOT_STABLE")?UiText.POSITION_DID_NOT_SETTLE_SUFFICIENTLY_BEFORE_CAPTURE_THE.text():
            UiText.MOVEMENT_COULD_NOT_BE_COMPLETED_RELIABLY_NO_FURTHER.text();
        return reason+UiText.THE_JOB_AND_EXISTING_CAPTURES_ARE_RETAINED_MOTORS.text()+
            (s.plan!=null && s.plan.mode==1?UiText.REPEAT_THE_AFFECTED_TILT_BAND.text():UiText.REPEAT_THE_AFFECTED_OR_PREVIOUS_COLUMN.text());
    }
}
