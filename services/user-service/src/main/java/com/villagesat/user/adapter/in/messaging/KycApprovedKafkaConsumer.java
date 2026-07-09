package com.villagesat.user.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.user.domain.model.User;
import com.villagesat.user.domain.port.out.KeycloakSyncPort;
import com.villagesat.user.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class KycApprovedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KycApprovedKafkaConsumer.class);

    private final UserRepository userRepository;
    private final KeycloakSyncPort keycloakSync;
    private final ObjectMapper objectMapper;

    public KycApprovedKafkaConsumer(UserRepository userRepository,
                                    KeycloakSyncPort keycloakSync,
                                    ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.keycloakSync = keycloakSync;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "kyc.events", groupId = "user-service-kyc")
    public void onKycEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!"kyc.approved".equals(root.path("eventType").asText())) {
                return;
            }
            JsonNode payload = root.path("payload");
            UUID userId = UUID.fromString(payload.path("userId").asText());
            int level = payload.path("level").asInt(1);

            updateKycLevel(userId, level);
            log.info("Updated KYC level {} for user {}", level, userId);
        } catch (Exception e) {
            log.error("Failed to process KYC event: {}", message, e);
        }
    }

    @Transactional
    void updateKycLevel(UUID userId, int level) {
        userRepository.findById(userId).ifPresent(user -> {
            User updated = user.withKycLevel(level);
            if (level >= 1 && user.status() == User.UserStatus.PENDING_VERIFICATION) {
                updated = updated.withStatus(User.UserStatus.ACTIVE);
                keycloakSync.updateStatus(userId, User.UserStatus.ACTIVE.name());
            }
            userRepository.save(updated);
            keycloakSync.updateKycLevel(userId, level);
        });
    }
}
