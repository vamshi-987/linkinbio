package com.backend.libserver.profile.controller;

import com.backend.libserver.profile.dto.UpdateProfileRequest;
import com.backend.libserver.profile.service.ProfileSettingsService;
import com.backend.libserver.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileSettingsController {

    private final ProfileSettingsService profileSettingsService;

    @PatchMapping
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal UserPrincipal user,
                                              @Valid @RequestBody UpdateProfileRequest req) {
        profileSettingsService.updateProfile(user.getId(), req);
        return ResponseEntity.noContent().build();
    }
}