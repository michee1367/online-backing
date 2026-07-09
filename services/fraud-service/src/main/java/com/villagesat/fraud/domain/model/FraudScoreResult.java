package com.villagesat.fraud.domain.model;

import java.util.List;

public record FraudScoreResult(
        int score,
        FraudAction action,
        List<String> reasons,
        List<String> rulesFired
) {
    public FraudScoreResult {
        score = Math.min(100, Math.max(0, score));
        reasons = List.copyOf(reasons);
        rulesFired = List.copyOf(rulesFired);
    }
}
