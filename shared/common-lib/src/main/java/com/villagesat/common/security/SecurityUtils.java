package com.villagesat.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        Jwt jwt = getJwt();
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean hasRole(String role) {
        Jwt jwt = getJwt();
        var directRoles = jwt.getClaimAsStringList("roles");
        if (directRoles != null && directRoles.contains(role)) {
            return true;
        }
        @SuppressWarnings("unchecked")
        var realmAccess = (java.util.Map<String, Object>) jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.List<?> roles) {
            return roles.stream().anyMatch(r -> role.equalsIgnoreCase(String.valueOf(r)));
        }
        return false;
    }

    public static boolean isMfaVerified() {
        Jwt jwt = getJwt();
        Boolean verified = jwt.getClaim("mfa_verified");
        return Boolean.TRUE.equals(verified);
    }

    private static Jwt getJwt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated JWT found");
        }
        return jwt;
    }
}
