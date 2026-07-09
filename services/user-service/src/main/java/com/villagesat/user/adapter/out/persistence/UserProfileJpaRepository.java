package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, UUID> {
}
