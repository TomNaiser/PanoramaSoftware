package de.tomnaiser.panoramaandroid;

/** Preserve accepted destinations across live measurements and compensate for overshoot. */
final class ManualTargets {
    private ManualTargets() { }
    static int step(int target, int actual, int delta) {
        if (delta == 0) return target;
        long value = delta > 0 ? Math.max((long)target + delta, (long)actual + delta)
                              : Math.min((long)target + delta, (long)actual + delta);
        return Math.toIntExact(value);
    }
}
