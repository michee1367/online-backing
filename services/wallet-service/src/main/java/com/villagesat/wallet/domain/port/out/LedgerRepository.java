package com.villagesat.wallet.domain.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface LedgerRepository {

    void appendEntry(UUID walletId, UUID transactionId, String entryType,
                     BigDecimal amount, BigDecimal balanceAfter, String description);

    BigDecimal sumDebitsSince(UUID walletId, Instant since);

    /**
     * 💡 Vérifie si une transaction d'un certain type (DEBIT/CREDIT) a déjà été enregistrée.
     * Utilisé pour garantir l'idempotence et éviter les doubles débits/crédits en cas de retry réseau.
     */
    boolean existsByTransactionIdAndEntryType(UUID transactionId, String entryType);
    
}
