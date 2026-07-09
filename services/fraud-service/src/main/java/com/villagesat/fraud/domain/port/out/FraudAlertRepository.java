package com.villagesat.fraud.domain.port.out;

import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudAlertRepository {

    FraudAlert save(FraudAlert alert);

    Optional<FraudAlert> findById(UUID id);

    List<FraudAlert> findByStatus(AlertStatus status);

    List<FraudAlert> findByUserId(UUID userId);
}
