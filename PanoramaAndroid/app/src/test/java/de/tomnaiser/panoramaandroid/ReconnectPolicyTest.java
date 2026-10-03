package de.tomnaiser.panoramaandroid;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class ReconnectPolicyTest {
    @Test public void initialFailureDoesNotRetry() {
        assertEquals(0, new ReconnectPolicy().nextAttempt());
    }

    @Test public void lostConnectionHasThreeAttempts() {
        ReconnectPolicy policy = new ReconnectPolicy();
        policy.connected();
        assertEquals(1, policy.nextAttempt());
        assertEquals(2, policy.nextAttempt());
        assertEquals(3, policy.nextAttempt());
        assertEquals(0, policy.nextAttempt());
    }

    @Test public void userDisconnectCancelsRetries() {
        ReconnectPolicy policy = new ReconnectPolicy();
        policy.connected();
        policy.nextAttempt();
        policy.stop();
        assertEquals(0, policy.nextAttempt());
    }

    @Test public void successfulHandshakeRestoresBudget() {
        ReconnectPolicy policy = new ReconnectPolicy();
        policy.connected();
        policy.nextAttempt();
        policy.connected();
        assertEquals(1, policy.nextAttempt());
    }
}
