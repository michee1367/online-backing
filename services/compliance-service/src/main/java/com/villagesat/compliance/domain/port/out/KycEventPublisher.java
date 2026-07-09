package com.villagesat.compliance.domain.port.out;

import com.villagesat.compliance.domain.model.KycSubmission;

public interface KycEventPublisher {

    void publishSubmitted(KycSubmission submission);

    void publishApproved(KycSubmission submission);

    void publishRejected(KycSubmission submission);
}
