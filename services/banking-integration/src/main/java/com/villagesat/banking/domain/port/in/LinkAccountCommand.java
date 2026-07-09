package com.villagesat.banking.domain.port.in;

import java.util.UUID;

public record LinkAccountCommand(
        UUID userId,
        String bankName,
        String bankCode,
        String accountNumber,
        String accountHolderName,
        String currency
) {
}
