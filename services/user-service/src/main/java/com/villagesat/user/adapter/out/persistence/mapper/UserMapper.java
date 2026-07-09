package com.villagesat.user.adapter.out.persistence.mapper;

import com.villagesat.user.adapter.out.persistence.entity.DataExportRequestEntity;
import com.villagesat.user.adapter.out.persistence.entity.UserEntity;
import com.villagesat.user.adapter.out.persistence.entity.UserProfileEntity;
import com.villagesat.user.application.service.UserService;
import com.villagesat.user.domain.model.DataExportRequest;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.model.UserProfile;

import java.util.Map;

public final class UserMapper {

    private UserMapper() {}

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCountryCode(),
                entity.getKycLevel(),
                User.UserStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getKeycloakId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setEmailHash(UserService.hashValue(user.email()));
        entity.setPhone(user.phone());
        entity.setPhoneHash(user.phone() != null ? UserService.hashValue(user.phone()) : null);
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setCountryCode(user.countryCode());
        entity.setKycLevel(user.kycLevel());
        entity.setStatus(user.status().name());
        entity.setTenantId(user.tenantId());
        entity.setKeycloakId(user.keycloakId());
        entity.setCreatedAt(user.createdAt());
        entity.setUpdatedAt(user.updatedAt());
        entity.setDeletedAt(user.deletedAt());
        entity.setVersion(user.version());
        return entity;
    }

    public static UserProfile toDomain(UserProfileEntity entity) {
        return new UserProfile(
                entity.getUserId(),
                entity.getDateOfBirth(),
                entity.getAddressLine1(),
                entity.getAddressCity(),
                entity.getAddressCountry(),
                entity.getPreferredLanguage(),
                entity.getTimezone(),
                entity.getAvatarUrl(),
                entity.getMetadata() != null ? entity.getMetadata() : Map.of(),
                entity.getUpdatedAt()
        );
    }

    public static UserProfileEntity toEntity(UserProfile profile) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setUserId(profile.userId());
        entity.setDateOfBirth(profile.dateOfBirth());
        entity.setAddressLine1(profile.addressLine1());
        entity.setAddressCity(profile.addressCity());
        entity.setAddressCountry(profile.addressCountry());
        entity.setPreferredLanguage(profile.preferredLanguage());
        entity.setTimezone(profile.timezone());
        entity.setAvatarUrl(profile.avatarUrl());
        entity.setMetadata(profile.metadata());
        return entity;
    }

    public static DataExportRequest toDomain(DataExportRequestEntity entity) {
        return new DataExportRequest(
                entity.getId(),
                entity.getUserId(),
                DataExportRequest.ExportStatus.valueOf(entity.getStatus()),
                entity.getDownloadUrl(),
                entity.getRequestedAt(),
                entity.getCompletedAt()
        );
    }

    public static DataExportRequestEntity toEntity(DataExportRequest request) {
        DataExportRequestEntity entity = new DataExportRequestEntity();
        entity.setId(request.id());
        entity.setUserId(request.userId());
        entity.setStatus(request.status().name());
        entity.setDownloadUrl(request.downloadUrl());
        entity.setRequestedAt(request.requestedAt());
        entity.setCompletedAt(request.completedAt());
        return entity;
    }
}
