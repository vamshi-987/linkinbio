package com.backend.libserver.auth;

import lombok.Getter;

/**
 * Thrown when credentials are correct but the account's email has never been confirmed. Carries the
 * email so the client can jump straight to the verification step — only ever thrown after the
 * password check passes, so this does not leak the address to anyone but the account owner.
 */
@Getter
public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Please verify your email address to continue. We've sent you a new code.");
        this.email = email;
    }
}
