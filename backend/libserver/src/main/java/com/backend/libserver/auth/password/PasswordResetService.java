package com.backend.libserver.auth.password;

/** OTP-backed password reset: request a code, verify it for a reset token, then set a new password. */
public interface PasswordResetService {

    /**
     * Emails a reset code if the address belongs to a user. Returns silently either way so callers
     * cannot use this to discover which emails are registered.
     */
    void requestReset(String email);

    /** Verifies a reset code and, on success, returns a short-lived reset token. */
    String verifyOtp(String email, String otp);

    /** Sets a new password for the user identified by a token from {@link #verifyOtp}. */
    void resetPassword(String resetToken, String newPassword);
}
