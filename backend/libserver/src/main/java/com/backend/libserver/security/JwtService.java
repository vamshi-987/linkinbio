package com.backend.libserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(UUID userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    /** Short-lived token issued after OTP verification; only valid for resetting a password. */
    public String generatePasswordResetToken(UUID userId, long expirationMinutes) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000L);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", "password_reset")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    /**
     * Validates a password-reset token and returns the user id, or throws if the token is
     * invalid, expired, or not a password-reset token.
     */
    public UUID extractPasswordResetUserId(String token) {
        Claims claims = extractAllClaims(token);
        if (!"password_reset".equals(claims.get("purpose", String.class))) {
            throw new IllegalArgumentException("Invalid reset token");
        }
        return UUID.fromString(claims.getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            return extractAllClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
