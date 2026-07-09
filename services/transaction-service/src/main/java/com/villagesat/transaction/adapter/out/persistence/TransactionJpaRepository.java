package com.villagesat.transaction.adapter.out.persistence;

import com.villagesat.transaction.adapter.out.persistence.entity.TransactionEntity;
import com.villagesat.transaction.adapter.out.persistence.entity.TransactionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, TransactionId> {

    Optional<TransactionEntity> findFirstByIdempotencyKeyOrderById_CreatedAtDesc(UUID idempotencyKey);

    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM TransactionEntity t WHERE t.id.id = :transactionId")
    Optional<TransactionEntity> findByTransactionId(
            @org.springframework.data.repository.query.Param("transactionId") UUID transactionId);
}
