package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmailHashAndDeletedAtIsNull(String emailHash);
}
