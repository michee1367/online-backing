package com.villagesat.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "villagesat.keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret
) {
    public String tokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String adminUserEndpoint(String userId) {
        return serverUrl + "/admin/realms/" + realm + "/users/" + userId;
    }
}
