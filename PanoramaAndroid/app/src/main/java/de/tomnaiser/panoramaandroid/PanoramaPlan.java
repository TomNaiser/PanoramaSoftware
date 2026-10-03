package de.tomnaiser.panoramaandroid;

import java.util.Arrays;

/** Firmware recipe, including integer spherical geometry identical to Windows. */
public final class PanoramaPlan {
    public final long runId;
    public final int mode, startPan, startTilt, columns, rows, stepPan, stepTilt, endPan, endTilt;
    public final int settleMs, pulseMs, exposureMs, panSpeed, tiltSpeed, policy, heartbeatMs;
    private PanoramaPlan(long id, int mode, int x, int y, int cols, int rows, int dx, int dy,
                         int ex, int ey, int settle, int pulse, int exposure, int speedX, int speedY,
                         int policy, int heartbeat) {
        this.runId = id; this.mode = mode; startPan = x; startTilt = y; columns = cols;
        this.rows = rows; stepPan = dx; stepTilt = dy; endPan = ex; endTilt = ey;
        settleMs = settle; pulseMs = pulse; exposureMs = exposure; panSpeed = speedX;
        tiltSpeed = speedY; this.policy = policy; heartbeatMs = heartbeat;
        if (id < 1 || id > 4294967295L || mode < 0 || mode > 1 || cols < 1 || rows < 1
                || cols > 65535 || rows > 4000 || settle < 0 || settle > 600000
                || pulse < 1 || pulse > 60000 || exposure < 0 || exposure > 3600000
                || speedX < 1 || speedX > 1000 || speedY < 1 || speedY > 1000
                || policy < 0 || policy > 1 || (policy == 1 && (heartbeat < 500 || heartbeat > 60000)))
            throw new IllegalArgumentException("Ungültige Planparameter");
        if (mode == 1 && (dx <= 0 || y < -90000 || y > 90000 || ey < -90000 || ey > 90000
                || (rows == 1 && y != ey) || Math.abs((long)ex - x) > 360000))
            throw new IllegalArgumentException("Ungültiges sphärisches Rezept");
        if (total() > 4000) throw new IllegalArgumentException("Mehr als 4000 Aufnahmen");
        if (mode == 1) {
            int max = 0; for (int band = 0; band < rows; band++) max = Math.max(max, shots(band));
            if (max != cols) throw new IllegalArgumentException("Sphärische Spaltenzahl inkonsistent");
        }
    }
    public static PanoramaPlan create(long id, boolean spherical, double sx, double sy,
            double ex, double ey, double fovX, double fovY, double overlap,
            int settle, int pulse, int exposure, int speed, boolean stopOnLoss) {
        if (!Double.isFinite(overlap) || overlap < 0 || overlap >= 100
                || !Double.isFinite(fovX) || !Double.isFinite(fovY) || fovX <= 0 || fovY <= 0)
            throw new IllegalArgumentException("Bildwinkel/Überlappung ungültig");
        double dx = fovX * (100 - overlap) / 100, dy = fovY * (100 - overlap) / 100;
        int x = DeviceState.toDevice(sx), y = DeviceState.toTiltDevice(sy);
        int endX = DeviceState.toDevice(ex), endY = DeviceState.toTiltDevice(ey);
        int stepX = Math.abs(DeviceState.toDevice(dx)), stepY = Math.abs(DeviceState.toDevice(dy));
        if (stepX < 1 || stepY < 1) throw new IllegalArgumentException("Bildschritt zu klein");
        long c, r;
        if (!spherical) {
            c = 1 + (long)Math.abs(Math.rint((ex - sx) / dx));
            r = 1 + (long)Math.abs(Math.rint((ey - sy) / dy));
            stepX = endX < x ? -stepX : stepX; stepY = endY < y ? -stepY : stepY;
        } else {
            r = endY == y ? 1 : (Math.abs((long)endY - y) + stepY - 1) / stepY + 1;
            if (r > 4000) throw new IllegalArgumentException("Zu viele Tilt-Bänder");
            c = 0;
            for (int band = 0; band < r; band++)
                c = Math.max(c, shotsInBand(x, endX, interpolate(y, endY, band, (int)r - 1), stepX));
        }
        if (c > 65535 || r > 4000 || c < 1 || r < 1) throw new IllegalArgumentException("Raster zu groß");
        return new PanoramaPlan(id, spherical ? 1 : 0, x, y, (int)c, (int)r, stepX, stepY,
            endX, endY, settle, pulse, exposure, speed, speed, stopOnLoss ? 1 : 0, stopOnLoss ? 5000 : 0);
    }
    public static PanoramaPlan parse(String[] fields) {
        if (fields.length != 16 && fields.length != 18) throw new IllegalArgumentException("Plan unvollständig");
        int[] n = new int[fields.length];
        for (int i = 1; i < fields.length; i++) n[i] = Integer.parseInt(fields[i]);
        if ((n[2] == 0 && (n[1] != 1 || fields.length != 16))
                || (n[2] == 1 && (n[1] != 2 || fields.length != 18)))
            throw new IllegalArgumentException("Planversion unbekannt");
        return new PanoramaPlan(DeviceState.unsigned(fields[0]), n[2], n[3], n[4], n[5], n[6], n[7], n[8],
            fields.length == 18 ? n[16] : 0, fields.length == 18 ? n[17] : 0,
            n[9], n[10], n[11], n[12], n[13], n[14], n[15]);
    }
    public int total() {
        if (mode == 0) return Math.multiplyExact(columns, rows);
        int count = 0; for (int band = 0; band < rows; band++) count = Math.addExact(count, shots(band));
        return count;
    }
    public int shots(int band) { return shotsInBand(startPan, endPan, interpolate(startTilt, endTilt, band, rows - 1), stepPan); }
    public int[] target(int index) {
        if (index < 0 || index >= total()) throw new IllegalArgumentException("Bildindex");
        if (mode == 0) return new int[] {
            Math.toIntExact(startPan + (long)(index / rows) * stepPan),
            Math.toIntExact(startTilt + (long)(index % rows) * stepTilt), index / rows, index % rows };
        for (int band = 0; band < rows; band++) {
            int count = shots(band);
            if (index < count) return new int[] { interpolate(startPan, endPan, index,
                Math.abs((long)endPan - startPan) == 360000 ? count : count - 1),
                interpolate(startTilt, endTilt, band, rows - 1), index, band };
            index -= count;
        }
        throw new IllegalArgumentException("Bildindex");
    }
    public void validate(DeviceState device) {
        for (int i = 0; i < total(); i++) { int[] t = target(i); device.validateTarget(t[0], t[1]); }
        if (mode == 1) device.validateTarget(endPan, endTilt);
        if (P2Codec.encode(65535, "LOAD_PLAN", fields()).length() > 164)
            throw new IllegalArgumentException("Auftrag überschreitet Firmware-Rahmenlimit");
    }
    public String[] fields() {
        String[] f = new String[] { Long.toString(runId), mode == 0 ? "1" : "2", Integer.toString(mode),
            ""+startPan, ""+startTilt, ""+columns, ""+rows, ""+stepPan, ""+stepTilt,
            ""+settleMs, ""+pulseMs, ""+exposureMs, ""+panSpeed, ""+tiltSpeed, ""+policy, ""+heartbeatMs };
        if (mode == 0) return f;
        f = Arrays.copyOf(f, 18); f[16] = ""+endPan; f[17] = ""+endTilt; return f;
    }
    /** Run identities differ on each preview; compare the actual device recipe. */
    public boolean sameRecipe(PanoramaPlan other) {
        if(other==null) return false;
        String[] a=fields(),b=other.fields();
        return Arrays.equals(Arrays.copyOfRange(a,1,a.length),Arrays.copyOfRange(b,1,b.length));
    }
    static int shotsInBand(int x, int ex, int tilt, int step) {
        if (step <= 0 || tilt < -90000 || tilt > 90000) throw new IllegalArgumentException("Bandparameter");
        long span = Math.abs((long)ex - x); if (span == 0) return 1;
        long radians = Math.abs((long)tilt) * 3141593L / 180000L;
        long square = radians * radians / 1000000L, fourth = square * square / 1000000L;
        long sixth = fourth * square / 1000000L;
        long cosine = 1000000L - square / 2 + fourth / 24 - sixth / 720;
        long denominator = (long)step * 1000000L;
        long intervals = (span * cosine + denominator - 1) / denominator;
        return Math.toIntExact(span == 360000 ? Math.max(1, intervals) : intervals + 1);
    }
    private static int interpolate(int start, int end, int index, int divisor) {
        if (divisor == 0) return start;
        long v = ((long)end - start) * index;
        return Math.toIntExact(start + (v >= 0 ? v + divisor / 2 : v - divisor / 2) / divisor);
    }
}
