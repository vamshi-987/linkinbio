package com.backend.libserver.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Redis-backed storage for {@link PendingSignup}s. Keys expire on their own, so an abandoned signup
 * leaves nothing behind — neither here nor in the database.
 *
 * <p>The record is stored as a Redis hash of primitive string fields (no JSON library involved).
 * Alongside it: a username → email pointer so login can find a pending signup by the username the
 * user typed, and an hourly counter that caps how many codes one email can be sent.
 */
@Component
public class PendingSignupStore {

    private static final String RECORD_KEY = "signup:pending:";
    private static final String USERNAME_KEY = "signup:uname:";
    private static final String COUNT_KEY = "signup:count:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public PendingSignupStore(StringRedisTemplate redis,
                              @Value("${app.signup.pending-ttl-minutes:30}") long ttlMinutes) {
        this.redis = redis;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Stores the record (and its username pointer), (re)setting the pending window to the full TTL. */
    public void save(PendingSignup pending) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("username", pending.username());
        fields.put("email", pending.email());
        fields.put("passwordHash", pending.passwordHash());
        fields.put("otpHash", pending.otpHash());
        fields.put("otpExpiresAt", Long.toString(pending.otpExpiresAt().toEpochMilli()));
        fields.put("attempts", Integer.toString(pending.attempts()));
        fields.put("issuedAt", Long.toString(pending.issuedAt().toEpochMilli()));

        String recordKey = RECORD_KEY + key(pending.email());
        redis.opsForHash().putAll(recordKey, fields);
        redis.expire(recordKey, ttl);
        redis.opsForValue().set(USERNAME_KEY + key(pending.username()), key(pending.email()), ttl);
    }

    public Optional<PendingSignup> find(String email) {
        Map<Object, Object> raw = redis.opsForHash().entries(RECORD_KEY + key(email));
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PendingSignup(
                field(raw, "username"),
                field(raw, "email"),
                field(raw, "passwordHash"),
                field(raw, "otpHash"),
                Instant.ofEpochMilli(Long.parseLong(field(raw, "otpExpiresAt"))),
                Integer.parseInt(field(raw, "attempts")),
                Instant.ofEpochMilli(Long.parseLong(field(raw, "issuedAt")))));
    }

    public Optional<PendingSignup> findByUsername(String username) {
        String email = redis.opsForValue().get(USERNAME_KEY + key(username));
        return email == null ? Optional.empty() : find(email);
    }

    public void delete(PendingSignup pending) {
        redis.delete(RECORD_KEY + key(pending.email()));
        redis.delete(USERNAME_KEY + key(pending.username()));
    }

    /** Increments this email's hourly code counter, arming the 1-hour expiry on the first send. */
    public long incrementHourlyCount(String email) {
        String countKey = COUNT_KEY + key(email);
        Long count = redis.opsForValue().increment(countKey);
        if (count != null && count == 1L) {
            redis.expire(countKey, Duration.ofHours(1));
        }
        return count == null ? 0 : count;
    }

    public long hourlyCount(String email) {
        String value = redis.opsForValue().get(COUNT_KEY + key(email));
        return value == null ? 0 : Long.parseLong(value);
    }

    private String key(String value) {
        return value.trim().toLowerCase();
    }

    private String field(Map<Object, Object> hash, String name) {
        return String.valueOf(hash.get(name));
    }
}
