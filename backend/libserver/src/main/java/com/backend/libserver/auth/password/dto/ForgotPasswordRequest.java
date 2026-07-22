package com.backend.libserver.auth.password.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A password reset is requested by username, never by email: the email is only ever entered at
 * signup. The code is sent to whatever address that account registered.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Username cannot be empty")
        String username
) {}
