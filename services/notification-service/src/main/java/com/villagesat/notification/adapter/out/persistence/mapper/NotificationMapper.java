package com.villagesat.notification.adapter.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.notification.adapter.out.persistence.entity.NotificationEntity;
import com.villagesat.notification.domain.model.Notification;

import java.util.Map;

public final class NotificationMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private NotificationMapper() {}

    public static Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getUserId(),
                Notification.Channel.valueOf(entity.getChannel().name()),
                entity.getTemplateCode(),
                entity.getRecipientAddress(),
                entity.getSubject(),
                entity.getBody(),
                Notification.Status.valueOf(entity.getStatus().name()),
                Notification.Priority.valueOf(entity.getPriority().name()),
                parseMetadata(entity.getMetadata()),
                entity.getCreatedAt(),
                entity.getSentAt(),
                entity.getFailedReason()
        );
    }

    public static NotificationEntity toEntity(Notification domain) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setChannel(Notification.Channel.valueOf(domain.channel().name()));
        entity.setTemplateCode(domain.templateCode());
        entity.setRecipientAddress(domain.recipientAddress());
        entity.setSubject(domain.subject());
        entity.setBody(domain.body());
        entity.setStatus(Notification.Status.valueOf(domain.status().name()));
        entity.setPriority(Notification.Priority.valueOf(domain.priority().name()));
        entity.setMetadata(serializeMetadata(domain.metadata()));
        entity.setCreatedAt(domain.createdAt());
        entity.setSentAt(domain.sentAt());
        entity.setFailedReason(domain.failedReason());
        return entity;
    }

    private static Map<String, String> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
