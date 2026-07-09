package com.villagesat.user.domain.port.out;

import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.model.UserProfile;

import java.util.UUID;

public interface KeycloakSyncPort {

    void syncUserProfile(UUID keycloakUserId, User user, UserProfile profile);

    void updateKycLevel(UUID keycloakUserId, int kycLevel);

    void updateStatus(UUID keycloakUserId, String status);
}
