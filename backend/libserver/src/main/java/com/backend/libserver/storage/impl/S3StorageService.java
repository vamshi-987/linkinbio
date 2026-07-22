package com.backend.libserver.storage.impl;

import com.backend.libserver.storage.StorageService;
import com.backend.libserver.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * S3-compatible object storage — AWS S3, MinIO, Cloudflare R2, anything speaking the same API.
 *
 * <p>Only created when {@code app.storage.provider=s3}, so a deployment without credentials starts
 * on the local provider rather than failing at boot.
 *
 * <p>Two URL modes. A bucket fronted by a CDN or public read policy gets a plain, permanently
 * cacheable URL. A private bucket instead gets a presigned GET: the object stays inaccessible
 * without a signature, and the link expires. Presigned URLs are minted on read rather than stored,
 * because a URL saved in the database would outlive its own signature.
 */
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final Duration presignTtl;

    public S3StorageService(S3Client s3,
                            S3Presigner presigner,
                            @Value("${app.storage.s3.bucket}") String bucket,
                            @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl,
                            @Value("${app.storage.s3.presign-minutes:60}") long presignMinutes) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.presignTtl = Duration.ofMinutes(presignMinutes);
    }

    @Override
    public StoredObject store(String prefix, byte[] content, String contentType, String extension) {
        String key = prefix + "/" + UUID.randomUUID() + "." + extension;

        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        // Immutable keys, so anything downstream may cache the object indefinitely.
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(content));

        return new StoredObject(key, urlFor(key));
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ex) {
            // The row referencing it is already gone; an orphaned object is cheaper than a failed
            // request, and lifecycle rules can sweep it up.
            log.warn("Could not delete S3 object '{}': {}", key, ex.getMessage());
        }
    }

    @Override
    public String urlFor(String key) {
        if (!publicBaseUrl.isEmpty()) {
            return publicBaseUrl + "/" + key;
        }
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    @Override
    public String providerName() {
        return "s3";
    }
}
