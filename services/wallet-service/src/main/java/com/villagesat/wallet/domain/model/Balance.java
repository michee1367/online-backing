package com.villagesat.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Balance(
        UUID walletId,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        Instant lastTransactionAt,
        long version
) {
    public Balance {
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
    }

  public boolean hasSufficientFunds(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    public Balance debit(BigDecimal amount) {
        if (!hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(walletId, amount, availableBalance);
        }
        BigDecimal newBalance = balance.subtract(amount);
        BigDecimal newAvailable = availableBalance.subtract(amount);
        return new Balance(walletId, newBalance, newAvailable, pendingBalance,
                Instant.now(), version);
    }

    public Balance credit(BigDecimal amount) {
        return new Balance(walletId, balance.add(amount), availableBalance.add(amount),
                pendingBalance, Instant.now(), version);
    }
}
