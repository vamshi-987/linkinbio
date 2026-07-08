package com.backend.libserver.profile.controller;

import com.backend.libserver.profile.dto.PublicProfileResponse;
import com.backend.libserver.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ProfileService profileService;

    @GetMapping("/{username}")
    public PublicProfileResponse getProfile(@PathVariable String username) {
        return profileService.getPublicProfile(username.toLowerCase());
    }
}
