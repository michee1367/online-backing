package com.villagesat.user.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.user.domain.model.DataExportRequest;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.model.UserProfile;
import com.villagesat.user.domain.model.UserWithProfile;
import com.villagesat.user.domain.port.in.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Profils utilisateurs")
@PreAuthorize("hasRole('CUSTOMER') or hasRole('MERCHANT') or hasRole('ADMIN')")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté")
    public UserProfileResponse getMe() {
        return UserProfileResponse.from(userUseCase.getCurrentUser(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Mettre à jour le profil")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        UserWithProfile updated = userUseCase.updateCurrentUser(
                SecurityUtils.getCurrentUserId(), request.toCommand());
        return UserProfileResponse.from(updated);
    }

    @GetMapping("/me/data-export")
    @Operation(summary = "Demander un export RGPD des données personnelles")
    public ResponseEntity<DataExportResponse> requestDataExport() {
        DataExportRequest request = userUseCase.requestDataExport(SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DataExportResponse.from(request));
    }

    public record UpdateProfileRequest(
            String firstName,
            String lastName,
            String phone,
            LocalDate dateOfBirth,
            String addressLine1,
            String addressCity,
            String addressCountry,
            String preferredLanguage,
            String timezone,
            String avatarUrl,
            Map<String, Object> metadata
    ) {
        UserUseCase.UpdateProfileCommand toCommand() {
            return new UserUseCase.UpdateProfileCommand(
                    firstName, lastName, phone, dateOfBirth,
                    addressLine1, addressCity, addressCountry,
                    preferredLanguage, timezone, avatarUrl, metadata);
        }
    }

    public record UserProfileResponse(
            UUID userId,
            String email,
            String phone,
            String firstName,
            String lastName,
            String countryCode,
            int kycLevel,
            String status,
            LocalDate dateOfBirth,
            String addressLine1,
            String addressCity,
            String addressCountry,
            String preferredLanguage,
            String timezone,
            String avatarUrl
    ) {
        static UserProfileResponse from(UserWithProfile data) {
            User u = data.user();
            UserProfile p = data.profile();
            return new UserProfileResponse(
                    u.id(), u.email(), u.phone(), u.firstName(), u.lastName(), u.countryCode(),
                    u.kycLevel(), u.status().name(),
                    p.dateOfBirth(), p.addressLine1(), p.addressCity(), p.addressCountry(),
                    p.preferredLanguage(), p.timezone(), p.avatarUrl());
        }
    }

    public record DataExportResponse(
            UUID exportId,
            String status,
            String message
    ) {
        static DataExportResponse from(DataExportRequest r) {
            return new DataExportResponse(
                    r.id(),
                    r.status().name(),
                    "Votre export est en cours de préparation. Vous recevrez un lien de téléchargement sous 24h."
            );
        }
    }
}
