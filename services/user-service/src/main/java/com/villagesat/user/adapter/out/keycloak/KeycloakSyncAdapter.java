package com.villagesat.user.adapter.out.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.villagesat.user.config.KeycloakProperties;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.model.UserProfile;
import com.villagesat.user.domain.port.out.KeycloakSyncPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KeycloakSyncAdapter implements KeycloakSyncPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakSyncAdapter.class);

    private final WebClient webClient;
    private final KeycloakProperties properties;
    private volatile String cachedAdminToken;
    private volatile long tokenExpiresAt;

    public KeycloakSyncAdapter(WebClient.Builder builder, KeycloakProperties properties) {
        this.webClient = builder.baseUrl(properties.serverUrl()).build();
        this.properties = properties;
    }

    @Override
    public void syncUserProfile(UUID keycloakUserId, User user, UserProfile profile) {
        try {
            Map<String, Object> body = Map.of(
                    "firstName", user.firstName(),
                    "lastName", user.lastName(),
                    "email", user.email(),
                    "emailVerified", true,
                    "attributes", Map.of(
                            "phone", List.of(nullToEmpty(user.phone())),
                            "countryCode", List.of(nullToEmpty(user.countryCode())),
                            "preferredLanguage", List.of(nullToEmpty(profile.preferredLanguage())),
                            "timezone", List.of(nullToEmpty(profile.timezone())),
                            "kyc_level", List.of(String.valueOf(user.kycLevel()))
                    )
            );
            updateUser(keycloakUserId, body);
        } catch (Exception e) {
            log.error("Keycloak profile sync failed for user {}", keycloakUserId, e);
        }
    }

    @Override
    public void updateKycLevel(UUID keycloakUserId, int kycLevel) {
        try {
            updateUser(keycloakUserId, Map.of(
                    "attributes", Map.of("kyc_level", List.of(String.valueOf(kycLevel)))
            ));
        } catch (Exception e) {
            log.error("Keycloak KYC sync failed for user {}", keycloakUserId, e);
        }
    }

    @Override
    public void updateStatus(UUID keycloakUserId, String status) {
        try {
            boolean enabled = !"SUSPENDED".equals(status) && !"CLOSED".equals(status) && !"FROZEN".equals(status);
            updateUser(keycloakUserId, Map.of(
                    "enabled", enabled,
                    "attributes", Map.of("account_status", List.of(status))
            ));
        } catch (Exception e) {
            log.error("Keycloak status sync failed for user {}", keycloakUserId, e);
        }
    }

    private void updateUser(UUID keycloakUserId, Map<String, Object> body) {
        webClient.put()
                .uri(properties.adminUserEndpoint(keycloakUserId.toString()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String adminToken() {
        if (cachedAdminToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedAdminToken;
        }
        TokenResponse response = webClient.post()
                .uri(properties.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", properties.adminClientId())
                        .with("client_secret", properties.adminClientSecret()))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Unable to obtain Keycloak admin token");
        }
        cachedAdminToken = response.accessToken();
        tokenExpiresAt = System.currentTimeMillis() + (response.expiresIn() - 30) * 1000L;
        return cachedAdminToken;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {}
}
