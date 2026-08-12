package com.spms.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Spring Security WebFlux Configuration for API Gateway.
 *
 * Configures perimeter authentication using Spring Security OAuth2 Resource Server.
 * Validates RSA-signed JWT tokens using the RSA Public Key.
 * Whitelists public paths (e.g. /api/users/login, /api/users/register, /api/users/refresh).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${gateway.public-paths:/api/users/register,/api/users/login,/api/users/refresh}")
    private List<String> publicPaths;

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

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
//        if (publicKeyPem == null || publicKeyPem.isBlank() || publicKeyPem.contains("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz8g...")) {
//            // Fallback for development before User Service generates/provisions the actual RSA key pair
//            return token -> {
//                throw new IllegalStateException("RSA Public Key not initialized in configuration");
//            };
//        }
        try {
            RSAPublicKey key = parsePublicKey(publicKeyPem);
            return NimbusReactiveJwtDecoder.withPublicKey(key).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA Public Key for Gateway Security", e);
        }
    }

    private RSAPublicKey parsePublicKey(String pem) throws Exception {
        String cleanPem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
