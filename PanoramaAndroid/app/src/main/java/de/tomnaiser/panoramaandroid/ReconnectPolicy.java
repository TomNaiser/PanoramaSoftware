package de.tomnaiser.panoramaandroid;

/** Retry only a session that has completed the V2 handshake, with a bounded budget. */
final class ReconnectPolicy {
    private boolean established;
    private int attempts;

    void connected() { established = true; attempts = 0; }
    void stop() { established = false; attempts = 0; }
    int nextAttempt() { return established && attempts < 3 ? ++attempts : 0; }
}
