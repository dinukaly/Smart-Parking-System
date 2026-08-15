package com.spms.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Manages refresh tokens backed by Redis.
 *
 * Refresh token format: 48-byte cryptographically random value, Base64URL-encoded.
 * Stored in Redis as:  KEY="refresh:{token}"  VALUE="{userId}"  TTL=7 days
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")  // 7 days
    private long refreshTokenExpiryMs;

    /**
     * Create and persist a new refresh token for the given userId.
     *
     * @return The generated refresh token string
     */
    public String createRefreshToken(String userId) {
        String token = generateToken();
        Duration ttl = Duration.ofMillis(refreshTokenExpiryMs);
        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId, ttl);
        return token;
    }

    /**
     * Validate the refresh token and return the associated userId.
     *
     * @return userId if valid, null if expired / not found
     */
    public String validateAndGetUserId(String token) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + token);
    }

    /**
     * Invalidate a refresh token (logout / rotation).
     */
    public void invalidate(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }

    /**
     * Rotate: invalidate the old token and issue a new one atomically.
     */
    public String rotate(String oldToken, String userId) {
        invalidate(oldToken);
        return createRefreshToken(userId);
    }

    // Internal helpers

    private String generateToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
