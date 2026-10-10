package de.tomnaiser.panoramaandroid;

/** Frame shape is negotiated; existing firmware continues to receive two fields. */
final class ManualMotionOptions {
    static String[] fields(DeviceState state,int pan,int tilt,int tempo) {
        if (!state.profiledMotion) return new String[]{""+pan,""+tilt};
        if (tempo<1 || tempo>1000) throw new IllegalArgumentException("Fahrtempo muss 1–1000 ‰ betragen");
        return new String[]{""+pan,""+tilt,""+tempo,""+tempo};
    }
}
