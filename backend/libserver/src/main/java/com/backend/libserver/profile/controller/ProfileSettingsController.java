package com.backend.libserver.profile.controller;

import com.backend.libserver.profile.dto.ProfileSettingsResponse;
import com.backend.libserver.profile.dto.UpdateProfileRequest;
import com.backend.libserver.profile.service.ProfileSettingsService;
import com.backend.libserver.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileSettingsController {

    private final ProfileSettingsService profileSettingsService;

    @GetMapping
    public ResponseEntity<ProfileSettingsResponse> getProfile(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(profileSettingsService.getProfile(user.getId()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal UserPrincipal user,
                                              @Valid @RequestBody UpdateProfileRequest req) {
        profileSettingsService.updateProfile(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileSettingsResponse> uploadAvatar(@AuthenticationPrincipal UserPrincipal user,
                                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileSettingsService.uploadAvatar(user.getId(), file));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<ProfileSettingsResponse> deleteAvatar(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(profileSettingsService.deleteAvatar(user.getId()));
    }
}