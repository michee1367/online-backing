package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.config.FraudProperties;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import com.villagesat.fraud.domain.port.out.TransactionHistoryPort;
import org.springframework.stereotype.Component;

@Component
public class VelocityRule implements ScoringRule {

    private final TransactionHistoryPort transactionHistory;
    private final int velocityLimit;
    private final int windowHours;

    public VelocityRule(TransactionHistoryPort transactionHistory, FraudProperties properties) {
        this.transactionHistory = transactionHistory;
        this.velocityLimit = properties.getVelocityLimit();
        this.windowHours = properties.getVelocityWindowHours();
    }

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        int recentCount = transactionHistory.getRecentTransactionCount(request.userId(), windowHours);
        if (recentCount > velocityLimit) {
            return new RuleResult("VELOCITY", 25,
                    "%d transactions en %dh (limite: %d)".formatted(recentCount, windowHours, velocityLimit));
        }
        return RuleResult.NONE;
    }
}
