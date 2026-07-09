package com.villagesat.wallet.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Session User", description = "Profil utilisateur pour la session mobile")
public class SessionUserController {

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Profil de l'utilisateur connecté (depuis JWT Keycloak)")
    public UserMeResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return new UserMeResponse(
                userId,
                claim(jwt, "email"),
                claim(jwt, "phone"),
                firstNonBlank(claim(jwt, "given_name"), claim(jwt, "firstName")),
                firstNonBlank(claim(jwt, "family_name"), claim(jwt, "lastName")),
                jwt.getIssuedAt() != null ? jwt.getIssuedAt() : Instant.now()
        );
    }

    private static String claim(Jwt jwt, String name) {
        String value = jwt.getClaimAsString(name);
        return value != null && !value.isBlank() ? value : null;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    public record UserMeResponse(
            UUID userId,
            String email,
            String phone,
            String firstName,
            String lastName,
            Instant createdAt
    ) {}
}
