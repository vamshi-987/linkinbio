package com.backend.libserver.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Only letters, numbers and underscores allowed")
        String username,

        @Email @NotBlank
        String email,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
