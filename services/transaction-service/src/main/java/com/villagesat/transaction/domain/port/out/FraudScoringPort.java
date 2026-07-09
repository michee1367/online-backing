package com.villagesat.transaction.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface FraudScoringPort {

    FraudResult score(FraudRequest request);

    record FraudRequest(UUID userId, UUID walletId, BigDecimal amount, String currency) {}

    record FraudResult(int score, FraudAction action) {}

    enum FraudAction { ALLOW, REVIEW, BLOCK, STEP_UP_MFA }
}
