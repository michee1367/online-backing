package com.villagesat.mobilemoney.adapter.out.persistence;

import com.villagesat.mobilemoney.adapter.out.persistence.mapper.MobileMoneyMapper;
import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MobileMoneyTransactionRepositoryAdapter implements MobileMoneyTransactionRepository {

    private final MobileMoneyTransactionJpaRepository jpaRepository;

    public MobileMoneyTransactionRepositoryAdapter(MobileMoneyTransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MobileMoneyTransaction save(MobileMoneyTransaction transaction) {
        var entity = MobileMoneyMapper.toEntity(transaction);
        var saved = jpaRepository.save(entity);
        return MobileMoneyMapper.toDomain(saved);
    }

    @Override
    public Optional<MobileMoneyTransaction> findById(UUID id) {
        return jpaRepository.findById(id).map(MobileMoneyMapper::toDomain);
    }

    @Override
    public Optional<MobileMoneyTransaction> findByExternalRef(String externalRef) {
        return jpaRepository.findByExternalRef(externalRef).map(MobileMoneyMapper::toDomain);
    }

    @Override
    public List<MobileMoneyTransaction> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(MobileMoneyMapper::toDomain)
                .toList();
    }
}
