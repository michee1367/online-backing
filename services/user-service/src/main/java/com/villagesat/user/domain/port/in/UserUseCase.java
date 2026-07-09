package com.villagesat.user.domain.port.in;

import com.villagesat.user.domain.model.DataExportRequest;
import com.villagesat.user.domain.model.UserWithProfile;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface UserUseCase {

    UserWithProfile getCurrentUser(UUID userId);

    UserWithProfile updateCurrentUser(UUID userId, UpdateProfileCommand command);

    DataExportRequest requestDataExport(UUID userId);

    void provisionFromRegistration(ProvisionUserCommand command);

    record UpdateProfileCommand(
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
    ) {}

    record ProvisionUserCommand(
            UUID userId,
            String email,
            String phone,
            String firstName,
            String lastName,
            String countryCode,
            int kycLevel,
            String status
    ) {}
}
