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
public class WalletNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletNotificationConsumer.class);

    private final NotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;

    public WalletNotificationConsumer(NotificationUseCase notificationUseCase,
                                      ObjectMapper objectMapper) {
        this.notificationUseCase = notificationUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet.events", groupId = "notification-service")
    public void onWalletEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"wallet.created".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");

            UUID userId = UUID.fromString(payload.path("userId").asText());
            String phone = payload.path("phone").asText();
            String currency = payload.path("currency").asText();
            String accountNumber = payload.path("accountNumber").asText();

            notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                    userId,
                    Notification.Channel.SMS,
                    "wallet.created",
                    phone,
                    null,
                    Map.of("currency", currency, "accountNumber", accountNumber),
                    Notification.Priority.NORMAL
            ));

            log.info("Welcome notification sent for new wallet of user {}", userId);
        } catch (Exception e) {
            log.error("Failed to process wallet event: {}", message, e);
        }
    }
}
