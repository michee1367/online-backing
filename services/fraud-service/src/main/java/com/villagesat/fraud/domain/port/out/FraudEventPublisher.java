package com.villagesat.fraud.domain.port.out;

import com.villagesat.fraud.domain.model.FraudAlert;

public interface FraudEventPublisher {

    void publishFraudAlert(FraudAlert alert);
}
