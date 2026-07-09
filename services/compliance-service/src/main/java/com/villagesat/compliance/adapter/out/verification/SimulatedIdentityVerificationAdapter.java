package com.villagesat.compliance.adapter.out.verification;

import com.villagesat.compliance.domain.port.out.IdentityVerificationPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulateur de vérification d'identité (Onfido/Jumio).
 * En production : remplacer par un adapter HTTP vers le fournisseur KYC.
 */
@Component
public class SimulatedIdentityVerificationAdapter implements IdentityVerificationPort {

    @Override
    public VerificationResult verify(VerificationRequest request) {
        boolean hasSelfie = request.selfieKey() != null && !request.selfieKey().isBlank();
        boolean hasDoc = request.documentFrontKey() != null && !request.documentFrontKey().isBlank();

        if (!hasDoc || !hasSelfie) {
            return new VerificationResult(
                    BigDecimal.valueOf(0.35), "sim-" + request.userId(), false);
        }

        double raw = 0.75 + ThreadLocalRandom.current().nextDouble() * 0.24;
        BigDecimal score = BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
        return new VerificationResult(score, "sim-provider-" + request.userId(), true);
    }
}
