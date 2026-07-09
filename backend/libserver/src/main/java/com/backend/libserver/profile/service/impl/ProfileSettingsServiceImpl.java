package com.backend.libserver.profile.service.impl;

import com.backend.libserver.profile.dto.UpdateProfileRequest;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSettingsServiceImpl {

    private final UserRepository userRepository;
    private final ProfileService profileService;

    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (req.displayName() != null) user.setDisplayName(req.displayName());
        if (req.bio() != null) user.setBio(req.bio());
        if (req.theme() != null) user.setTheme(req.theme());

        userRepository.save(user);
        profileService.evictProfileCache(user.getUsername());
    }

}
