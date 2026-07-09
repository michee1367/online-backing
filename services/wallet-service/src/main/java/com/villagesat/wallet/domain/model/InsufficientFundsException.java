package com.villagesat.wallet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    private final UUID walletId;
    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFundsException(UUID walletId, BigDecimal requested, BigDecimal available) {
        super("Insufficient funds in wallet %s: requested=%s, available=%s"
                .formatted(walletId, requested, available));
        this.walletId = walletId;
        this.requested = requested;
        this.available = available;
    }

    public UUID getWalletId() { return walletId; }
    public BigDecimal getRequested() { return requested; }
    public BigDecimal getAvailable() { return available; }
}
