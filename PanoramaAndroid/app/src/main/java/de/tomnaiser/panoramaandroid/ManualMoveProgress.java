package de.tomnaiser.panoramaandroid;

/** Completion evidence for one accepted move, bounded by a pre-move log snapshot. */
final class ManualMoveProgress {
    private final int baseline, pan, tilt;
    private boolean started;
    ManualMoveProgress(int baseline, int pan, int tilt) {
        this.baseline = baseline; this.pan = pan; this.tilt = tilt;
    }
    boolean accept(String[] fields) {
        if (fields.length < 14) throw new IllegalArgumentException("Debug-Ereignis unvollständig");
        int seq = Integer.parseInt(fields[0]), code = Integer.parseInt(fields[2]);
        int distance = (seq - baseline) & 65535;
        if (seq < 1 || seq > 65535 || distance == 0 || distance >= 32768) return false;
        // Firmware stores actual position and error separately in tenths of a degree.
        boolean matches = Math.abs((long)Integer.parseInt(fields[7])-pan) < 200
            && Math.abs((long)Integer.parseInt(fields[8])-tilt) < 200;
        if (code == 13) started = matches;
        return code == 14 && started && matches;
    }
}
