package com.villagesat.banking.adapter.in.web.dto;

import com.villagesat.banking.domain.model.BankTransferType;
import com.villagesat.banking.domain.model.TransferDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateTransferRequest(
        @NotNull UUID walletId,
        @NotNull UUID linkedAccountId,
        @NotNull TransferDirection direction,
        @NotNull BankTransferType transferType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String swiftCode
) {
}
