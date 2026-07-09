package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.config.FraudProperties;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LargeTransferRule implements ScoringRule {

    private final BigDecimal threshold;

    public LargeTransferRule(FraudProperties properties) {
        this.threshold = BigDecimal.valueOf(properties.getLargeTransferThreshold());
    }

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        if (request.amount().compareTo(threshold) > 0) {
            return new RuleResult("LARGE_TRANSFER", 40,
                    "Transfert important: %s (seuil: %s)".formatted(request.amount(), threshold));
        }
        return RuleResult.NONE;
    }
}
