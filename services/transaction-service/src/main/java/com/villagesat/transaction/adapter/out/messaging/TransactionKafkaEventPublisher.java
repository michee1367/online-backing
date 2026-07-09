package com.villagesat.transaction.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.transaction.domain.model.Transaction;
import com.villagesat.transaction.domain.port.out.TransactionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class TransactionKafkaEventPublisher implements TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionKafkaEventPublisher.class);
    static final String TOPIC = "transaction.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TransactionKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        publish(transaction.id().toString(), "transaction.completed", transaction);
    }

    @Override
    public void publishTransactionFailed(Transaction transaction) {
        publish(transaction.id().toString(), "transaction.failed", transaction);
    }

    private void publish(String key, String eventType, Transaction transaction) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "payload", Map.of(
                            "transactionId", transaction.id(),
                            "status", transaction.status().name(),
                            "amount", transaction.amount().toPlainString(),
                            "currency", transaction.currency(),
                            "sourceWalletId", transaction.sourceWalletId(),
                            "destWalletId", transaction.destWalletId(),
                            "timestamp", Instant.now().toString()
                    )
            ));
            kafkaTemplate.send(TOPIC, key, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction event {}", eventType, e);
        }
    }
}
