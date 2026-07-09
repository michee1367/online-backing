package com.villagesat.banking.adapter.in.web.dto;

import com.villagesat.banking.domain.model.LinkedBankAccount;

import java.time.Instant;
import java.util.UUID;

public record LinkedAccountResponse(
        UUID id,
        UUID userId,
        String bankName,
        String bankCode,
        String accountHolderName,
        String currency,
        String status,
        Instant createdAt
) {
    public static LinkedAccountResponse from(LinkedBankAccount account) {
        return new LinkedAccountResponse(
                account.getId(), account.getUserId(),
                account.getBankName(), account.getBankCode(),
                account.getAccountHolderName(), account.getCurrency(),
                account.getStatus().name(), account.getCreatedAt()
        );
    }
}
