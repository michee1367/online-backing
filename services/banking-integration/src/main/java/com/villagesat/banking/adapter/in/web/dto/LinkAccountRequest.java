package com.villagesat.banking.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkAccountRequest(
        @NotBlank String bankName,
        String bankCode,
        @NotBlank String accountNumber,
        @NotBlank String accountHolderName,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
