package com.villagesat.wallet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionLimitExceededException extends RuntimeException {

    private final String limitType;

    public TransactionLimitExceededException(UUID walletId, String limitType,
                                           BigDecimal amount, BigDecimal limit, BigDecimal used) {
        super("Transaction limit exceeded for wallet %s (%s): amount=%s, limit=%s, used=%s"
                .formatted(walletId, limitType, amount, limit, used));
        this.limitType = limitType;
    }

    public String limitType() {
        return limitType;
    }
}
