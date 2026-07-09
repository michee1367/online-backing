package com.villagesat.banking.domain.port.in;

import com.villagesat.banking.domain.model.BankTransferType;
import com.villagesat.banking.domain.model.TransferDirection;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateTransferCommand(
        UUID userId,
        UUID walletId,
        UUID linkedAccountId,
        TransferDirection direction,
        BankTransferType transferType,
        BigDecimal amount,
        String currency,
        String swiftCode
) {
}
