package com.spms.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        refreshTokenService = new RefreshTokenService(redisTemplate);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiryMs", 604800000L);
    }

    @Test
    void shouldCreateRefreshToken() {
        String userId = "123e4567-e89b-12d3-a456-426614174000";

        String token = refreshTokenService.createRefreshToken(userId);

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(valueOperations, times(1)).set(eq("refresh:" + token), eq(userId), any(Duration.class));
    }

    @Test
    void shouldValidateAndGetUserId() {
        String token = "valid-refresh-token";
        String expectedUserId = "123e4567-e89b-12d3-a456-426614174000";

        when(valueOperations.get("refresh:" + token)).thenReturn(expectedUserId);

        String actualUserId = refreshTokenService.validateAndGetUserId(token);

        assertEquals(expectedUserId, actualUserId);
        verify(valueOperations, times(1)).get("refresh:" + token);
    }

    @Test
    void shouldReturnNullForInvalidOrExpiredToken() {
        String token = "invalid-or-expired-token";

        when(valueOperations.get("refresh:" + token)).thenReturn(null);

        String actualUserId = refreshTokenService.validateAndGetUserId(token);

        assertNull(actualUserId);
    }

    @Test
    void shouldInvalidateToken() {
        String token = "token-to-delete";

        refreshTokenService.invalidate(token);

        verify(redisTemplate, times(1)).delete("refresh:" + token);
    }

    @Test
    void shouldRotateToken() {
        String oldToken = "old-token";
        String userId = "123e4567-e89b-12d3-a456-426614174000";

        String newToken = refreshTokenService.rotate(oldToken, userId);

        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        verify(redisTemplate, times(1)).delete("refresh:" + oldToken);
        verify(valueOperations, times(1)).set(eq("refresh:" + newToken), eq(userId), any(Duration.class));
    }
}
