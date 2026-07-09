package com.villagesat.fraud.domain.port.in;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.FraudScoreResult;

public interface FraudScoringUseCase {

    FraudScoreResult score(FraudScoreRequest request);
}
