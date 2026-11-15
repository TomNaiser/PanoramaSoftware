package de.tomnaiser.panoramaandroid;

import java.util.Arrays;

/** Firmware recipe, including integer spherical geometry identical to Windows. */
public final class PanoramaPlan {
    public final long runId;
    public final int version, mode, startPan, startTilt, columns, rows, stepPan, stepTilt, endPan, endTilt;
    public final int settleMs, pulseMs, exposureMs, panSpeed, tiltSpeed, policy, heartbeatMs;
    private PanoramaPlan(long id, int version, int mode, int x, int y, int cols, int rows, int dx, int dy,
                         int ex, int ey, int settle, int pulse, int exposure, int speedX, int speedY,
                         int policy, int heartbeat) {
        this.runId = id; this.version = version; this.mode = mode; startPan = x; startTilt = y; columns = cols;
        this.rows = rows; stepPan = dx; stepTilt = dy; endPan = ex; endTilt = ey;
        settleMs = settle; pulseMs = pulse; exposureMs = exposure; panSpeed = speedX;
        tiltSpeed = speedY; this.policy = policy; heartbeatMs = heartbeat;
        if (id < 1 || id > 4294967295L || mode < 0 || mode > 1 || cols < 1 || rows < 1
                || cols > 65535 || rows > 4000 || settle < 0 || settle > 600000
                || pulse < 1 || pulse > 60000 || exposure < 0 || exposure > 3600000
                || speedX < 1 || speedX > 2000 || speedY < 1 || speedY > 2000
                || policy < 0 || policy > 1 || (policy == 1 && (heartbeat < 500 || heartbeat > 60000)))
            throw new IllegalArgumentException(UiText.INVALID_PLAN_PARAMETERS.text());
        if (mode == 1 && (dx <= 0 || y < -90000 || y > 90000 || ey < -90000 || ey > 90000
                || (rows == 1 && y != ey) || Math.abs((long)ex - x) > 360000))
            throw new IllegalArgumentException(UiText.INVALID_SPHERICAL_RECIPE.text());
        if (version == 3 && mode == 0 &&
                (!validAxis(x, ex, cols, dx) || !validAxis(y, ey, rows, dy)))
            throw new IllegalArgumentException(UiText.INVALID_PLAN_PARAMETERS.text());
        if (total() > 4000) throw new IllegalArgumentException(UiText.MORE_THAN_4000_CAPTURES.text());
        if (mode == 1) {
            int max = 0; for (int band = 0; band < rows; band++) max = Math.max(max, shots(band));
            if (max != cols) throw new IllegalArgumentException(UiText.INCONSISTENT_SPHERICAL_COLUMN_COUNT.text());
        }
    }
    public static PanoramaPlan create(long id, boolean spherical, double sx, double sy,
            double ex, double ey, double fovX, double fovY, double overlap,
            int settle, int pulse, int exposure, int speed, boolean stopOnLoss) {
        if (!Double.isFinite(overlap) || overlap < 0 || overlap >= 100
                || !Double.isFinite(fovX) || !Double.isFinite(fovY) || fovX <= 0 || fovY <= 0)
            throw new IllegalArgumentException(UiText.INVALID_FIELD_OF_VIEW_OVERLAP.text());
        double overlapFactor = (100 - overlap) / 100;
        double dx = fovX * overlapFactor, dy = fovY * overlapFactor;
        int x = DeviceState.toDevice(sx), y = DeviceState.toTiltDevice(sy);
        int endX = DeviceState.toDevice(ex), endY = DeviceState.toTiltDevice(ey);
        int stepX = (int)Math.floor(dx * 1000), stepY = (int)Math.floor(dy * 1000);
        if (stepX < 1 || stepY < 1) throw new IllegalArgumentException(UiText.IMAGE_STEP_TOO_SMALL.text());
        long c, r;
        int version;
        if (!spherical) {
            long panSpan = (long)endX - x, tiltSpan = (long)endY - y;
            c = 1 + intervals(panSpan, stepX);
            r = 1 + intervals(tiltSpan, stepY);
            version = 3;
            if ((c == 1 || panSpan % (c - 1) == 0) && (r == 1 || tiltSpan % (r - 1) == 0)) {
                version = 1;
                stepX = c == 1 ? 0 : (int)(panSpan / (c - 1));
                stepY = r == 1 ? 0 : (int)(tiltSpan / (r - 1));
            }
        } else {
            boolean closeCircle = Math.abs((long)endX - x) == 360000;
            version = closeCircle ? 3 : 2;
            r = 1 + intervals((long)endY - y, stepY);
            if (r > 4000) throw new IllegalArgumentException(UiText.TOO_MANY_TILT_BANDS.text());
            c = 0;
            for (int band = 0; band < r; band++)
                c = Math.max(c, shotsInBand(x, endX, interpolate(y, endY, band, (int)r - 1), stepX, closeCircle));
        }
        if (c > 65535 || r > 4000 || c < 1 || r < 1) throw new IllegalArgumentException(UiText.GRID_TOO_LARGE.text());
        return new PanoramaPlan(id, version, spherical ? 1 : 0, x, y, (int)c, (int)r, stepX, stepY,
            endX, endY, settle, pulse, exposure, speed, speed, stopOnLoss ? 1 : 0, stopOnLoss ? 5000 : 0);
    }
    public static PanoramaPlan parse(String[] fields) {
        if (fields.length != 16 && fields.length != 18) throw new IllegalArgumentException(UiText.INCOMPLETE_PLAN.text());
        int[] n = new int[fields.length];
        for (int i = 1; i < fields.length; i++) n[i] = Integer.parseInt(fields[i]);
        boolean hasEndpoints = n[2] == 1 || n[1] == 3;
        if ((n[1] != 3 && n[1] != (n[2] == 1 ? 2 : 1)) || fields.length != (hasEndpoints ? 18 : 16))
            throw new IllegalArgumentException(UiText.UNKNOWN_PLAN_VERSION.text());
        return new PanoramaPlan(DeviceState.unsigned(fields[0]), n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8],
            fields.length == 18 ? n[16] : 0, fields.length == 18 ? n[17] : 0,
            n[9], n[10], n[11], n[12], n[13], n[14], n[15]);
    }
    public int total() {
        if (mode == 0) return Math.multiplyExact(columns, rows);
        int count = 0; for (int band = 0; band < rows; band++) count = Math.addExact(count, shots(band));
        return count;
    }
    public int shots(int band) { return shotsInBand(startPan, endPan, interpolate(startTilt, endTilt, band, rows - 1), stepPan, version == 3); }
    public int[] target(int index) {
        if (index < 0 || index >= total()) throw new IllegalArgumentException(UiText.IMAGE_INDEX.text());
        if (mode == 0) return new int[] {
            version == 3 ? interpolate(startPan, endPan, index / rows, columns - 1)
                : Math.toIntExact(startPan + (long)(index / rows) * stepPan),
            version == 3 ? interpolate(startTilt, endTilt, index % rows, rows - 1)
                : Math.toIntExact(startTilt + (long)(index % rows) * stepTilt), index / rows, index % rows };
        for (int band = 0; band < rows; band++) {
            int count = shots(band);
            if (index < count) return new int[] { interpolate(startPan, endPan, index,
                version == 2 && Math.abs((long)endPan - startPan) == 360000 ? count : count - 1),
                interpolate(startTilt, endTilt, band, rows - 1), index, band };
            index -= count;
        }
        throw new IllegalArgumentException(UiText.IMAGE_INDEX.text());
    }
    public void validate(DeviceState device) {
        if (version == 3 && !device.endpointPlans)
            throw new IllegalArgumentException(UiText.ENDPOINT_PLAN_REQUIRES_NEWER_FIRMWARE.text());
        if(panSpeed>device.runMaximumTempo || tiltSpeed>device.runMaximumTempo)
            throw new IllegalArgumentException(UiText.DEVICE_SUPPORTS_AT_MOST.text()+device.runMaximumTempo+UiText.RUN_SPEED_NEWER_FIRMWARE_REQUIRED.text());
        for (int i = 0; i < total(); i++) { int[] t = target(i); device.validateTarget(t[0], t[1]); }
        if (mode == 1) device.validateTarget(endPan, endTilt);
        if (P2Codec.encode(65535, "LOAD_PLAN", fields()).length() > 164)
            throw new IllegalArgumentException(UiText.JOB_EXCEEDS_FIRMWARE_FRAME_LIMIT.text());
    }
    public String[] fields() {
        String[] f = new String[] { Long.toString(runId), ""+version, Integer.toString(mode),
            ""+startPan, ""+startTilt, ""+columns, ""+rows, ""+stepPan, ""+stepTilt,
            ""+settleMs, ""+pulseMs, ""+exposureMs, ""+panSpeed, ""+tiltSpeed, ""+policy, ""+heartbeatMs };
        if (version == 1) return f;
        f = Arrays.copyOf(f, 18); f[16] = ""+endPan; f[17] = ""+endTilt; return f;
    }
    /** Run identities differ on each preview; compare the actual device recipe. */
    public boolean sameRecipe(PanoramaPlan other) {
        if(other==null) return false;
        String[] a=fields(),b=other.fields();
        return Arrays.equals(Arrays.copyOfRange(a,1,a.length),Arrays.copyOfRange(b,1,b.length));
    }
    private static long intervals(long span, int maximum) { return (Math.abs(span) + maximum - 1) / maximum; }
    private static boolean validAxis(int start, int end, int count, int maximum) {
        long span = Math.abs((long)end - start);
        return maximum > 0 && span >= count - 1 && span <= (long)(count - 1) * maximum;
    }
    static int shotsInBand(int x, int ex, int tilt, int step) { return shotsInBand(x, ex, tilt, step, false); }
    static int shotsInBand(int x, int ex, int tilt, int step, boolean includeEnd) {
        if (step <= 0 || tilt < -90000 || tilt > 90000) throw new IllegalArgumentException(UiText.BAND_PARAMETERS.text());
        long span = Math.abs((long)ex - x); if (span == 0) return 1;
        long radians = Math.abs((long)tilt) * 3141593L / 180000L;
        long square = radians * radians / 1000000L, fourth = square * square / 1000000L;
        long sixth = fourth * square / 1000000L;
        long cosine = Math.max(1, 1000000L - square / 2 + fourth / 24 - sixth / 720);
        long denominator = (long)step * 1000000L;
        long intervals = (span * cosine + denominator - 1) / denominator;
        return Math.toIntExact(span == 360000 && !includeEnd ? intervals : intervals + 1);
    }
    static int interpolate(int start, int end, int index, int divisor) {
        if (divisor == 0) return start;
        long v = ((long)end - start) * index;
        return Math.toIntExact(start + (v >= 0 ? v + divisor / 2 : v - divisor / 2) / divisor);
    }
}
