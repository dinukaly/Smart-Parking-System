package com.spms.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Loads the RSA 2048-bit Key Pair from the JKS keystore bundled in resources.
 * - Private Key  → used ONLY by User Service to sign JWTs
 * - Public  Key  → injected into JwtService; also exported as Base64 in startup logs
 *                  so it can be pasted into api-gateway.yml > jwt.public-key
 */
@Configuration
public class KeyStoreConfig {

    @Value("${jwt.keystore.path:classpath:spms-jwt.jks}")
    private String keystorePath;

    @Value("${jwt.keystore.password:spms_store_pass}")
    private String keystorePassword;

    @Value("${jwt.keystore.alias:spms-jwt}")
    private String keystoreAlias;

    @Bean
    public KeyStore keyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream is;

        if (keystorePath.startsWith("classpath:")) {
            String resource = keystorePath.substring("classpath:".length());
            is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalStateException("Keystore not found on classpath: " + resource);
            }
        } else {
            is = java.nio.file.Files.newInputStream(java.nio.file.Path.of(keystorePath));
        }

        ks.load(is, keystorePassword.toCharArray());
        return ks;
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey(KeyStore keyStore) throws Exception {
        PrivateKey key = (PrivateKey) keyStore.getKey(keystoreAlias, keystorePassword.toCharArray());
        if (!(key instanceof RSAPrivateKey rsaKey)) {
            throw new IllegalStateException("Key in keystore is not an RSA private key");
        }
        return rsaKey;
    }

    @Bean
    public RSAPublicKey rsaPublicKey(KeyStore keyStore) throws Exception {
        java.security.cert.Certificate cert = keyStore.getCertificate(keystoreAlias);
        PublicKey key = cert.getPublicKey();
        if (!(key instanceof RSAPublicKey rsaKey)) {
            throw new IllegalStateException("Key in keystore is not an RSA public key");
        }
        return rsaKey;
    }
}
