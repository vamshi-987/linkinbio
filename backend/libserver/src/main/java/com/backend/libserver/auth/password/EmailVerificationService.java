package com.backend.libserver.auth.password;

import com.backend.libserver.auth.AuthResponse;
import com.backend.libserver.user.domain.User;

/** Email ownership check for new signups: send a code, then confirm it before the account is usable. */
public interface EmailVerificationService {

    /** Emails a fresh verification code to a user who has not confirmed their address yet. */
    void sendCode(User user);

    /**
     * Resends a code by email address. Silent when the address is unknown or already verified, so
     * this cannot be used to discover which emails are registered.
     */
    void resendCode(String email);

    /** Confirms the code and logs the user in, returning the same token shape as login/signup. */
    AuthResponse verify(String email, String otp);
}
