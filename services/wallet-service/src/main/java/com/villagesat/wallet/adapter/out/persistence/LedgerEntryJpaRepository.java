package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.LedgerEntryEntity;
import com.villagesat.wallet.adapter.out.persistence.entity.LedgerEntryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, LedgerEntryId> {

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM wallets.ledger_entries
            WHERE wallet_id = :walletId
              AND entry_type = 'DEBIT'
              AND created_at >= :since
            """, nativeQuery = true)
    BigDecimal sumDebitsSince(@Param("walletId") UUID walletId, @Param("since") Instant since);

    /**
     * 💡 Requête d'idempotence automatique générée par Spring Data JPA.
     * Elle va générer un "SELECT COUNT(...) > 0" optimisé en arrière-plan.
     */
    boolean existsByTransactionIdAndEntryType(UUID transactionId, String entryType);

    // Spring Data JPA va générer automatiquement la requête SQL basée sur ce nom de méthode
    Page<LedgerEntryEntity> findByWalletIdOrderByEntrySequenceDesc(UUID walletId, Pageable pageable);
}
