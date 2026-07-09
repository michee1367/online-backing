package com.villagesat.compliance.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface IdentityVerificationPort {

    VerificationResult verify(VerificationRequest request);

    record VerificationRequest(
            UUID userId,
            String documentType,
            String documentFrontKey,
            String selfieKey
    ) {}

    record VerificationResult(BigDecimal score, String providerRef, boolean documentValid) {}
}
