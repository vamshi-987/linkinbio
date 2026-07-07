package com.backend.libserver.link.dto;


import com.backend.libserver.link.domain.Link;

import java.util.UUID;

public record LinkResponse(UUID id, String title, String url, int position, boolean active) {

    public static LinkResponse from(Link link) {
        return new LinkResponse(link.getId(), link.getTitle(), link.getUrl(), link.getPosition(), link.isActive());
    }
}
