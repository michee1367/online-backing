package com.villagesat.audit.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.audit.domain.model.AuditEntry;
import com.villagesat.audit.domain.port.in.AuditUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumer.class);

    private final AuditUseCase auditUseCase;
    private final ObjectMapper objectMapper;

    public AuditKafkaConsumer(AuditUseCase auditUseCase, ObjectMapper objectMapper) {
        this.auditUseCase = auditUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"user.events", "wallet.events", "transaction.events", "kyc.events"},
                   groupId = "audit-service")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);

            AuditEntry entry = new AuditEntry();
            entry.setId(UUID.randomUUID());
            entry.setServiceName(extractServiceName(topic));
            entry.setEventType(getTextOrNull(node, "eventType"));
            entry.setUserId(getUuidOrNull(node, "userId"));
            entry.setEntityType(getTextOrNull(node, "entityType"));
            entry.setEntityId(getTextOrNull(node, "entityId"));
            entry.setAction(getTextOrNull(node, "action"));
            entry.setOldValue(getJsonOrNull(node, "oldValue"));
            entry.setNewValue(getJsonOrNull(node, "newValue"));
            entry.setIpAddress(getTextOrNull(node, "ipAddress"));
            entry.setTimestamp(Instant.now());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("topic", topic);
            if (node.has("metadata") && node.get("metadata").isObject()) {
                node.get("metadata").fields().forEachRemaining(
                        field -> metadata.put(field.getKey(), field.getValue().asText()));
            }
            entry.setMetadata(metadata);

            auditUseCase.logEvent(entry);
            log.debug("Audit entry saved for topic={}, eventType={}", topic, entry.getEventType());

        } catch (Exception e) {
            log.error("Failed to process audit event from topic={}: {}", topic, e.getMessage(), e);
        }
    }

    private String extractServiceName(String topic) {
        return topic.replace(".events", "-service");
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private UUID getUuidOrNull(JsonNode node, String field) {
        String text = getTextOrNull(node, field);
        if (text == null) return null;
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String getJsonOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).toString();
    }
}
