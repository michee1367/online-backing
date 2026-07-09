package com.villagesat.user.support;

import com.villagesat.user.domain.model.DataExportRequest;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.model.UserProfile;
import com.villagesat.user.domain.model.UserWithProfile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class UserTestFixtures {

    public static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UserTestFixtures() {}

    public static User activeUser() {
        return new User(
                USER_ID, "alice@example.com", "+243810000001",
                "Alice", "Mutombo", "CD", 0,
                User.UserStatus.ACTIVE, TENANT_ID, USER_ID,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                null, 0L
        );
    }

    public static User pendingUser() {
        return new User(
                USER_ID, "alice@example.com", "+243810000001",
                "Alice", "Mutombo", "CD", 0,
                User.UserStatus.PENDING_VERIFICATION, TENANT_ID, USER_ID,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                null, 0L
        );
    }

    public static User closedUser() {
        return new User(
                USER_ID, "alice@example.com", "+243810000001",
                "Alice", "Mutombo", "CD", 0,
                User.UserStatus.CLOSED, TENANT_ID, USER_ID,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:00Z"),
                null, 0L
        );
    }

    public static UserProfile defaultProfile() {
        return UserProfile.defaultProfile(USER_ID);
    }

    public static UserWithProfile activeUserWithProfile() {
        return new UserWithProfile(activeUser(), defaultProfile());
    }

    public static DataExportRequest pendingExportRequest() {
        return new DataExportRequest(
                UUID.randomUUID(), USER_ID,
                DataExportRequest.ExportStatus.PENDING,
                null, Instant.now(), null
        );
    }

    public static String userCreatedEvent() {
        return """
                {
                  "eventType": "user.created",
                  "payload": {
                    "userId": "%s",
                    "email": "alice@example.com",
                    "phone": "+243810000001",
                    "firstName": "Alice",
                    "lastName": "Mutombo",
                    "countryCode": "CD",
                    "kycLevel": 0,
                    "status": "PENDING_VERIFICATION"
                  }
                }
                """.formatted(USER_ID);
    }

    public static String kycApprovedEvent(int level) {
        return """
                {
                  "eventType": "kyc.approved",
                  "payload": {
                    "userId": "%s",
                    "level": %d,
                    "submissionId": "sub-1",
                    "status": "APPROVED",
                    "riskScore": 0.95
                  }
                }
                """.formatted(USER_ID, level);
    }
}
