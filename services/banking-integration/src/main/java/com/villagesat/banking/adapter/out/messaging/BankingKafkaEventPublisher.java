package com.villagesat.banking.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.model.LinkedBankAccount;
import com.villagesat.banking.domain.port.out.BankingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BankingKafkaEventPublisher implements BankingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BankingKafkaEventPublisher.class);
    private static final String TOPIC = "banking.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BankingKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishTransferCompleted(BankTransfer transfer) {
        publish("banking.transfer.completed", transfer.getUserId().toString(), Map.of(
                "transferId", transfer.getId().toString(),
                "userId", transfer.getUserId().toString(),
                "walletId", transfer.getWalletId().toString(),
                "amount", transfer.getAmount().toPlainString(),
                "currency", transfer.getCurrency(),
                "reference", transfer.getReference()
        ));
    }

    @Override
    public void publishTransferFailed(BankTransfer transfer) {
        publish("banking.transfer.failed", transfer.getUserId().toString(), Map.of(
                "transferId", transfer.getId().toString(),
                "userId", transfer.getUserId().toString(),
                "reference", transfer.getReference(),
                "reason", transfer.getFailedReason() != null ? transfer.getFailedReason() : "unknown"
        ));
    }

    @Override
    public void publishAccountLinked(LinkedBankAccount account) {
        publish("banking.account.linked", account.getUserId().toString(), Map.of(
                "accountId", account.getId().toString(),
                "userId", account.getUserId().toString(),
                "bankName", account.getBankName(),
                "currency", account.getCurrency()
        ));
    }

    private void publish(String eventType, String key, Map<String, Object> payload) {
        try {
            Map<String, Object> event = Map.of("eventType", eventType, "payload", payload);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, key, json);
            log.info("Published {}", eventType);
        } catch (Exception e) {
            log.error("Failed to publish event {}: {}", eventType, e.getMessage(), e);
        }
    }
}
