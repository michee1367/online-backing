package com.villagesat.compliance.domain.port.out;

import com.villagesat.compliance.domain.model.Screening;

import java.util.List;
import java.util.UUID;

public interface AmlScreeningPort {

    List<Screening> screenUser(UUID userId, UUID kycSubmissionId, String documentNumber);
}
