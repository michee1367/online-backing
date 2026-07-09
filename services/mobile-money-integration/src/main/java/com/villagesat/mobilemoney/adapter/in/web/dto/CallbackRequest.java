package com.villagesat.mobilemoney.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CallbackRequest(
        @NotBlank String externalRef,
        String providerRef,
        @NotBlank String status,
        String failedReason
) {
}
