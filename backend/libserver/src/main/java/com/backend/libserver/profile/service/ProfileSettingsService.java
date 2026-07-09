package com.backend.libserver.profile.service;

import com.backend.libserver.profile.dto.UpdateProfileRequest;

import java.util.UUID;

public interface ProfileSettingsService {

    void updateProfile(UUID userId, UpdateProfileRequest req);

}
