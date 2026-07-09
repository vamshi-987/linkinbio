package com.backend.libserver.profile.service.impl;

import com.backend.libserver.link.domain.Link;
import com.backend.libserver.link.repository.LinkRepository;
import com.backend.libserver.profile.dto.PublicLinkResponse;
import com.backend.libserver.profile.dto.PublicProfileResponse;
import com.backend.libserver.profile.service.ProfileService;
import com.backend.libserver.user.domain.User;
import com.backend.libserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    @Cacheable(value = "publicProfiles", key = "#username")
    public PublicProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));

        List<Link> links = linkRepository.findActiveLinksByUsername(username);

        List<PublicLinkResponse> linkResponses = links.stream()
                .map(l -> new PublicLinkResponse(l.getId(), l.getTitle(), l.getUrl()))
                .collect(Collectors.toList());

        return new PublicProfileResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getTheme(),
                linkResponses
        );
    }

    @CacheEvict(value = "publicProfiles", key = "#username")
    public void evictProfileCache(String username) {
        // annotation handles eviction — body intentionally empty
    }
}
