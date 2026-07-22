package com.backend.libserver.storage;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns a stored object key into a URL at read time.
 *
 * <p>The URL is not treated as the source of truth because it is not stable: a presigned link
 * expires, and a bucket or CDN hostname can change. The key is. The URL saved alongside it is kept
 * only as a fallback — it also covers avatars set to an external address rather than uploaded.
 */
@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(MediaUrlResolver.class);

    private final StorageService storageService;

    public String resolve(String key, String storedUrl) {
        if (key == null || key.isBlank()) return storedUrl;
        try {
            return storageService.urlFor(key);
        } catch (RuntimeException ex) {
            log.warn("Could not resolve URL for object '{}': {}", key, ex.getMessage());
            return storedUrl;
        }
    }
}
