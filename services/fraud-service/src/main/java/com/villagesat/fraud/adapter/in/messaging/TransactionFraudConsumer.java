package com.villagesat.fraud.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.fraud.application.service.FraudScoringService;
import com.villagesat.fraud.domain.model.FraudScoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class TransactionFraudConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionFraudConsumer.class);

    private final FraudScoringService fraudScoringService;
    private final ObjectMapper objectMapper;

    public TransactionFraudConsumer(FraudScoringService fraudScoringService,
                                    ObjectMapper objectMapper) {
        this.fraudScoringService = fraudScoringService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction.events", groupId = "fraud-service-txn")
    public void onTransactionEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if (!"transaction.completed".equals(eventType)) {
                return;
            }

            JsonNode payload = root.path("payload");
            UUID transactionId = UUID.fromString(payload.path("transactionId").asText());
            UUID userId = UUID.fromString(payload.path("userId").asText());
            UUID walletId = parseUuidOrNull(payload.path("walletId").asText(null));
            BigDecimal amount = new BigDecimal(payload.path("amount").asText("0"));
            String currency = payload.path("currency").asText("CDF");

            FraudScoreRequest request = new FraudScoreRequest(
                    userId, walletId, amount, currency, null, null, Instant.now());

            fraudScoringService.scoreTransaction(request, transactionId);
            log.info("Post-hoc fraud analysis completed for transaction {}", transactionId);
        } catch (Exception e) {
            log.error("Failed to process transaction event for fraud analysis: {}", message, e);
        }
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
