package com.villagesat.transaction.adapter.out.persistence;

import com.villagesat.transaction.adapter.out.persistence.mapper.TransactionMapper;
import com.villagesat.transaction.adapter.out.persistence.entity.TransactionEntity;
import com.villagesat.transaction.adapter.out.persistence.entity.TransactionId;
import com.villagesat.transaction.domain.model.Transaction;
import com.villagesat.transaction.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionId pk = new TransactionId(transaction.id(), transaction.createdAt());
        TransactionEntity entity = jpaRepository.findById(pk)
                .map(existing -> {
                    TransactionMapper.updateEntity(existing, transaction);
                    return existing;
                })
                .orElseGet(() -> TransactionMapper.toEntity(transaction));
        return TransactionMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findByTransactionId(id).map(TransactionMapper::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey) {
        return jpaRepository.findFirstByIdempotencyKeyOrderById_CreatedAtDesc(idempotencyKey)
                .map(TransactionMapper::toDomain);
    }
}
