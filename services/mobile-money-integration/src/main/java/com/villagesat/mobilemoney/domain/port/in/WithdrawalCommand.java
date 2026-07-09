package com.villagesat.mobilemoney.domain.port.in;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalCommand(
        UUID userId,
        UUID walletId,
        MobileMoneyProvider provider,
        String phoneNumber,
        BigDecimal amount,
        String currency
) {
}
