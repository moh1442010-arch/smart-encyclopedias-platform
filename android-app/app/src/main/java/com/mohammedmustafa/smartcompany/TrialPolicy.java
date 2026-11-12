package com.mohammedmustafa.smartcompany;

/**
 * Deterministic policy for the seven-day offline trial.
 *
 * This class deliberately does not persist state, activate paid licenses, or claim
 * tamper-proof time. A production integration must persist state using protected
 * storage and verify a signed license/server response before treating a customer
 * as paid. Wall-clock checks alone can be bypassed by a determined user.
 */
public final class TrialPolicy {
    public static final long TRIAL_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;
    public static final long CLOCK_ROLLBACK_TOLERANCE_MILLIS = 5L * 60L * 1000L;

    public enum Status {
        NOT_STARTED,
        ACTIVE,
        EXPIRED,
        CLOCK_ROLLBACK,
        INVALID_STATE
    }

    private TrialPolicy() { }

    public static Status evaluate(long firstStartMillis, long lastSeenMillis, long nowMillis) {
        if (firstStartMillis <= 0L || lastSeenMillis <= 0L || nowMillis <= 0L) {
            return Status.INVALID_STATE;
        }
        if (lastSeenMillis < firstStartMillis) {
            return Status.INVALID_STATE;
        }
        if (nowMillis + CLOCK_ROLLBACK_TOLERANCE_MILLIS < lastSeenMillis
                || nowMillis + CLOCK_ROLLBACK_TOLERANCE_MILLIS < firstStartMillis) {
            return Status.CLOCK_ROLLBACK;
        }
        long elapsed = nowMillis - firstStartMillis;
        if (elapsed < 0L) {
            return Status.CLOCK_ROLLBACK;
        }
        return elapsed < TRIAL_DURATION_MILLIS ? Status.ACTIVE : Status.EXPIRED;
    }
}
