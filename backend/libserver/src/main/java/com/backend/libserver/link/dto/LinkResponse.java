package com.backend.libserver.link.dto;


import com.backend.libserver.link.domain.Link;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code status} is derived server-side so the dashboard does not have to re-implement the
 * visibility rules — and cannot drift from what visitors actually see.
 */
public record LinkResponse(
        UUID id,
        String title,
        String url,
        int position,
        boolean active,
        String thumbnailUrl,
        Instant visibleFrom,
        Instant visibleUntil,
        String status
) {

    public static LinkResponse from(Link link) {
        return from(link, link.getThumbnailUrl());
    }

    /** Overload for callers that resolved a fresh URL from the thumbnail's storage key. */
    public static LinkResponse from(Link link, String thumbnailUrl) {
        return new LinkResponse(
                link.getId(),
                link.getTitle(),
                link.getUrl(),
                link.getPosition(),
                link.isActive(),
                thumbnailUrl,
                link.getVisibleFrom(),
                link.getVisibleUntil(),
                status(link, Instant.now()));
    }

    private static String status(Link link, Instant now) {
        if (!link.isActive()) return "HIDDEN";
        if (link.getVisibleFrom() != null && now.isBefore(link.getVisibleFrom())) return "SCHEDULED";
        if (link.getVisibleUntil() != null && !now.isBefore(link.getVisibleUntil())) return "EXPIRED";
        return "LIVE";
    }
}
