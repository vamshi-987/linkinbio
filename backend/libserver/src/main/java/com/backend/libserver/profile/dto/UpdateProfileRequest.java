package com.backend.libserver.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 280) String bio,
        @Pattern(regexp = "default|dark|pastel|neon", message = "Invalid theme")
        String theme
) {}
