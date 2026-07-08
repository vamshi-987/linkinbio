package com.backend.libserver.profile.dto;

import java.util.List;

public record PublicProfileResponse(
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String theme,
        List<PublicLinkResponse> links
) {}
