package com.villagesat.fraud.application.service;

import com.villagesat.fraud.application.rules.FraudRuleEngine;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.FraudScoreResult;
import com.villagesat.fraud.domain.port.in.FraudScoringUseCase;
import com.villagesat.fraud.domain.port.out.FraudAlertRepository;
import com.villagesat.fraud.domain.port.out.FraudEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FraudScoringService implements FraudScoringUseCase {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringService.class);

    private final FraudRuleEngine ruleEngine;
    private final FraudAlertRepository alertRepository;
    private final FraudEventPublisher eventPublisher;

    public FraudScoringService(FraudRuleEngine ruleEngine,
                               FraudAlertRepository alertRepository,
                               FraudEventPublisher eventPublisher) {
        this.ruleEngine = ruleEngine;
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public FraudScoreResult score(FraudScoreRequest request) {
        FraudScoreResult result = ruleEngine.evaluate(request);

        log.info("Fraud score for user={} amount={} currency={}: score={} action={}",
                request.userId(), request.amount(), request.currency(),
                result.score(), result.action());

        if (result.score() >= 30) {
            FraudAlert alert = FraudAlert.create(request.userId(), null, result);
            alert = alertRepository.save(alert);
            log.info("Created fraud alert {} for user {} (score={})",
                    alert.id(), request.userId(), result.score());

            if (result.score() >= 80) {
                eventPublisher.publishFraudAlert(alert);
            }
        }

        return result;
    }

    public FraudScoreResult scoreTransaction(FraudScoreRequest request, java.util.UUID transactionId) {
        FraudScoreResult result = ruleEngine.evaluate(request);

        log.info("Post-hoc fraud score for txn={} user={}: score={} action={}",
                transactionId, request.userId(), result.score(), result.action());

        if (result.score() >= 30) {
            FraudAlert alert = FraudAlert.create(request.userId(), transactionId, result);
            alertRepository.save(alert);

            if (result.score() >= 80) {
                eventPublisher.publishFraudAlert(alert);
            }
        }

        return result;
    }
}
