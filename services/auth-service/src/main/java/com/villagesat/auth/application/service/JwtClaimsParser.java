package com.villagesat.auth.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtClaimsParser {

    private final ObjectMapper objectMapper;

    public JwtClaimsParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UUID extractUserId(String accessToken) {
        JsonNode payload = parsePayload(accessToken);
        String sub = payload.path("sub").asText(null);
        if (sub == null) {
            throw new AuthService.AuthException("AUTH_INVALID_TOKEN", "Token JWT invalide");
        }
        return UUID.fromString(sub);
    }

    public Optional<String> extractJti(String accessToken) {
        JsonNode payload = parsePayload(accessToken);
        String jti = payload.path("jti").asText(null);
        return Optional.ofNullable(jti);
    }

    public long remainingTtlSeconds(String accessToken) {
        JsonNode payload = parsePayload(accessToken);
        long exp = payload.path("exp").asLong(0);
        long now = System.currentTimeMillis() / 1000;
        return Math.max(0, exp - now);
    }

    public String extractEmail(String accessToken) {
        JsonNode payload = parsePayload(accessToken);
        String email = payload.path("email").asText(null);
        if (email == null) {
            email = payload.path("preferred_username").asText(null);
        }
        return email;
    }

    private JsonNode parsePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT");
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readTree(decoded);
        } catch (Exception e) {
            throw new AuthService.AuthException("AUTH_INVALID_TOKEN", "Impossible de parser le JWT");
        }
    }
}
