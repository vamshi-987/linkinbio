package com.backend.libserver.profile.service.impl;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.profile.dto.PublicLinkResponse;
import com.backend.libserver.profile.dto.PublicProfileResponse;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.storage.MediaUrlResolver;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final MediaUrlResolver mediaUrlResolver;

    @Cacheable(value = "publicProfiles", key = "#username")
    public PublicProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));

        // Scheduled and expired links are excluded here, and LinkVisibilityScheduler evicts this
        // cache entry as each boundary passes so a cached copy cannot outlive the window.
        List<Link> links = linkRepository.findVisibleLinksByUsername(username, Instant.now());

        List<PublicLinkResponse> linkResponses = links.stream()
                .map(l -> new PublicLinkResponse(l.getId(), l.getTitle(), l.getUrl(),
                        mediaUrlResolver.resolve(l.getThumbnailKey(), l.getThumbnailUrl())))
                .collect(Collectors.toList());

        return new PublicProfileResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                // Resolved from the storage key so a presigned URL is minted fresh; the cache TTL
                // is far shorter than the signature's lifetime, so a cached entry cannot go stale.
                mediaUrlResolver.resolve(user.getAvatarKey(), user.getAvatarUrl()),
                user.getTheme(),
                linkResponses
        );
    }

    @CacheEvict(value = "publicProfiles", key = "#username")
    public void evictProfileCache(String username) {
        // annotation handles eviction — body intentionally empty
    }
}
