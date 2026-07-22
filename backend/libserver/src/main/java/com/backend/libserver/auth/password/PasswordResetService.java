package com.backend.libserver.auth.password;

/** OTP-backed password reset: request a code, verify it for a reset token, then set a new password. */
public interface PasswordResetService {

    /**
     * Emails a reset code to the address registered by the given username, if that user exists.
     * Returns silently either way so callers cannot use this to discover which usernames are
     * registered (usernames are already public, but their registered email is not).
     */
    void requestReset(String username);

    /** Verifies a reset code for the username and, on success, returns a short-lived reset token. */
    String verifyOtp(String username, String otp);

    /** Sets a new password for the user identified by a token from {@link #verifyOtp}. */
    void resetPassword(String resetToken, String newPassword);
}
