package com.backend.libserver.link.service.impl;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.dto.CreateLinkRequest;
import com.backend.libserver.link.dto.LinkResponse;
import com.backend.libserver.link.dto.UpdateLinkRequest;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.link.service.LinkService;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.storage.ImageUploadValidator;
import com.backend.libserver.storage.MediaUrlResolver;
import com.backend.libserver.storage.StorageService;
import com.backend.libserver.storage.StoredObject;
import com.backend.libserver.storage.ValidatedImage;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.HashSet;
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
    private final ProfileService profileService;
    private final StorageService storageService;
    private final MediaUrlResolver mediaUrlResolver;
    private final ImageUploadValidator imageUploadValidator;

    public List<LinkResponse> getLinksForUser(UUID userId) {
        return linkRepository.findAllByUserIdOrderByPositionAsc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public LinkResponse create(UUID userId, CreateLinkRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        List<Link> existing = linkRepository.findAllByUserIdOrderByPositionAsc(userId);
        int nextPosition = existing.isEmpty()
                ? 0
                : existing.get(existing.size() - 1).getPosition() + 1;

        Link link = new Link();
        link.setUser(user);
        link.setTitle(req.title());
        link.setUrl(req.url());
        link.setPosition(nextPosition);
        link.setVisibleFrom(req.visibleFrom());
        link.setVisibleUntil(req.visibleUntil());

        Link saved = linkRepository.save(link);
        profileService.evictProfileCache(user.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public LinkResponse update(UUID userId, UUID linkId, UpdateLinkRequest req) {
        Link link = findOwnedLink(userId, linkId);
        link.setTitle(req.title());
        link.setUrl(req.url());
        link.setActive(req.active());
        link.setVisibleFrom(req.visibleFrom());
        link.setVisibleUntil(req.visibleUntil());

        Link saved = linkRepository.save(link);
        profileService.evictProfileCache(link.getUser().getUsername());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID linkId) {
        Link link = findOwnedLink(userId, linkId);
        String username = link.getUser().getUsername();
        String thumbnailKey = link.getThumbnailKey();

        linkRepository.delete(link);
        // Only after the row is gone: deleting the object first would leave a broken image on the
        // page if the delete below failed.
        storageService.delete(thumbnailKey);
        profileService.evictProfileCache(username);
    }

    @Transactional
    public LinkResponse uploadThumbnail(UUID userId, UUID linkId, MultipartFile file) {
        ValidatedImage image = imageUploadValidator.validate(file);
        Link link = findOwnedLink(userId, linkId);

        String previousKey = link.getThumbnailKey();
        StoredObject stored = storageService.store(
                "thumbnails", image.bytes(), image.contentType(), image.extension());

        link.setThumbnailKey(stored.key());
        link.setThumbnailUrl(stored.url());
        Link saved = linkRepository.save(link);

        // The new object is committed to the row before the old one is removed, so a failure here
        // costs an orphan rather than a thumbnail that points at nothing.
        storageService.delete(previousKey);
        profileService.evictProfileCache(link.getUser().getUsername());
        return toResponse(saved);
    }

    @Transactional
    public LinkResponse deleteThumbnail(UUID userId, UUID linkId) {
        Link link = findOwnedLink(userId, linkId);
        String previousKey = link.getThumbnailKey();

        link.setThumbnailKey(null);
        link.setThumbnailUrl(null);
        Link saved = linkRepository.save(link);

        storageService.delete(previousKey);
        profileService.evictProfileCache(link.getUser().getUsername());
        return toResponse(saved);
    }

    @Transactional
    public void reorder(UUID userId, List<UUID> orderedIds) {
        List<Link> links = linkRepository.findAllByUserIdOrderByPositionAsc(userId);
        Map<UUID, Link> byId = links.stream().collect(Collectors.toMap(Link::getId, l -> l));

        // Sizes alone would accept a list that repeats one id and omits another: every link keeps a
        // position, but two share one and the public page order becomes non-deterministic. Comparing
        // the id sets is what actually enforces "a permutation of exactly this user's links".
        if (orderedIds.size() != links.size() || !byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("Ordered list does not match user's links");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setPosition(i);
        }
        linkRepository.saveAll(links);

        if (!links.isEmpty()) {
            profileService.evictProfileCache(links.get(0).getUser().getUsername());
        }
    }

    private LinkResponse toResponse(Link link) {
        return LinkResponse.from(link,
                mediaUrlResolver.resolve(link.getThumbnailKey(), link.getThumbnailUrl()));
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
