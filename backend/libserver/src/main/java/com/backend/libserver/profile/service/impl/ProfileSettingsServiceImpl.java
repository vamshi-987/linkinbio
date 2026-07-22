package com.backend.libserver.profile.service.impl;

import com.backend.libserver.profile.dto.ProfileSettingsResponse;
import com.backend.libserver.profile.dto.UpdateProfileRequest;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.profile.service.ProfileSettingsService;
import com.backend.libserver.storage.ImageUploadValidator;
import com.backend.libserver.storage.MediaUrlResolver;
import com.backend.libserver.storage.StorageService;
import com.backend.libserver.storage.StoredObject;
import com.backend.libserver.storage.ValidatedImage;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSettingsServiceImpl implements ProfileSettingsService {

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final StorageService storageService;
    private final MediaUrlResolver mediaUrlResolver;
    private final ImageUploadValidator imageUploadValidator;

    @Override
    @Transactional
    public ProfileSettingsResponse getProfile(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = requireUser(userId);

        // Name and bio are @NotBlank, so they are always present and meaningful here.
        user.setDisplayName(req.displayName().trim());
        user.setBio(req.bio().trim());
        if (req.theme() != null) user.setTheme(req.theme());

        userRepository.save(user);
        profileService.evictProfileCache(user.getUsername());
    }

    @Override
    @Transactional
    public ProfileSettingsResponse uploadAvatar(UUID userId, MultipartFile file) {
        // Validated before anything else: a rejected upload should cost a decode, not a write.
        ValidatedImage image = imageUploadValidator.validate(file);
        User user = requireUser(userId);

        String previousKey = user.getAvatarKey();
        StoredObject stored = storageService.store(
                "avatars", image.bytes(), image.contentType(), image.extension());

        user.setAvatarKey(stored.key());
        user.setAvatarUrl(stored.url());
        userRepository.save(user);

        // The old object goes only once the new one is referenced, so a failure here leaves an
        // orphaned file rather than a profile pointing at something that no longer exists.
        storageService.delete(previousKey);
        profileService.evictProfileCache(user.getUsername());
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfileSettingsResponse deleteAvatar(UUID userId) {
        User user = requireUser(userId);
        String previousKey = user.getAvatarKey();

        user.setAvatarKey(null);
        user.setAvatarUrl(null);
        userRepository.save(user);

        storageService.delete(previousKey);
        profileService.evictProfileCache(user.getUsername());
        return toResponse(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private ProfileSettingsResponse toResponse(User user) {
        return new ProfileSettingsResponse(
                user.getDisplayName(),
                user.getBio(),
                user.getTheme(),
                mediaUrlResolver.resolve(user.getAvatarKey(), user.getAvatarUrl()));
    }

}
