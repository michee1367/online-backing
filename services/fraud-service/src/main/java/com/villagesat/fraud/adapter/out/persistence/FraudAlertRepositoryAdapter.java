package com.villagesat.fraud.adapter.out.persistence;

import com.villagesat.fraud.adapter.out.persistence.mapper.FraudAlertMapper;
import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.port.out.FraudAlertRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FraudAlertRepositoryAdapter implements FraudAlertRepository {

    private final FraudAlertJpaRepository jpaRepository;

    public FraudAlertRepositoryAdapter(FraudAlertJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FraudAlert save(FraudAlert alert) {
        var saved = jpaRepository.save(FraudAlertMapper.toEntity(alert));
        return FraudAlertMapper.toDomain(saved);
    }

    @Override
    public Optional<FraudAlert> findById(UUID id) {
        return jpaRepository.findById(id).map(FraudAlertMapper::toDomain);
    }

    @Override
    public List<FraudAlert> findByStatus(AlertStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(status.name())
                .stream()
                .map(FraudAlertMapper::toDomain)
                .toList();
    }

    @Override
    public List<FraudAlert> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FraudAlertMapper::toDomain)
                .toList();
    }
}
