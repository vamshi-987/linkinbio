package com.backend.libserver.auth.password;

import com.backend.libserver.auth.AuthResponse;
import com.backend.libserver.auth.PendingSignup;

import java.util.Optional;

/**
 * Email ownership check for new signups. The account is held in Redis (never the database) until the
 * emailed code is confirmed; only then is the user row created and the caller logged in.
 */
public interface EmailVerificationService {

    /** Stages a pending signup and emails a code. No user row is created yet. */
    void startSignup(String username, String email, String rawPassword);

    /**
     * Resends a code by email address. Silent when the address has no pending signup, so this cannot
     * be used to discover which emails are registered.
     */
    void resendCode(String email);

    /**
     * Confirms the code, creates the verified user row, and logs them in — returning the same token
     * shape as login/signup.
     */
    AuthResponse verify(String email, String otp);

    /** The pending signup for a username, if one is awaiting verification — used by login. */
    Optional<PendingSignup> pendingSignupByUsername(String username);
}
