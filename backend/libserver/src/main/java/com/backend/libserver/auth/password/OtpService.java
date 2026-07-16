package com.backend.libserver.auth.password;

import com.backend.libserver.user.domain.User;

/**
 * Issues and verifies the one-time codes emailed to users. Shared by email verification at signup
 * and by password resets — the {@link OtpPurpose} keeps the two flows isolated so a code minted for
 * one can never be redeemed for the other.
 */
public interface OtpService {

    /**
     * Generates a fresh code, invalidates any earlier ones for this purpose, and emails it.
     *
     * <p>Rate limited per user: a request made during the resend cooldown, or beyond the hourly cap,
     * is silently ignored — no new code, no email, no error. Callers must respond identically either
     * way, or they would leak whether an address is registered.
     */
    void issue(User user, OtpPurpose purpose);

    /**
     * Consumes the code on success.
     *
     * @throws IllegalArgumentException (mapped to HTTP 400) with a generic message on any failure,
     *                                  so callers cannot probe for valid codes.
     */
    void verify(User user, String otp, OtpPurpose purpose);
}
