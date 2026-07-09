package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class CrossBorderRule implements ScoringRule {

    private static final String LOCAL_CURRENCY = "CDF";

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        if (request.currency() != null && !LOCAL_CURRENCY.equalsIgnoreCase(request.currency())) {
            return new RuleResult("CROSS_BORDER", 10,
                    "Transaction internationale en %s".formatted(request.currency()));
        }
        return RuleResult.NONE;
    }
}
