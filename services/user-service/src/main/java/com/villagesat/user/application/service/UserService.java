package com.villagesat.user.application.service;

import com.villagesat.user.domain.model.*;
import com.villagesat.user.domain.port.in.UserUseCase;
import com.villagesat.user.domain.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserUseCase {

    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final KeycloakSyncPort keycloakSync;
    private final DataExportRepository dataExportRepository;

    public UserService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       KeycloakSyncPort keycloakSync,
                       DataExportRepository dataExportRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.keycloakSync = keycloakSync;
        this.dataExportRepository = dataExportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserWithProfile getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElse(UserProfile.defaultProfile(userId));
        return new UserWithProfile(user, profile);
    }

    @Override
    public UserWithProfile updateCurrentUser(UUID userId, UpdateProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isActive()) {
            throw new UserException("USER_INACTIVE", "Ce compte n'est pas actif");
        }

        User updatedUser = user;
        if (command.firstName() != null || command.lastName() != null) {
            updatedUser = user.withProfileNames(
                    command.firstName() != null ? command.firstName() : user.firstName(),
                    command.lastName() != null ? command.lastName() : user.lastName());
        }
        if (command.phone() != null) {
            updatedUser = new User(updatedUser.id(), updatedUser.email(), command.phone(),
                    updatedUser.firstName(), updatedUser.lastName(), updatedUser.countryCode(),
                    updatedUser.kycLevel(), updatedUser.status(), updatedUser.tenantId(),
                    updatedUser.keycloakId(), updatedUser.createdAt(), Instant.now(),
                    updatedUser.deletedAt(), updatedUser.version());
        }
        updatedUser = userRepository.save(updatedUser);

        UserProfile current = profileRepository.findByUserId(userId)
                .orElse(UserProfile.defaultProfile(userId));
        UserProfile updatedProfile = mergeProfile(current, command);
        updatedProfile = profileRepository.save(updatedProfile);

        UUID keycloakId = updatedUser.keycloakId() != null ? updatedUser.keycloakId() : updatedUser.id();
        keycloakSync.syncUserProfile(keycloakId, updatedUser, updatedProfile);

        return new UserWithProfile(updatedUser, updatedProfile);
    }

    @Override
    public DataExportRequest requestDataExport(UUID userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        DataExportRequest request = new DataExportRequest(
                UUID.randomUUID(),
                userId,
                DataExportRequest.ExportStatus.PENDING,
                null,
                Instant.now(),
                null
        );
        return dataExportRepository.save(request);
    }

    @Override
    public void provisionFromRegistration(ProvisionUserCommand command) {
        if (userRepository.existsById(command.userId())) {
            return;
        }

        User.UserStatus status = parseStatus(command.status());
        User user = new User(
                command.userId(),
                command.email(),
                command.phone(),
                command.firstName(),
                command.lastName(),
                command.countryCode(),
                command.kycLevel(),
                status,
                DEFAULT_TENANT,
                command.userId(),
                Instant.now(),
                Instant.now(),
                null,
                0L
        );
        userRepository.save(user);
        profileRepository.save(UserProfile.defaultProfile(command.userId()));

        keycloakSync.syncUserProfile(command.userId(), user, UserProfile.defaultProfile(command.userId()));
        keycloakSync.updateKycLevel(command.userId(), command.kycLevel());
    }

    private UserProfile mergeProfile(UserProfile current, UpdateProfileCommand command) {
        return new UserProfile(
                current.userId(),
                command.dateOfBirth() != null ? command.dateOfBirth() : current.dateOfBirth(),
                command.addressLine1() != null ? command.addressLine1() : current.addressLine1(),
                command.addressCity() != null ? command.addressCity() : current.addressCity(),
                command.addressCountry() != null ? command.addressCountry() : current.addressCountry(),
                command.preferredLanguage() != null ? command.preferredLanguage() : current.preferredLanguage(),
                command.timezone() != null ? command.timezone() : current.timezone(),
                command.avatarUrl() != null ? command.avatarUrl() : current.avatarUrl(),
                command.metadata() != null ? command.metadata() : current.metadata(),
                Instant.now()
        );
    }

    private User.UserStatus parseStatus(String status) {
        try {
            return User.UserStatus.valueOf(status);
        } catch (Exception e) {
            return User.UserStatus.PENDING_VERIFICATION;
        }
    }

    public static String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.toLowerCase().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID userId) {
            super("Utilisateur introuvable: " + userId);
        }
    }

    public static class UserException extends RuntimeException {
        private final String code;

        public UserException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
