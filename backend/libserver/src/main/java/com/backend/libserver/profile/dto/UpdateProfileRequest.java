package com.backend.libserver.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Display name is required on every update: it is what the public page is headed with, and a blank
 * value here means the client sent an emptied-out form, which must not silently report success.
 *
 * <p>Bio and theme are optional in the same sense — absent means "leave it alone", so a client that
 * only knows about one field (onboarding, which asks for the name alone) can send just that. A bio
 * that is present but empty is an intentional clear and is applied as one.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Display name cannot be empty")
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        String displayName,

        @Size(max = 280, message = "Bio cannot exceed 280 characters")
        String bio,

        @Pattern(regexp = "default|dark|pastel|neon", message = "Invalid theme")
        String theme
) {}
