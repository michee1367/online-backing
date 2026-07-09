package com.villagesat.auth.adapter.out.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.villagesat.auth.application.service.AuthService;
import com.villagesat.auth.config.KeycloakProperties;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.port.in.AuthUseCase;
import com.villagesat.auth.domain.port.out.IdentityProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProviderAdapter.class);

    private final WebClient webClient;
    private final KeycloakProperties properties;

    public KeycloakIdentityProviderAdapter(WebClient.Builder builder, KeycloakProperties properties) {
        this.webClient = builder.baseUrl(properties.serverUrl()).build();
        this.properties = properties;
    }

    @Override
    public UUID registerUser(AuthUseCase.RegisterCommand command) {
        String adminToken = obtainAdminToken();

        Map<String, Object> userBody = Map.of(
                "username", command.email(),
                "email", command.email(),
                "firstName", command.firstName(),
                "lastName", command.lastName(),
                "enabled", true,
                "emailVerified", true,
                "attributes", Map.of(
                        "phone", List.of(command.phone()),
                        "countryCode", List.of(command.countryCode()),
                        "mfa_verified", List.of("false")
                ),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", command.password(),
                        "temporary", false
                )),
                "requiredActions", List.of()
        );

        var response = webClient.post()
                .uri(properties.adminUsersEndpoint())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userBody)
                .retrieve()
                .toBodilessEntity()
                .block();

        String location = response != null && response.getHeaders().getLocation() != null
                ? response.getHeaders().getLocation().toString() : null;
        if (location == null) {
            throw new AuthService.AuthException("AUTH_REGISTRATION_FAILED", "Échec création utilisateur Keycloak");
        }
        String userId = location.substring(location.lastIndexOf('/') + 1);

        finalizeUserAccount(UUID.fromString(userId), adminToken);
        return UUID.fromString(userId);
    }

    @Override
    public AuthTokens authenticate(String email, String password) {
        return requestToken(Map.of(
                "grant_type", "password",
                "client_id", properties.clientId(),
                "username", email,
                "password", password,
                "scope", "openid profile email"
        ));
    }

    @Override
    public AuthTokens refreshToken(String refreshToken) {
        return requestToken(Map.of(
                "grant_type", "refresh_token",
                "client_id", properties.clientId(),
                "refresh_token", refreshToken
        ));
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        try {
            webClient.post()
                    .uri(properties.logoutEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", properties.clientId())
                            .with("refresh_token", refreshToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Keycloak logout failed: {}", e.getMessage());
        }
    }

    @Override
    public void setUserAttribute(UUID userId, String attribute, String value) {
        String adminToken = obtainAdminToken();
        KeycloakUserRepresentation user = webClient.get()
                .uri(properties.adminUserEndpoint(userId.toString()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(KeycloakUserRepresentation.class)
                .block();

        if (user == null) {
            throw new AuthService.AuthException("AUTH_USER_NOT_FOUND", "Utilisateur introuvable");
        }

        var attributes = user.attributes() != null
                ? new java.util.HashMap<>(user.attributes())
                : new java.util.HashMap<String, List<String>>();
        attributes.put(attribute, List.of(value));

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("username", user.username());
        if (user.email() != null) body.put("email", user.email());
        if (user.firstName() != null) body.put("firstName", user.firstName());
        if (user.lastName() != null) body.put("lastName", user.lastName());
        body.put("enabled", user.enabled() != null ? user.enabled() : true);
        body.put("emailVerified", user.emailVerified() != null ? user.emailVerified() : true);
        body.put("attributes", attributes);
        body.put("requiredActions", user.requiredActions() != null ? user.requiredActions() : List.of());

        webClient.put()
                .uri(properties.adminUserEndpoint(userId.toString()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public void assignRealmRole(UUID userId, String roleName) {
        String adminToken = obtainAdminToken();
        var role = webClient.get()
                .uri(properties.serverUrl() + "/admin/realms/" + properties.realm() + "/roles/" + roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(KeycloakRoleRepresentation.class)
                .block();

        if (role == null) return;

        webClient.post()
                .uri(properties.adminUserEndpoint(userId.toString()) + "/role-mappings/realm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(role))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public UUID findUserIdByEmail(String email) {
        String adminToken = obtainAdminToken();
        List<KeycloakUserRepresentation> users = webClient.get()
                .uri("/admin/realms/{realm}/users?email={email}&exact=true",
                        properties.realm(), email)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToFlux(KeycloakUserRepresentation.class)
                .collectList()
                .block();

        if (users == null || users.isEmpty()) {
            throw new AuthService.AuthException("AUTH_USER_NOT_FOUND", "Utilisateur introuvable");
        }
        return UUID.fromString(users.getFirst().id());
    }


    private void finalizeUserAccount(UUID userId, String adminToken) {
        for (String action : List.of(
                "VERIFY_EMAIL", "UPDATE_PASSWORD", "CONFIGURE_TOTP", "UPDATE_PROFILE", "VERIFY_PROFILE"
        )) {
            try {
                webClient.delete()
                        .uri(properties.adminUserEndpoint(userId.toString()) + "/required-actions/" + action)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } catch (Exception ignored) {
                // action absente
            }
        }
    }

    private AuthTokens requestToken(Map<String, String> params) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);

        try {
            TokenResponse response = webClient.post()
                    .uri(properties.tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null || response.accessToken() == null) {
                throw new AuthService.AuthException("AUTH_TOKEN_FAILED", "Échec obtention token");
            }
            return new AuthTokens(
                    response.accessToken(),
                    response.refreshToken(),
                    response.expiresIn(),
                    response.tokenType() != null ? response.tokenType() : "Bearer"
            );
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.BadRequest e) {
            throw new AuthService.AuthException("AUTH_INVALID_CREDENTIALS", "Email ou mot de passe incorrect");
        }
    }

    private String obtainAdminToken() {
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
            throw new AuthService.AuthException("AUTH_KEYCLOAK_UNAVAILABLE", "Keycloak admin indisponible");
        }
        return response.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeycloakUserRepresentation(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            Boolean enabled,
            Boolean emailVerified,
            List<String> requiredActions,
            Map<String, List<String>> attributes
    ) {
        KeycloakUserRepresentation(String username, String email, String firstName, String lastName,
                                   Boolean enabled, Boolean emailVerified,
                                   Map<String, List<String>> attributes) {
            this(null, username, email, firstName, lastName, enabled, emailVerified, List.of(), attributes);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KeycloakRoleRepresentation(String id, String name) {}
}
