package com.villagesat.mobilemoney.domain.port.in;

public record CallbackCommand(
        String externalRef,
        String providerRef,
        String status,
        String failedReason
) {
}
