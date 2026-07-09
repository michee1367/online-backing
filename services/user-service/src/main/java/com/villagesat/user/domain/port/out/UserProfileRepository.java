package com.villagesat.user.domain.port.out;

import com.villagesat.user.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    UserProfile save(UserProfile profile);

    Optional<UserProfile> findByUserId(UUID userId);
}
