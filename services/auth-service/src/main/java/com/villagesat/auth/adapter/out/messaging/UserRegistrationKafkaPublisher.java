package com.villagesat.auth.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.auth.domain.model.RegisteredUser;
import com.villagesat.auth.domain.port.in.AuthUseCase;
import com.villagesat.auth.domain.port.out.UserRegistrationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class UserRegistrationKafkaPublisher implements UserRegistrationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationKafkaPublisher.class);
    static final String TOPIC = "user.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserRegistrationKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishUserRegistered(RegisteredUser user, AuthUseCase.RegisterCommand command) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", "user.created",
                    "payload", Map.of(
                            "userId", user.userId().toString(),
                            "email", user.email(),
                            "phone", command.phone(),
                            "firstName", command.firstName(),
                            "lastName", command.lastName(),
                            "countryCode", command.countryCode(),
                            "kycLevel", user.kycLevel(),
                            "status", user.status(),
                            "timestamp", Instant.now().toString()
                    )
            ));
            kafkaTemplate.send(TOPIC, user.userId().toString(), message);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish user.created event", e);
        }
    }
}
