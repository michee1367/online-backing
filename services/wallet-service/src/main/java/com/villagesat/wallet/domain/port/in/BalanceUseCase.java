package com.villagesat.wallet.domain.port.in;

import com.villagesat.wallet.domain.model.Balance;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalanceUseCase {

    Balance getBalance(UUID walletId, UUID userId);

    Balance debit(UUID walletId, UUID transactionId, BigDecimal amount, String description);

    Balance credit(UUID walletId, UUID transactionId, BigDecimal amount, String description);
}
