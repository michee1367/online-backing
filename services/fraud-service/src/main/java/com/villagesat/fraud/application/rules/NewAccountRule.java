package com.villagesat.fraud.application.rules;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class NewAccountRule implements ScoringRule {

    @Override
    public RuleResult evaluate(FraudScoreRequest request) {
        // Simulé : toujours false pour l'instant — sera branché sur le user-service
        boolean isNewAccount = false;
        if (isNewAccount) {
            return new RuleResult("NEW_ACCOUNT", 15, "Compte créé il y a moins de 7 jours");
        }
        return RuleResult.NONE;
    }
}
