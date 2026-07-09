package com.villagesat.fraud.adapter.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.fraud.adapter.out.persistence.entity.FraudAlertEntity;
import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAction;
import com.villagesat.fraud.domain.model.FraudAlert;

import java.util.List;

public final class FraudAlertMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private FraudAlertMapper() {}

    public static FraudAlert toDomain(FraudAlertEntity entity) {
        return new FraudAlert(
                entity.getId(),
                entity.getUserId(),
                entity.getTransactionId(),
                entity.getScore(),
                FraudAction.valueOf(entity.getAction()),
                parseJsonList(entity.getReasons()),
                parseJsonList(entity.getRulesFired()),
                AlertStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getResolvedBy(),
                entity.getResolutionNote(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static FraudAlertEntity toEntity(FraudAlert domain) {
        FraudAlertEntity entity = new FraudAlertEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setTransactionId(domain.transactionId());
        entity.setScore((short) domain.score());
        entity.setAction(domain.action().name());
        entity.setReasons(toJson(domain.reasons()));
        entity.setRulesFired(toJson(domain.rulesFired()));
        entity.setStatus(domain.status().name());
        entity.setCreatedAt(domain.createdAt());
        entity.setResolvedAt(domain.resolvedAt());
        entity.setResolvedBy(domain.resolvedBy());
        entity.setResolutionNote(domain.resolutionNote());
        entity.setVersion(domain.version());
        return entity;
    }

    private static List<String> parseJsonList(String json) {
        try {
            return MAPPER.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static String toJson(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
