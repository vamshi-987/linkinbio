package com.backend.libserver.profile.dto;

import java.util.UUID;

public record PublicLinkResponse(
        UUID id, String title, String url, String thumbnailUrl) {}
