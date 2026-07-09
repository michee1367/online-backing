package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;

public interface ScoringRule {

    RuleResult evaluate(FraudScoreRequest request);
}
