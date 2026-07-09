package com.villagesat.user.domain.port.out;

import com.villagesat.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmailHash(String emailHash);

    boolean existsById(UUID id);
}
