package de.tomnaiser.panoramaandroid;

/** A remaining device recipe cut at an original column boundary, never recalculated from a FOV. */
final class RasterRecovery {
    final PanoramaPlan source;
    final int nextIndex;
    RasterRecovery(PanoramaPlan source,long nextIndex) {
        if(source==null || source.mode!=0 || nextIndex<0 || nextIndex>=source.total())
            throw new IllegalArgumentException("Wiederaufnahme benötigt ein unterbrochenes Raster");
        this.source=source;this.nextIndex=(int)nextIndex;
    }
    static boolean recoverable(String error) {
        return error.equals("EXPOSURE_UNSTABLE") || error.equals("MOTION_STALLED") ||
            error.equals("MOTION_SETTLE_TIMEOUT") || error.equals("MOTION_NOT_QUIET") ||
            error.equals("MOVEMENT_TIMEOUT") || error.equals("MOTION_TARGET_MISSED") ||
            error.equals("CAPTURE_NOT_STABLE") || error.equals("MOTION_FINE_TIMEOUT");
    }
    int startColumn(boolean previous) { return Math.max(0,nextIndex/source.rows-(previous?1:0)); }
    PanoramaPlan remaining(long newId,boolean previous) {
        if(newId==source.runId) throw new IllegalArgumentException("Restauftrag braucht eine neue Laufnummer");
        int column=startColumn(previous);
        String[] fields=source.fields();fields[0]=""+newId;
        fields[3]=""+source.target(column*source.rows)[0];fields[5]=""+(source.columns-column);
        return PanoramaPlan.parse(fields);
    }
}
