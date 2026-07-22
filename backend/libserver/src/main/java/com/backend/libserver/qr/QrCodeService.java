package com.backend.libserver.qr;

/** Renders a URL as a PNG QR code, memoised in Redis. */
public interface QrCodeService {

    int MIN_SIZE = 128;
    int MAX_SIZE = 1024;
    int DEFAULT_SIZE = 512;

    /**
     * @param cacheKey stable identity of what is being encoded (not the URL itself, so a change of
     *                 domain does not silently serve stale codes under the same key)
     * @param content  the URL to encode
     * @param size     edge length in pixels, clamped to [{@value #MIN_SIZE}, {@value #MAX_SIZE}]
     */
    byte[] pngFor(String cacheKey, String content, int size);
}
