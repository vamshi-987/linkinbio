package com.backend.libserver.qr;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Binary side-cache for rendered QR PNGs.
 *
 * <p>Deliberately not Spring's {@code @Cacheable}: the shared cache manager serialises values as
 * JSON, which would base64 every image and cost a third more memory plus an encode/decode on each
 * hit. A dedicated template stores the bytes as-is.
 *
 * <p>Every operation degrades to a miss if Redis is unavailable — a QR code can always be recomputed,
 * and an unreachable cache must not turn into a failed request.
 */
@Component
@RequiredArgsConstructor
public class QrCodeCache {

    private static final Logger log = LoggerFactory.getLogger(QrCodeCache.class);
    private static final String KEY_PREFIX = "qr:";

    private final RedisTemplate<String, byte[]> qrRedisTemplate;

    @Value("${app.qr.cache-ttl-hours:24}")
    private long ttlHours;

    public byte[] get(String key) {
        try {
            return qrRedisTemplate.opsForValue().get(KEY_PREFIX + key);
        } catch (RuntimeException ex) {
            log.warn("QR cache read failed for '{}': {}", key, ex.getMessage());
            return null;
        }
    }

    public void put(String key, byte[] png) {
        try {
            qrRedisTemplate.opsForValue().set(KEY_PREFIX + key, png, Duration.ofHours(ttlHours));
        } catch (RuntimeException ex) {
            log.warn("QR cache write failed for '{}': {}", key, ex.getMessage());
        }
    }
}
