package com.villagesat.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QrCode(
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String reference,
        Instant expiresAt,
        String data
) {}
