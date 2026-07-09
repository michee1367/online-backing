package com.villagesat.fraud.application.service;

import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.port.in.FraudAlertUseCase;
import com.villagesat.fraud.domain.port.out.FraudAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FraudAlertService implements FraudAlertUseCase {

    private final FraudAlertRepository alertRepository;

    public FraudAlertService(FraudAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudAlert> getAlerts(AlertStatus status) {
        return alertRepository.findByStatus(status);
    }

    @Override
    public FraudAlert resolveAlert(UUID alertId, AlertStatus resolution, UUID resolvedBy, String note) {
        FraudAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        if (alert.status() != AlertStatus.OPEN && alert.status() != AlertStatus.INVESTIGATING) {
            throw new AlertAlreadyResolvedException(alertId);
        }

        FraudAlert resolved = alert.resolve(resolution, resolvedBy, note);
        return alertRepository.save(resolved);
    }

    public static class AlertNotFoundException extends RuntimeException {
        public AlertNotFoundException(UUID id) {
            super("Fraud alert not found: " + id);
        }
    }

    public static class AlertAlreadyResolvedException extends RuntimeException {
        public AlertAlreadyResolvedException(UUID id) {
            super("Fraud alert already resolved: " + id);
        }
    }
}
