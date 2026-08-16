package com.spms.paymentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;

/**
 * Zero-Trust JWT Token Validator for Payment Service.
 * Validates RS256 RSA signed JWT tokens using the bundled RSA public certificate
 * without making remote network calls to the User Service
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final ResourceLoader resourceLoader;

    @Value("${jwt.public-key-path:classpath:spms-jwt-public.pem}")
    private String publicKeyPath;

    @Value("${jwt.issuer:spms-user-service}")
    private String expectedIssuer;

    private RSAPublicKey rsaPublicKey;

    public JwtTokenValidator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource(publicKeyPath);
            try (InputStream is = resource.getInputStream()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate cert = cf.generateCertificate(is);
                this.rsaPublicKey = (RSAPublicKey) cert.getPublicKey();
                log.info("Successfully loaded RSA Public Key for JWT validation from {}", publicKeyPath);
            }
        } catch (Exception e) {
            log.error("Failed to load RSA public key from {}: {}", publicKeyPath, e.getMessage(), e);
            throw new IllegalStateException("Could not initialize JWT RSA Public Key", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(rsaPublicKey)
                .requireIssuer(expectedIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
