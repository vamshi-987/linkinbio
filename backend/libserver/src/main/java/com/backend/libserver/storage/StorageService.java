package com.backend.libserver.storage;

/**
 * Blob storage for user-uploaded images.
 *
 * <p>Two implementations ship: local disk (the default, so a clone runs with no cloud account) and
 * S3-compatible object storage (AWS S3, MinIO, R2). Callers only ever hold the returned key — the
 * URL is resolved through {@link #urlFor(String)}, so a bucket can be swapped, or a CDN put in
 * front, without rewriting stored rows.
 */
public interface StorageService {

    /**
     * @param prefix logical folder, e.g. {@code avatars}
     * @param extension file extension without the dot, e.g. {@code png}
     */
    StoredObject store(String prefix, byte[] content, String contentType, String extension);

    /** Best-effort: a missing object is not an error, since the row it belonged to is already gone. */
    void delete(String key);

    String urlFor(String key);

    /** Reported on the upload response so the client can tell which backend served it. */
    String providerName();
}
