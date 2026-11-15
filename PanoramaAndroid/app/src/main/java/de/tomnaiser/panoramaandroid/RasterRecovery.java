package de.tomnaiser.panoramaandroid;

/** Remaining recipe at an original column/band boundary, without recalculating a FOV. */
final class RasterRecovery {
    final PanoramaPlan source;
    final int nextIndex;
    RasterRecovery(PanoramaPlan source,long nextIndex) {
        if(source==null || nextIndex<0 || nextIndex>=source.total())
            throw new IllegalArgumentException(UiText.RECOVERY_REQUIRES_AN_INTERRUPTED_PANORAMA_JOB.text());
        this.source=source;this.nextIndex=(int)nextIndex;
    }
    static boolean recoverable(String error) {
        return error.equals("EXPOSURE_UNSTABLE") || error.equals("MOTION_STALLED") ||
            error.equals("MOTION_SETTLE_TIMEOUT") || error.equals("MOTION_NOT_QUIET") ||
            error.equals("MOVEMENT_TIMEOUT") || error.equals("MOTION_TARGET_MISSED") ||
            error.equals("CAPTURE_NOT_STABLE") || error.equals("MOTION_FINE_TIMEOUT") ||
            error.equals("FAULT_PAN_SENSOR") || error.equals("FAULT_TILT_SENSOR") || error.equals("MOTION_SENSOR") ||
            error.equals("FAULT_TILT_LIMIT") || error.equals("CONTROL_TIMEOUT") || error.equals("TX_OVERFLOW") ||
            error.equals("CONNECTION_TIMEOUT") || error.equals("ANDROID_ESTOP") || error.equals("USER_STOP") ||
            error.equals("TARGET_REJECTED");
    }
    static boolean uncertainExposure(String error) {
        return recoverable(error) && !(error.equals("CAPTURE_NOT_STABLE") || error.equals("MOTION_STALLED") ||
            error.equals("MOTION_SETTLE_TIMEOUT") || error.equals("MOTION_NOT_QUIET") || error.equals("MOVEMENT_TIMEOUT") ||
            error.equals("MOTION_TARGET_MISSED") || error.equals("MOTION_FINE_TIMEOUT") || error.equals("TARGET_REJECTED"));
    }
    String unit() { return source.mode==0?UiText.COLUMN.text():UiText.TILT_BAND.text(); }
    int startColumn(boolean previous) {
        int requested=Math.max(0,(source.mode==0?nextIndex/source.rows:source.target(nextIndex)[3])-(previous?1:0));
        if(source.mode==0 && source.version!=3) return requested;
        // Cutting an interpolated recipe changes the divisor. Move the
        // boundary backwards if rounding would alter an original coordinate.
        boolean rectangular=source.mode==0;
        int first=rectangular?source.startPan:source.startTilt;
        int last=rectangular?source.endPan:source.endTilt;
        int count=rectangular?source.columns:source.rows;
        for(int start=requested;start>=0;start--) {
            int coordinate=PanoramaPlan.interpolate(first,last,start,count-1);
            boolean exact=true;
            for(int index=start;index<count;index++)
                if(PanoramaPlan.interpolate(first,last,index,count-1)!=
                   PanoramaPlan.interpolate(coordinate,last,index-start,count-start-1)) {exact=false;break;}
            if(exact) return start;
        }
        throw new IllegalStateException(UiText.ORIGINAL_BAND_BOUNDARIES_MISSING.text());
    }
    PanoramaPlan remaining(long newId,boolean previous) {
        if(newId==source.runId) throw new IllegalArgumentException(UiText.RECOVERY_JOB_NEEDS_A_NEW_RUN_NUMBER.text());
        int column=startColumn(previous);
        String[] fields=source.fields();fields[0]=""+newId;
        if(source.mode==0) {
            fields[3]=""+source.target(column*source.rows)[0];fields[5]=""+(source.columns-column);
        } else {
            fields[4]=""+PanoramaPlan.interpolate(source.startTilt,source.endTilt,column,source.rows-1);
            fields[6]=""+(source.rows-column);
            int maximum=0;for(int band=column;band<source.rows;band++) maximum=Math.max(maximum,source.shots(band));
            fields[5]=""+maximum;
        }
        return PanoramaPlan.parse(fields);
    }
}
