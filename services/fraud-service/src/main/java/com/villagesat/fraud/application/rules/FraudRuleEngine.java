package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.domain.model.FraudAction;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.FraudScoreResult;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FraudRuleEngine {

    private final List<ScoringRule> rules;

    public FraudRuleEngine(List<ScoringRule> rules) {
        this.rules = rules;
    }

    public FraudScoreResult evaluate(FraudScoreRequest request) {
        List<String> reasons = new ArrayList<>();
        List<String> rulesFired = new ArrayList<>();
        int totalScore = 0;

        for (ScoringRule rule : rules) {
            RuleResult result = rule.evaluate(request);
            if (result.triggered()) {
                totalScore += result.score();
                reasons.add(result.reason());
                rulesFired.add(result.ruleName());
            }
        }

        int clampedScore = Math.min(100, totalScore);
        FraudAction action = FraudAction.fromScore(clampedScore);

        return new FraudScoreResult(clampedScore, action, reasons, rulesFired);
    }
}
