package com.backend.libserver.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-IP request limits. {@code /api/auth/} is capped harder than public profile traffic because
 * those endpoints send email and accept one-time codes: unlimited calls mean a flooded inbox and a
 * brute-forceable 6-digit code. This only bounds a single IP — the per-user limits in OtpService are
 * what hold up against an attacker rotating addresses.
 *
 * <p>Depends on {@code server.forward-headers-strategy}: behind Railway's proxy, without it every
 * request appears to come from the proxy and all users would share one bucket.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String PUBLIC_PREFIX = "/api/public/";
    private static final String AUTH_PREFIX = "/api/auth/";
    /**
     * Assets a single profile view fans out into: one avatar plus a thumbnail per link, and the QR
     * image. They share the visitor's IP with the profile fetch and the click redirect, so counting
     * them against the same 20/min bucket lets a page with a dozen images lock a real visitor out of
     * their own clicks on the second refresh. They are cheap, cacheable reads, so they get their own
     * far larger allowance instead of an exemption.
     */
    private static final String[] ASSET_PREFIXES = {"/api/public/media/", "/api/public/qr/"};

    private static final int ASSET_PER_MINUTE = 300;
    private static final int PUBLIC_PER_MINUTE = 20;
    /** Room for a few login typos and a resend, not for a scripted loop. */
    private static final int AUTH_PER_MINUTE = 10;

    /**
     * Idle buckets are dropped so the map cannot grow one permanent entry per IP seen. Every bucket
     * refills completely within a minute, so an entry untouched for longer carries no state a fresh
     * bucket would not give back anyway — evicting it can never hand out extra allowance.
     */
    private static final Duration IDLE_TTL = Duration.ofMinutes(5);
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);
    private static final int SWEEP_THRESHOLD = 10_000;

    private record Entry(Bucket bucket, AtomicLong lastSeenNanos) {}

    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastSweepNanos = new AtomicLong(System.nanoTime());

    private Bucket newBucket(int perMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static int limitFor(String uri) {
        for (String assetPrefix : ASSET_PREFIXES) {
            if (uri.startsWith(assetPrefix)) return ASSET_PER_MINUTE;
        }
        if (uri.startsWith(PUBLIC_PREFIX)) return PUBLIC_PER_MINUTE;
        if (uri.startsWith(AUTH_PREFIX)) return AUTH_PER_MINUTE;
        return 0;
    }

    /**
     * Drops entries idle past the TTL, at most once per {@link #SWEEP_INTERVAL} and only once the map
     * is big enough to be worth a full scan — otherwise a steady load of distinct IPs would pay for
     * an O(n) walk on every single request. The CAS means one request does the work and the rest go
     * straight through.
     */
    private void sweepIdle(long nowNanos) {
        if (buckets.size() < SWEEP_THRESHOLD) return;

        long lastSweep = lastSweepNanos.get();
        if (nowNanos - lastSweep < SWEEP_INTERVAL.toNanos()) return;
        if (!lastSweepNanos.compareAndSet(lastSweep, nowNanos)) return;

        long cutoff = nowNanos - IDLE_TTL.toNanos();
        buckets.values().removeIf(entry -> entry.lastSeenNanos().get() - cutoff < 0);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        int perMinute = limitFor(uri);

        if (perMinute > 0) {
            long now = System.nanoTime();
            sweepIdle(now);

            // Scoped by prefix so exhausting one limit cannot lock a user out of the other.
            String key = perMinute + "|" + request.getRemoteAddr();
            int capacity = perMinute;
            Entry entry = buckets.computeIfAbsent(key, k -> new Entry(newBucket(capacity), new AtomicLong(now)));
            entry.lastSeenNanos().set(now);

            if (!entry.bucket().tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many requests, please slow down");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
