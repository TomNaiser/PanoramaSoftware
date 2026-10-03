package de.tomnaiser.panoramaandroid;

/** A firmware boot identity survives Bluetooth reconnects, but changes on power-up. */
final class DeviceSession {
    private long bootId;
    private boolean received;
    DeviceSession(long previousBootId) { bootId=previousBootId; }
    boolean accept(long nextBootId) {
        if(nextBootId<0 || nextBootId>4294967295L) throw new IllegalArgumentException("Ungültige Gerätesitzung");
        boolean reset=(nextBootId==0 && !received) || (bootId>=0 && bootId!=nextBootId);
        bootId=nextBootId; received=true; return reset;
    }
}
