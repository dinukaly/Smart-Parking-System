package com.spms.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Handles all JWT operations for User Service:
 *  - Sign access tokens with the RSA Private Key (RS256)
 *  - Validate tokens using the RSA Public Key
 *  - Extract claims from tokens
 *
 * On startup, logs the Base64-encoded RSA Public Key so it can be
 * copied into api-gateway.yml > jwt.public-key
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;

    @Value("${jwt.access-token-expiry-ms:900000}")     // 15 minutes
    private long accessTokenExpiryMs;

    @Value("${jwt.issuer:spms-user-service}")
    private String issuer;

    /**
     * Log the public key in Base64 DER format on startup.
     */
    public void logPublicKey() {
        String base64PublicKey = Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());
        log.info("=============================================================");
        log.info("RSA Public Key (Base64 DER — configure in api-gateway.yml):");
        log.info(base64PublicKey);
        log.info("=============================================================");
    }

    /**
     * Generate a signed RS256 JWT access token.
     *
     * @param userId  Subject (UUID as string)
     * @param email   User email (stored as a claim)
     * @param role    User role (DRIVER | OWNER | ADMIN)
     * @return Signed JWT string
     */
    public String generateAccessToken(String userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(rsaPrivateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validate the JWT signature and expiry using the RSA Public Key.
     *
     * @param token JWT string
     * @return true if valid
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract the subject (userId) from a token.
     */
    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract the role claim from a token.
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Extract the email claim from a token.
     */
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // Internal helpers

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(rsaPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
