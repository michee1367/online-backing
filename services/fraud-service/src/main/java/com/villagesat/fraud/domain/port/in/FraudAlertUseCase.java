package com.villagesat.fraud.domain.port.in;

import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAlert;

import java.util.List;
import java.util.UUID;

public interface FraudAlertUseCase {

    List<FraudAlert> getAlerts(AlertStatus status);

    FraudAlert resolveAlert(UUID alertId, AlertStatus resolution, UUID resolvedBy, String note);
}
