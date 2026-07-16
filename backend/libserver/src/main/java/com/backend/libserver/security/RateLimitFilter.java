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

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket(int perMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        int perMinute = 0;
        if (uri.startsWith(PUBLIC_PREFIX)) {
            perMinute = 20;
        } else if (uri.startsWith(AUTH_PREFIX)) {
            // Room for a few login typos and a resend, not for a scripted loop.
            perMinute = 10;
        }

        if (perMinute > 0) {
            // Scoped by prefix so exhausting one limit cannot lock a user out of the other.
            String key = perMinute + "|" + request.getRemoteAddr();
            int capacity = perMinute;
            Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(capacity));

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many requests, please slow down");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
