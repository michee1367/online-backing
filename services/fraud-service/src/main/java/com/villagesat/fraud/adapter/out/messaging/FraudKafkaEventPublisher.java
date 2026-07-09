package com.villagesat.fraud.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.port.out.FraudEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class FraudKafkaEventPublisher implements FraudEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudKafkaEventPublisher.class);
    static final String TOPIC = "fraud.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishFraudAlert(FraudAlert alert) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", "fraud.alert.created",
                    "payload", Map.of(
                            "alertId", alert.id(),
                            "userId", alert.userId(),
                            "transactionId", alert.transactionId() != null ? alert.transactionId().toString() : "",
                            "score", alert.score(),
                            "action", alert.action().name(),
                            "reasons", alert.reasons(),
                            "rulesFired", alert.rulesFired(),
                            "timestamp", Instant.now().toString()
                    )
            ));
            kafkaTemplate.send(TOPIC, alert.userId().toString(), message);
            log.info("Published fraud alert event for alert={} user={} action={}",
                    alert.id(), alert.userId(), alert.action());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize fraud alert event for alert={}", alert.id(), e);
        }
    }
}
