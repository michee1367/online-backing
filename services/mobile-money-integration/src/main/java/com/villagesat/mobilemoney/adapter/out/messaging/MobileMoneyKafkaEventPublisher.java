package com.villagesat.mobilemoney.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.mobilemoney.domain.model.MobileMoneyTransaction;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MobileMoneyKafkaEventPublisher implements MobileMoneyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyKafkaEventPublisher.class);
    private static final String TOPIC = "mobilemoney.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public MobileMoneyKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishDeposit(MobileMoneyTransaction transaction) {
        publish("mobilemoney.deposit.completed", transaction);
    }

    @Override
    public void publishWithdrawal(MobileMoneyTransaction transaction) {
        publish("mobilemoney.withdrawal.completed", transaction);
    }

    private void publish(String eventType, MobileMoneyTransaction transaction) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "payload", Map.of(
                            "transactionId", transaction.getId().toString(),
                            "userId", transaction.getUserId().toString(),
                            "walletId", transaction.getWalletId().toString(),
                            "provider", transaction.getProvider().name(),
                            "amount", transaction.getAmount().toPlainString(),
                            "currency", transaction.getCurrency(),
                            "externalRef", transaction.getExternalRef()
                    )
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, transaction.getUserId().toString(), json);
            log.info("Published {} for transactionId={}", eventType, transaction.getId());
        } catch (Exception e) {
            log.error("Failed to publish event {}: {}", eventType, e.getMessage(), e);
        }
    }
}
