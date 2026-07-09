package com.villagesat.compliance.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.out.KycEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class KycKafkaEventPublisher implements KycEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KycKafkaEventPublisher.class);
    static final String TOPIC = "kyc.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KycKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishSubmitted(KycSubmission submission) {
        publish("kyc.submitted", submission);
    }

    @Override
    public void publishApproved(KycSubmission submission) {
        publish("kyc.approved", submission);
    }

    @Override
    public void publishRejected(KycSubmission submission) {
        publish("kyc.rejected", submission);
    }

    private void publish(String eventType, KycSubmission submission) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "payload", Map.of(
                            "submissionId", submission.id().toString(),
                            "userId", submission.userId().toString(),
                            "level", submission.targetLevel(),
                            "status", submission.status().name(),
                            "riskScore", submission.riskScore(),
                            "timestamp", Instant.now().toString()
                    )
            ));
            kafkaTemplate.send(TOPIC, submission.userId().toString(), message);
            log.info("Published {} for user {}", eventType, submission.userId());
        } catch (JsonProcessingException e) {
            log.error("Failed to publish {}", eventType, e);
        }
    }
}
