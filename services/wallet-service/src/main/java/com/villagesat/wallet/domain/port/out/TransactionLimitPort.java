package com.villagesat.wallet.domain.port.out;

import com.villagesat.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionLimitPort {

    void validateDebitWithinLimits(Wallet wallet, UUID transactionId, BigDecimal amount);
}
