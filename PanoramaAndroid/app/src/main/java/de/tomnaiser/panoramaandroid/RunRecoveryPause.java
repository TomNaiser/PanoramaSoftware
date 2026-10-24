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
            "Position während der Aufnahme verändert. Das Bild wurde nicht gezählt; eine zusätzliche Kameradatei kann existieren.":
            s.lastError.equals("CAPTURE_NOT_STABLE")?"Position wurde vor der Aufnahme nicht ausreichend ruhig. Es wurde nicht ausgelöst.":
            "Die Fahrt konnte nicht zuverlässig abgeschlossen werden. Es wurde kein weiteres Bild ausgelöst.";
        return reason+"\n\nAuftrag und bisherige Aufnahmen bleiben erhalten. Motoren und Auslösung sind aus. Nach Quittierung im Reiter Lauf bewusst fortsetzen oder "+
            (s.plan!=null && s.plan.mode==1?"das betroffene Tilt-Band wiederholen.":"die betroffene oder vorherige Spalte wiederholen.");
    }
}
