package com.villagesat.mobilemoney.adapter.in.web.dto;

import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID userId,
        UUID walletId,
        String provider,
        String phoneNumber,
        BigDecimal amount,
        String currency,
        String transactionType,
        String status,
        String externalRef,
        String providerRef,
        Instant createdAt,
        Instant completedAt,
        String failedReason
) {
    public static TransactionResponse from(MobileMoneyTransaction tx) {
        return new TransactionResponse(
                tx.getId(), tx.getUserId(), tx.getWalletId(),
                tx.getProvider().name(), tx.getPhoneNumber(),
                tx.getAmount(), tx.getCurrency(),
                tx.getTransactionType().name(), tx.getStatus().name(),
                tx.getExternalRef().toString(), tx.getProviderRef(),
                tx.getCreatedAt(), tx.getCompletedAt(), tx.getFailedReason()
        );
    }
}
