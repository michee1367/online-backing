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
public class KycNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(KycNotificationConsumer.class);

    private final NotificationUseCase notificationUseCase;
    private final ObjectMapper objectMapper;

    public KycNotificationConsumer(NotificationUseCase notificationUseCase,
                                   ObjectMapper objectMapper) {
        this.notificationUseCase = notificationUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "kyc.events", groupId = "notification-service")
    public void onKycEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            JsonNode payload = root.path("payload");

            UUID userId = UUID.fromString(payload.path("userId").asText());
            String email = payload.path("email").asText();

            switch (eventType) {
                case "kyc.approved" -> {
                    String level = payload.path("level").asText("1");
                    notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                            userId,
                            Notification.Channel.EMAIL,
                            "kyc.approved",
                            email,
                            "Vérification KYC approuvée",
                            Map.of("level", level),
                            Notification.Priority.NORMAL
                    ));
                    log.info("KYC approved notification sent to user {}", userId);
                }
                case "kyc.rejected" -> {
                    String reason = payload.path("reason").asText("Non spécifié");
                    notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                            userId,
                            Notification.Channel.EMAIL,
                            "kyc.rejected",
                            email,
                            "Vérification KYC rejetée",
                            Map.of("reason", reason),
                            Notification.Priority.NORMAL
                    ));
                    log.info("KYC rejected notification sent to user {}", userId);
                }
                default -> log.debug("Ignoring KYC event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process KYC event: {}", message, e);
        }
    }
}
