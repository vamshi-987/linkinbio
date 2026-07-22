package com.backend.libserver.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Display name and bio are required on every update: a blank value here means the client sent an
 * emptied-out form, and silently keeping the stored value would report success while ignoring the
 * edit. Theme stays optional — absent simply means "leave it alone".
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Display name cannot be empty")
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        String displayName,

        @NotBlank(message = "Bio cannot be empty")
        @Size(max = 280, message = "Bio cannot exceed 280 characters")
        String bio,

        @Pattern(regexp = "default|dark|pastel|neon", message = "Invalid theme")
        String theme
) {}
