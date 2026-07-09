package com.villagesat.wallet.adapter.out.persistence;

import com.villagesat.wallet.adapter.out.persistence.entity.LedgerEntryEntity;
import com.villagesat.wallet.domain.port.out.LedgerRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final LedgerEntryJpaRepository jpaRepository;

    public LedgerRepositoryAdapter(LedgerEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void appendEntry(UUID walletId, UUID transactionId, String entryType,
                            BigDecimal amount, BigDecimal balanceAfter, String description) {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setWalletId(walletId);
        entry.setTransactionId(transactionId);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setDescription(description);
        jpaRepository.save(entry);
    }

    @Override
    public BigDecimal sumDebitsSince(UUID walletId, Instant since) {
        return jpaRepository.sumDebitsSince(walletId, since);
    }

    // 💡 Implémentation du contrôle d'idempotence
    @Override
    public boolean existsByTransactionIdAndEntryType(UUID transactionId, String entryType) {
        return jpaRepository.existsByTransactionIdAndEntryType(transactionId, entryType);
    }
}
