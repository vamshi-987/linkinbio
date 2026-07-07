package com.backend.libserver.link.service.impl;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.dto.CreateLinkRequest;
import com.backend.libserver.link.dto.LinkResponse;
import com.backend.libserver.link.dto.UpdateLinkRequest;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.link.service.LinkService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final LinkRepository linkRepository;

    private final UserRepository userRepository;

    @Override
    public List<LinkResponse> getLinksForUser(UUID userId) {
        return linkRepository.findAllByUserIdOrderByPositionAsc(userId)
                .stream().map(LinkResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public LinkResponse create(UUID userId, CreateLinkRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        int nextPosition = linkRepository.findAllByUserIdOrderByPositionAsc(userId).size();

        Link link = new Link();
        link.setUser(user);
        link.setTitle(req.title());
        link.setUrl(req.url());
        link.setPosition(nextPosition);

        return LinkResponse.from(linkRepository.save(link));
    }

    @Transactional
    public LinkResponse update(UUID userId, UUID linkId, UpdateLinkRequest req) {
        Link link = findOwnedLink(userId, linkId);
        link.setTitle(req.title());
        link.setUrl(req.url());
        link.setActive(req.active());
        return LinkResponse.from(linkRepository.save(link));
    }

    @Transactional
    public void delete(UUID userId, UUID linkId) {
        Link link = findOwnedLink(userId, linkId);
        linkRepository.delete(link);
    }

    @Transactional
    public void reorder(UUID userId, List<UUID> orderedIds) {
        List<Link> links = linkRepository.findAllByUserIdOrderByPositionAsc(userId);
        Map<UUID, Link> byId = links.stream().collect(Collectors.toMap(Link::getId, l -> l));

        if (byId.size() != orderedIds.size()) {
            throw new IllegalArgumentException("Ordered list does not match user's links");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            Link link = byId.get(orderedIds.get(i));
            if (link == null) {
                throw new AccessDeniedException("Link does not belong to this user");
            }
            link.setPosition(i);
        }
        linkRepository.saveAll(links);
    }

    private Link findOwnedLink(UUID userId, UUID linkId) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NoSuchElementException("Link not found"));

        if (!link.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this link");
        }
        return link;
    }

}
