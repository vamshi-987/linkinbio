package com.backend.libserver.auth.password;

/** Delivers one-time codes to users by email. */
public interface EmailService {

    /**
     * Sends the code for the given purpose. Implementations must never throw on delivery failure —
     * a broken mailbox or misconfigured SMTP host must not fail the signup/reset request itself.
     */
    void sendOtp(String toEmail, String otp, long expiryMinutes, OtpPurpose purpose);
}
