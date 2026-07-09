package com.villagesat.compliance.domain.port.in;

import com.villagesat.compliance.domain.model.KycStatusSummary;
import com.villagesat.compliance.domain.model.KycSubmission;

import java.util.UUID;

public interface KycUseCase {

    KycSubmission submit(SubmitKycCommand command);

    KycStatusSummary getStatus(UUID userId);

    record SubmitKycCommand(
            UUID userId,
            int level,
            String documentType,
            String documentNumber,
            String documentFrontKey,
            String documentBackKey,
            String selfieKey
    ) {}
}
