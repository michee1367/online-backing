package com.villagesat.payment.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.payment.domain.model.Payment;
import com.villagesat.payment.domain.port.out.PaymentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class PaymentKafkaEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaEventPublisher.class);
    static final String TOPIC = "payment.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPaymentCompleted(Payment payment) {
        publish(payment.reference(), "payment.completed", Map.of(
                "paymentId", payment.id(),
                "reference", payment.reference(),
                "merchantId", payment.merchantId(),
                "customerId", payment.customerId(),
                "walletId", payment.walletId(),
                "amount", payment.amount().toPlainString(),
                "fee", payment.fee().toPlainString(),
                "currency", payment.currency(),
                "timestamp", Instant.now().toString()
        ));
    }

    @Override
    public void publishPaymentFailed(Payment payment) {
        publish(payment.reference(), "payment.failed", Map.of(
                "paymentId", payment.id(),
                "reference", payment.reference(),
                "merchantId", payment.merchantId(),
                "amount", payment.amount().toPlainString(),
                "currency", payment.currency(),
                "reason", payment.failedReason() != null ? payment.failedReason() : "unknown",
                "timestamp", Instant.now().toString()
        ));
    }

    private void publish(String key, String eventType, Map<String, Object> payload) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "payload", payload
            ));
            kafkaTemplate.send(TOPIC, key, message);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event {}", eventType, e);
        }
    }
}
