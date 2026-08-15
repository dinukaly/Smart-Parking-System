package com.spms.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey = (RSAPublicKey) kp.getPublic();

        jwtService = new JwtService(privateKey, publicKey);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", 900000L); // 15 mins
        ReflectionTestUtils.setField(jwtService, "issuer", "spms-user-service");
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String userId = UUID.randomUUID().toString();
        String email = "test@example.com";
        String role = "DRIVER";

        String token = jwtService.generateAccessToken(userId, email, role);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(role, jwtService.extractRole(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        KeyPairGenerator kpg2 = KeyPairGenerator.getInstance("RSA");
        kpg2.initialize(2048);
        KeyPair kp2 = kpg2.generateKeyPair();

        JwtService otherJwtService = new JwtService((RSAPrivateKey) kp2.getPrivate(), (RSAPublicKey) kp2.getPublic());
        ReflectionTestUtils.setField(otherJwtService, "accessTokenExpiryMs", 900000L);
        ReflectionTestUtils.setField(otherJwtService, "issuer", "spms-user-service");

        String tokenFromOther = otherJwtService.generateAccessToken(UUID.randomUUID().toString(), "other@example.com", "OWNER");

        // Valid on other service, but invalid on this jwtService due to different RSA key
        assertTrue(otherJwtService.isTokenValid(tokenFromOther));
        assertFalse(jwtService.isTokenValid(tokenFromOther));
    }

    @Test
    void shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs", -1000L); // expired 1s ago

        String token = jwtService.generateAccessToken(UUID.randomUUID().toString(), "expired@example.com", "DRIVER");

        assertNotNull(token);
        assertFalse(jwtService.isTokenValid(token));
    }
}
