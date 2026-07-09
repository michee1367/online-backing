package com.villagesat.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class WalletJwtConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${villagesat.keycloak.jwk-set-uri:http://127.0.0.1:8180/realms/villagesat/protocol/openid-connect/certs}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
