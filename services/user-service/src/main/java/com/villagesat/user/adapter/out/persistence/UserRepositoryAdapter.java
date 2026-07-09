package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.mapper.UserMapper;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return UserMapper.toDomain(jpaRepository.save(UserMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailHash(String emailHash) {
        return jpaRepository.findByEmailHashAndDeletedAtIsNull(emailHash).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}
