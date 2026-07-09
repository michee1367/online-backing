package com.villagesat.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "villagesat.keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String clientId,
        String adminClientId,
        String adminClientSecret
) {
    public String tokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String logoutEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    public String adminUsersEndpoint() {
        return serverUrl + "/admin/realms/" + realm + "/users";
    }

    public String adminUserEndpoint(String userId) {
        return adminUsersEndpoint() + "/" + userId;
    }
}
