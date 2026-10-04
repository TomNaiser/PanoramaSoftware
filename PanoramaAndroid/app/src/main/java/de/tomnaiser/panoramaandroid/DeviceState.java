package de.tomnaiser.panoramaandroid;

/** Validated firmware state in device millidegrees. Never inferred from button clicks. */
public final class DeviceState {
    public String firmware = "—", hardware = "—", state = "UNKNOWN", phase = "NONE", lastError = "—";
    public long runId, completed, total, lastStatusMs;
    public int pan, tilt, column, row, revision;
    public int panMin, panMax, tiltMin, tiltMax;
    public boolean limitsKnown, confirmed, compatible;
    public boolean tiltCalibrationKnown;
    public boolean inactiveNavigation;
    public int tiltZeroOffset, tiltAbsoluteMin, tiltAbsoluteMax;
    public PanoramaPlan plan;

    public void accept(P2Codec.Frame frame, long now) {
        String[] f = frame.fields;
        switch (frame.type) {
            case "DEVICE_INFO":
                if (f.length != 4) throw new IllegalArgumentException("Geräteinfo unvollständig");
                firmware = f[0]; hardware = f[3]; compatible = f[2].equals("2"); break;
            case "CAPABILITIES":
                if (f.length != 5) throw new IllegalArgumentException("Grenzen unvollständig");
                int p0 = Integer.parseInt(f[0]), p1 = Integer.parseInt(f[1]);
                int t0 = Integer.parseInt(f[2]), t1 = Integer.parseInt(f[3]);
                if (p0 >= p1 || t0 >= t1 || !(f[4].equals("0") || f[4].equals("1")))
                    throw new IllegalArgumentException("Ungültige Grenzen");
                panMin = p0; panMax = p1; tiltMin = t0; tiltMax = t1;
                confirmed = f[4].equals("1"); limitsKnown = true; break;
            case "TILT_CALIBRATION":
                if(f.length!=3) throw new IllegalArgumentException("Tilt-Referenz unvollständig");
                int zero=Integer.parseInt(f[0]), absoluteMinimum=Integer.parseInt(f[1]), absoluteMaximum=Integer.parseInt(f[2]);
                if(absoluteMinimum>=absoluteMaximum) throw new IllegalArgumentException("Tilt-Referenz ungültig");
                tiltZeroOffset=zero; tiltAbsoluteMin=absoluteMinimum; tiltAbsoluteMax=absoluteMaximum; tiltCalibrationKnown=true; break;
            case "NAVIGATION_CAPABILITIES":
                if(f.length!=1 || !(f[0].equals("0") || f[0].equals("1")))
                    throw new IllegalArgumentException("Navigationsfreigabe ungültig");
                inactiveNavigation=f[0].equals("1"); break;
            case "PLAN": plan = PanoramaPlan.parse(f); break;
            case "STATUS":
                if (f.length < 18) throw new IllegalArgumentException("Status unvollständig");
                long id = unsigned(f[0]), count = unsigned(f[3]), maximum = unsigned(f[4]);
                int x = Integer.parseInt(f[9]), y = Integer.parseInt(f[10]);
                int c = Integer.parseInt(f[5]), r = Integer.parseInt(f[6]), rev = Integer.parseInt(f[15]);
                if (c < 0 || r < 0 || rev < 0) throw new IllegalArgumentException("Ungültiger Rasterstatus");
                if ((plan != null && plan.runId != id) || f[1].equals("IDLE")) plan = null;
                runId = id; completed = count; total = maximum; pan = x; tilt = y;
                state = f[1]; phase = f[2]; column = c; row = r; revision = rev;
                lastError = f[13]; lastStatusMs = now; break;
            default: break;
        }
    }
    public boolean fresh(long now) { return compatible && lastStatusMs > 0 && now - lastStatusMs < 2500; }
    public boolean manual(long now) { return fresh(now) && (state.equals("IDLE") || state.equals("PAUSED")); }
    public boolean navigation(long now) {
        return manual(now) || fresh(now) && inactiveNavigation &&
            (state.equals("PLAN_READY") || state.equals("STOPPED") || state.equals("COMPLETED"));
    }
    public boolean positionSafe() {
        return limitsKnown && confirmed && pan >= panMin && pan <= panMax && tilt >= tiltMin && tilt <= tiltMax;
    }
    public void validateTarget(int x, int y) {
        if (!limitsKnown || !confirmed) throw new IllegalArgumentException("Sicherheitsgrenzen nicht bestätigt");
        if (x < panMin || x > panMax || y < tiltMin || y > tiltMax)
            throw new IllegalArgumentException("Ziel überschreitet die Gerätegrenzen");
    }
    public void validateManualTarget(int x,int y) {
        if(!limitsKnown || !confirmed) throw new IllegalArgumentException("Sicherheitsgrenzen nicht bestätigt");
        if(!manualAxisAllowed(pan,x,panMin,panMax) || !manualAxisAllowed(tilt,y,tiltMin,tiltMax))
            throw new IllegalArgumentException("Grenze erreicht: nur zurück in den erlaubten Bereich fahren");
        if(!positionSafe()) {
            boolean panOutside=pan<panMin || pan>panMax,tiltOutside=tilt<tiltMin || tilt>tiltMax;
            if((!panOutside && x!=pan) || (!tiltOutside && y!=tilt) || (x==pan && y==tilt))
                throw new IllegalArgumentException("Zuerst die Achse außerhalb der Grenze zurückfahren");
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
        if (value < 0 || value > 4294967295L) throw new IllegalArgumentException("Wert außerhalb uint32");
        return value;
    }
    public static int toDevice(double degrees) {
        if (!Double.isFinite(degrees) || Math.abs(degrees * 1000) > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Ungültiger Winkel");
        return (int)(-Math.copySign(Math.floor(Math.abs(degrees * 1000) + .5), degrees));
    }
    /** On this mount increasing encoder tilt raises the lens; positive UI tilt is up. */
    public static int toTiltDevice(double degrees) { return toDevice(-degrees); }
}
