package com.backend.libserver.profile.dto;

/** The signed-in user's own editable profile fields, used to populate the settings form. */
public record ProfileSettingsResponse(
        String displayName,
        String bio,
        String theme,
        String avatarUrl
) {}
