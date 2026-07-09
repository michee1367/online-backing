package com.villagesat.notification.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.notification.domain.model.Notification;
import com.villagesat.notification.domain.port.in.NotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class TransactionNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionNotificationConsumer.class);

    private final NotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;

    public TransactionNotificationConsumer(NotificationUseCase notificationUseCase,
                                           ObjectMapper objectMapper) {
        this.notificationUseCase = notificationUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction.events", groupId = "notification-service")
    public void onTransactionEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"transaction.completed".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");

            UUID senderId = UUID.fromString(payload.path("senderId").asText());
            UUID receiverId = UUID.fromString(payload.path("receiverId").asText());
            String amount = payload.path("amount").asText();
            String currency = payload.path("currency").asText();
            String fee = payload.path("fee").asText("0");
            String senderPhone = payload.path("senderPhone").asText();
            String receiverPhone = payload.path("receiverPhone").asText();
            String destination = payload.path("destination").asText(receiverPhone);
            String senderName = payload.path("senderName").asText();

            notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                    senderId,
                    Notification.Channel.SMS,
                    "txn.completed.sender",
                    senderPhone,
                    null,
                    Map.of("amount", amount, "currency", currency,
                            "destination", destination, "fee", fee),
                    Notification.Priority.HIGH
            ));

            notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                    receiverId,
                    Notification.Channel.SMS,
                    "txn.completed.receiver",
                    receiverPhone,
                    null,
                    Map.of("amount", amount, "currency", currency, "sender", senderName),
                    Notification.Priority.HIGH
            ));

            log.info("Transaction notifications sent for sender={} receiver={}", senderId, receiverId);
        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", message, e);
        }
    }
}
