package de.tomnaiser.panoramaandroid;

/** Frame shape is negotiated; existing firmware continues to receive two fields. */
final class ManualMotionOptions {
    static String[] fields(DeviceState state,int pan,int tilt,int tempo) {
        return fields(state,pan,tilt,tempo,tempo);
    }
    static String[] fields(DeviceState state,int pan,int tilt,int panTempo,int tiltTempo) {
        if (!state.profiledMotion) return new String[]{""+pan,""+tilt};
        if (panTempo<1 || panTempo>1000 || tiltTempo<1 || tiltTempo>1000) throw new IllegalArgumentException("Fahrtempo muss 1–1000 ‰ betragen");
        return new String[]{""+pan,""+tilt,""+panTempo,""+tiltTempo};
    }
}
