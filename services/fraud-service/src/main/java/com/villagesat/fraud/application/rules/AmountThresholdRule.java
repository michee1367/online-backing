package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.config.FraudProperties;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountThresholdRule implements ScoringRule {

    private final BigDecimal threshold;

    public AmountThresholdRule(FraudProperties properties) {
        this.threshold = BigDecimal.valueOf(properties.getAmountThreshold());
    }

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        if (request.amount().compareTo(threshold) > 0) {
            return new RuleResult("AMOUNT_THRESHOLD", 30,
                    "Montant %s dépasse le seuil de %s".formatted(request.amount(), threshold));
        }
        return RuleResult.NONE;
    }
}
