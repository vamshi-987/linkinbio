package com.backend.libserver.profile.service;

import com.backend.libserver.profile.dto.ProfileSettingsResponse;
import com.backend.libserver.profile.dto.UpdateProfileRequest;

import java.util.UUID;

public interface ProfileSettingsService {

    /** The user's current values, so the settings form can load them instead of starting blank. */
    ProfileSettingsResponse getProfile(UUID userId);

    /**
     * PATCH semantics: a null field is left untouched, an empty string clears the value. Callers
     * must therefore send only the fields they intend to change.
     */
    void updateProfile(UUID userId, UpdateProfileRequest req);

}
