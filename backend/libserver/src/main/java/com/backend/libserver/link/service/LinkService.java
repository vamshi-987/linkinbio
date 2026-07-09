package com.backend.libserver.link.service;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.dto.CreateLinkRequest;
import com.backend.libserver.link.dto.LinkResponse;
import com.backend.libserver.link.dto.UpdateLinkRequest;

import java.util.List;
import java.util.UUID;

public interface LinkService {

    List<LinkResponse> getLinksForUser(UUID userId);

    LinkResponse create(UUID userId, CreateLinkRequest req);

    LinkResponse update(UUID userId, UUID linkId, UpdateLinkRequest req);

    void delete(UUID userId, UUID linkId);

    void reorder(UUID userId, List<UUID> orderedIds);

}
