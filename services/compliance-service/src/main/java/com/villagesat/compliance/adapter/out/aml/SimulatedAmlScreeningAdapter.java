package com.villagesat.compliance.adapter.out.aml;

import com.villagesat.compliance.domain.model.Screening;
import com.villagesat.compliance.domain.port.out.AmlScreeningPort;
import com.villagesat.compliance.domain.port.out.ScreeningRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SimulatedAmlScreeningAdapter implements AmlScreeningPort {

    private static final List<String> SANCTIONS_TEST_HITS = List.of("SANCTION_TEST", "OFAC_BLOCKED");

    private final ScreeningRepository screeningRepository;

    public SimulatedAmlScreeningAdapter(ScreeningRepository screeningRepository) {
        this.screeningRepository = screeningRepository;
    }

    @Override
    public List<Screening> screenUser(UUID userId, UUID kycSubmissionId, String documentNumber) {
        List<Screening> results = new ArrayList<>();

        results.add(screen(userId, kycSubmissionId, Screening.ScreeningType.PEP, documentNumber));
        results.add(screen(userId, kycSubmissionId, Screening.ScreeningType.SANCTIONS, documentNumber));
        results.add(screen(userId, kycSubmissionId, Screening.ScreeningType.ADVERSE_MEDIA, documentNumber));

        return results;
    }

    private Screening screen(UUID userId, UUID kycSubmissionId, Screening.ScreeningType type,
                             String documentNumber) {
        Screening.ScreeningResult result = Screening.ScreeningResult.CLEAR;

        if (type == Screening.ScreeningType.SANCTIONS && documentNumber != null) {
            boolean match = SANCTIONS_TEST_HITS.stream()
                    .anyMatch(hit -> documentNumber.toUpperCase().contains(hit));
            if (match) {
                result = Screening.ScreeningResult.MATCH;
            }
        }

        Screening screening = new Screening(
                UUID.randomUUID(),
                userId,
                kycSubmissionId,
                type,
                result,
                "villagesat-aml-simulator",
                Map.of("checkedAt", Instant.now().toString()),
                Instant.now()
        );
        return screeningRepository.save(screening);
    }
}
