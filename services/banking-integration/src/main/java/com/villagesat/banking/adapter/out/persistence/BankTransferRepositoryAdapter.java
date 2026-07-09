package com.villagesat.banking.adapter.out.persistence;

import com.villagesat.banking.adapter.out.persistence.mapper.BankingMapper;
import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.port.out.BankTransferRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BankTransferRepositoryAdapter implements BankTransferRepository {

    private final BankTransferJpaRepository jpaRepository;

    public BankTransferRepositoryAdapter(BankTransferJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BankTransfer save(BankTransfer transfer) {
        var entity = BankingMapper.toEntity(transfer);
        var saved = jpaRepository.save(entity);
        return BankingMapper.toDomain(saved);
    }

    @Override
    public Optional<BankTransfer> findById(UUID id) {
        return jpaRepository.findById(id).map(BankingMapper::toDomain);
    }

    @Override
    public List<BankTransfer> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BankingMapper::toDomain)
                .toList();
    }
}
