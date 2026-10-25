package de.tomnaiser.panoramaandroid;

public final class CameraProfile {
    public final String name;
    public final double focal, width, height;
    public final int settleMs, exposureMs;
    public CameraProfile(String name, double focal, double width, double height, int settle, int exposure) {
        if (name.trim().isEmpty() || !Double.isFinite(focal) || !Double.isFinite(width) || !Double.isFinite(height)
                || focal <= 0 || width <= 0 || height <= 0 || settle < 0 || settle > 600000
                || exposure < 0 || exposure > 3600000) throw new IllegalArgumentException(UiText.INVALID_CAMERA_PROFILE.text());
        this.name = name; this.focal = focal; this.width = width; this.height = height;
        settleMs = settle; exposureMs = exposure;
    }
    public double fovX() { return Math.rint(Math.toDegrees(2 * Math.atan(width / (2 * focal))) * 1000) / 1000; }
    public double fovY() { return Math.rint(Math.toDegrees(2 * Math.atan(height / (2 * focal))) * 1000) / 1000; }
    @Override public String toString() { return name; }
}
