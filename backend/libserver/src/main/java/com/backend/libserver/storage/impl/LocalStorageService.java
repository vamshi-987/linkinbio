package com.backend.libserver.storage.impl;

import com.backend.libserver.storage.StorageService;
import com.backend.libserver.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Disk-backed storage, and the default provider.
 *
 * <p>Its purpose is that the project runs end to end — uploads included — with nothing but Postgres
 * and Redis. Files live under a configured directory and are served back by {@code MediaController}.
 * On a platform with an ephemeral filesystem this loses uploads on redeploy, which is exactly why
 * {@code app.storage.provider=s3} exists for production.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalStorageService(@Value("${app.storage.local.dir:uploads}") String dir,
                               @Value("${app.base-url:http://localhost:8081}") String baseUrl) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.publicBaseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public StoredObject store(String prefix, byte[] content, String contentType, String extension) {
        // A random name, never the client's filename: uploads then cannot collide, overwrite each
        // other, or smuggle a path out of the directory.
        String key = prefix + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store upload", ex);
        }
        return new StoredObject(key, urlFor(key));
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("Could not delete stored object '{}': {}", key, ex.getMessage());
        }
    }

    @Override
    public String urlFor(String key) {
        return publicBaseUrl + "/api/public/media/" + key;
    }

    @Override
    public String providerName() {
        return "local";
    }

    /** Reads bytes back for {@code MediaController}; empty when the key points outside the root. */
    public Optional<byte[]> read(String key) {
        try {
            Path path = resolve(key);
            if (!Files.isRegularFile(path)) return Optional.empty();
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Resolves a key under the storage root, refusing anything that escapes it. Keys are generated
     * here, but this method is also reached from a public URL, so it re-checks rather than assuming.
     */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
