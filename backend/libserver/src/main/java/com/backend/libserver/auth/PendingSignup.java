package com.backend.libserver.auth;

import java.time.Instant;

/**
 * A signup that has been requested but not yet email-verified. It lives only in Redis — never in the
 * {@code users} table — until the code is confirmed, so an unverified account never touches the
 * database. The password and OTP are stored hashed, exactly as they would be on a real user row.
 *
 * @param otpExpiresAt when the current code stops being valid (shorter than the Redis key's TTL,
 *                     which bounds how long the whole pending signup survives before it must restart)
 * @param attempts     wrong-code guesses so far against the current code, capped to stop brute force
 * @param issuedAt     when the current code was sent, for the resend cooldown
 */
public record PendingSignup(
        String username,
        String email,
        String passwordHash,
        String otpHash,
        Instant otpExpiresAt,
        int attempts,
        Instant issuedAt
) {
    /** A copy with the guess counter bumped; everything else (including code and expiry) unchanged. */
    public PendingSignup withFailedAttempt() {
        return new PendingSignup(username, email, passwordHash, otpHash, otpExpiresAt, attempts + 1, issuedAt);
    }
}
