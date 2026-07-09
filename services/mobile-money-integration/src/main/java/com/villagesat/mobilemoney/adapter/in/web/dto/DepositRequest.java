package com.villagesat.mobilemoney.adapter.in.web.dto;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(
        @NotNull UUID walletId,
        @NotNull MobileMoneyProvider provider,
        @NotBlank String phoneNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
) {
}
