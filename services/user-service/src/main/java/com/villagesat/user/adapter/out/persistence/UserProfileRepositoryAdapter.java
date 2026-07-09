package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.mapper.UserMapper;
import com.villagesat.user.domain.model.UserProfile;
import com.villagesat.user.domain.port.out.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfileRepositoryAdapter implements UserProfileRepository {

    private final UserProfileJpaRepository jpaRepository;

    public UserProfileRepositoryAdapter(UserProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return UserMapper.toDomain(jpaRepository.save(UserMapper.toEntity(profile)));
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(UserMapper::toDomain);
    }
}
