package com.backend.libserver.auth.password.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @Email @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
        String otp
) {}
