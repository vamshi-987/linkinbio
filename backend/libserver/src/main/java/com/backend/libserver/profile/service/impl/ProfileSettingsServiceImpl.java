package com.backend.libserver.profile.service.impl;

import com.backend.libserver.profile.dto.ProfileSettingsResponse;
import com.backend.libserver.profile.dto.UpdateProfileRequest;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.profile.service.ProfileSettingsService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSettingsServiceImpl implements ProfileSettingsService {

    private final UserRepository userRepository;
    private final ProfileService profileService;

    @Override
    @Transactional
    public ProfileSettingsResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return new ProfileSettingsResponse(user.getDisplayName(), user.getBio(), user.getTheme());
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (req.displayName() != null) user.setDisplayName(req.displayName().trim());
        if (req.bio() != null) user.setBio(req.bio().trim());
        if (req.theme() != null) user.setTheme(req.theme());

        userRepository.save(user);
        profileService.evictProfileCache(user.getUsername());
    }

}
