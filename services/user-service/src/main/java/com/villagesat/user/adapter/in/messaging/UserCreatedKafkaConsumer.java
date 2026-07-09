package com.villagesat.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.user.domain.port.in.UserUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserCreatedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedKafkaConsumer.class);

    private final UserUseCase userUseCase;
    private final ObjectMapper objectMapper;

    public UserCreatedKafkaConsumer(UserUseCase userUseCase, ObjectMapper objectMapper) {
        this.userUseCase = userUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user.events", groupId = "user-service")
    public void onUserEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();
            if (!"user.created".equals(eventType)) {
                return;
            }
            JsonNode payload = root.path("payload");
            UUID userId = UUID.fromString(payload.path("userId").asText());

            userUseCase.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                    userId,
                    payload.path("email").asText(),
                    payload.path("phone").asText(null),
                    payload.path("firstName").asText(),
                    payload.path("lastName").asText(),
                    payload.path("countryCode").asText("CD"),
                    payload.path("kycLevel").asInt(0),
                    payload.path("status").asText("PENDING_VERIFICATION")
            ));
            log.info("Provisioned user profile from Kafka: {}", userId);
        } catch (Exception e) {
            log.error("Failed to process user event: {}", message, e);
        }
    }
}
