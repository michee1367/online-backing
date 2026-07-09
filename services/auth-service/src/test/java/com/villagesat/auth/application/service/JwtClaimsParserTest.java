package com.villagesat.auth.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtClaimsParser — tests unitaires")
class JwtClaimsParserTest {

    private JwtClaimsParser parser;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        parser = new JwtClaimsParser(new ObjectMapper());
    }

    @Test
    @DisplayName("extractUserId — extrait le subject du JWT")
    void extractUserId_success() {
        String jwt = buildJwt(USER_ID.toString(), "jti-abc", Instant.now().plusSeconds(3600), "user@test.com");

        UUID result = parser.extractUserId(jwt);

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("extractUserId — exception si sub manquant")
    void extractUserId_missingSub() {
        String jwt = buildJwtWithPayload("{\"jti\":\"abc\",\"exp\":9999999999}");

        assertThatThrownBy(() -> parser.extractUserId(jwt))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_INVALID_TOKEN");
    }

    @Test
    @DisplayName("extractJti — retourne le JTI présent")
    void extractJti_present() {
        String jwt = buildJwt(USER_ID.toString(), "my-jti-value", Instant.now().plusSeconds(3600), null);

        Optional<String> jti = parser.extractJti(jwt);

        assertThat(jti).contains("my-jti-value");
    }

    @Test
    @DisplayName("extractJti — retourne Optional.empty si pas de JTI")
    void extractJti_absent() {
        String jwt = buildJwtWithPayload("{\"sub\":\"" + USER_ID + "\",\"exp\":9999999999}");

        Optional<String> jti = parser.extractJti(jwt);

        assertThat(jti).isEmpty();
    }

    @Test
    @DisplayName("remainingTtlSeconds — calcule le TTL restant")
    void remainingTtlSeconds_positive() {
        long futureExp = Instant.now().plusSeconds(600).getEpochSecond();
        String jwt = buildJwtWithPayload("{\"sub\":\"" + USER_ID + "\",\"exp\":" + futureExp + "}");

        long ttl = parser.remainingTtlSeconds(jwt);

        assertThat(ttl).isBetween(598L, 601L);
    }

    @Test
    @DisplayName("remainingTtlSeconds — retourne 0 si expiré")
    void remainingTtlSeconds_expired() {
        long pastExp = Instant.now().minusSeconds(100).getEpochSecond();
        String jwt = buildJwtWithPayload("{\"sub\":\"" + USER_ID + "\",\"exp\":" + pastExp + "}");

        long ttl = parser.remainingTtlSeconds(jwt);

        assertThat(ttl).isZero();
    }

    @Test
    @DisplayName("extractEmail — extrait l'email du claim")
    void extractEmail_success() {
        String jwt = buildJwt(USER_ID.toString(), "jti", Instant.now().plusSeconds(3600), "contact@villagesat.com");

        String email = parser.extractEmail(jwt);

        assertThat(email).isEqualTo("contact@villagesat.com");
    }

    @Test
    @DisplayName("extractEmail — fallback sur preferred_username")
    void extractEmail_fallbackPreferredUsername() {
        String payload = "{\"sub\":\"" + USER_ID + "\",\"preferred_username\":\"fallback@test.com\",\"exp\":9999999999}";
        String jwt = buildJwtWithPayload(payload);

        String email = parser.extractEmail(jwt);

        assertThat(email).isEqualTo("fallback@test.com");
    }

    @Test
    @DisplayName("parsePayload — exception sur token malformé")
    void invalidToken_throwsException() {
        assertThatThrownBy(() -> parser.extractUserId("not-a-jwt"))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_INVALID_TOKEN");
    }

    private String buildJwt(String sub, String jti, Instant exp, String email) {
        StringBuilder payload = new StringBuilder("{\"sub\":\"" + sub + "\",\"jti\":\"" + jti
                + "\",\"exp\":" + exp.getEpochSecond());
        if (email != null) {
            payload.append(",\"email\":\"").append(email).append("\"");
        }
        payload.append("}");
        return buildJwtWithPayload(payload.toString());
    }

    private String buildJwtWithPayload(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".sig";
    }
}
