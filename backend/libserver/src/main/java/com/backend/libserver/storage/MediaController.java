package com.backend.libserver.storage;

import com.backend.libserver.storage.impl.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * Serves files held by the local storage provider. With {@code app.storage.provider=s3} this
 * controller is not registered at all — objects are fetched straight from the bucket or CDN.
 */
@RestController
@RequestMapping("/api/public/media")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class MediaController {

    /** Exactly the shape this app generates: a known folder plus a UUID filename. */
    private static final Pattern KEY = Pattern.compile(
            "(avatars|thumbnails)/[0-9a-f-]{36}\\.(png|jpg)");

    private final LocalStorageService storage;

    @GetMapping("/{prefix}/{filename}")
    public ResponseEntity<byte[]> get(@PathVariable String prefix, @PathVariable String filename) {
        String key = prefix + "/" + filename;
        // Reject anything that is not a key we could have issued before touching the filesystem.
        if (!KEY.matcher(key).matches()) {
            throw new NoSuchElementException("File not found");
        }

        byte[] bytes = storage.read(key).orElseThrow(() -> new NoSuchElementException("File not found"));
        MediaType type = filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(type)
                // Keys are unique per upload and never rewritten, so the bytes at a URL can never
                // change — safe to cache immutably.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(bytes);
    }
}
