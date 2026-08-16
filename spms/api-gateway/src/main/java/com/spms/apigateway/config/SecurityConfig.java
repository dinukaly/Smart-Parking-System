package com.spms.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Spring Security WebFlux Configuration for API Gateway.
 *
 * Configures perimeter authentication using Spring Security OAuth2 Resource Server.
 * Validates RSA-signed JWT tokens using the RSA Public Key / Certificate.
 * Whitelists public paths (e.g. /api/users/login, /api/users/register, /api/users/refresh).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${gateway.public-paths:/api/users/register,/api/users/login,/api/users/refresh,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html}")
    private List<String> publicPaths;

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    @Value("${jwt.public-key-path:classpath:spms-jwt-public.pem}")
    private String publicKeyPath;

    public SecurityConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicPaths.toArray(new String[0])).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        try {
            RSAPublicKey key = resolvePublicKey();
            log.info("Successfully configured ReactiveJwtDecoder for API Gateway with RSA Public Key");
            return NimbusReactiveJwtDecoder.withPublicKey(key).build();
        } catch (Exception e) {
            log.error("Failed to initialize RSA Public Key for Gateway Security: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse RSA Public Key for Gateway Security", e);
        }
    }

    private RSAPublicKey resolvePublicKey() throws Exception {
        if (publicKeyPem != null && !publicKeyPem.isBlank() && !publicKeyPem.contains("...")) {
            return parsePublicKeyFromString(publicKeyPem);
        }

        Resource resource = resourceLoader.getResource(publicKeyPath);
        if (!resource.exists()) {
            throw new IllegalStateException("RSA Public Key certificate resource not found at: " + publicKeyPath);
        }
        try (InputStream is = resource.getInputStream()) {
            return parsePublicKeyFromStream(is);
        }
    }

    private RSAPublicKey parsePublicKeyFromStream(InputStream is) throws Exception {
        byte[] bytes = is.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        return parsePublicKeyFromString(content);
    }

    private RSAPublicKey parsePublicKeyFromString(String pem) throws Exception {
        if (pem.contains("-----BEGIN CERTIFICATE-----")) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
            return (RSAPublicKey) cert.getPublicKey();
        } else if (pem.contains("-----BEGIN PUBLIC KEY-----")) {
            String cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } else {
            String cleanPem = pem.replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        }
    }
}
