package com.villagesat.reporting.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.reporting.adapter.out.persistence.entity.ReportRequestEntity;
import com.villagesat.reporting.domain.model.*;
import com.villagesat.reporting.domain.port.out.ReportRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ReportRepositoryAdapter(ReportJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportRequest save(ReportRequest request) {
        ReportRequestEntity entity = toEntity(request);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ReportRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ReportRequest> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDomain).toList();
    }

    private ReportRequestEntity toEntity(ReportRequest request) {
        ReportRequestEntity entity = new ReportRequestEntity();
        entity.setId(request.getId());
        entity.setUserId(request.getUserId());
        entity.setReportType(request.getReportType().name());
        entity.setStatus(request.getStatus().name());
        entity.setFileUrl(request.getFileUrl());
        entity.setFormat(request.getFormat().name());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setCompletedAt(request.getCompletedAt());
        entity.setFailedReason(request.getFailedReason());
        entity.setVersion(request.getVersion());
        try {
            entity.setParameters(request.getParameters() != null
                    ? objectMapper.writeValueAsString(request.getParameters()) : "{}");
        } catch (Exception e) {
            entity.setParameters("{}");
        }
        return entity;
    }

    private ReportRequest toDomain(ReportRequestEntity entity) {
        Map<String, String> params = new HashMap<>();
        try {
            if (entity.getParameters() != null) {
                params = objectMapper.readValue(entity.getParameters(), new TypeReference<>() {});
            }
        } catch (Exception ignored) {}

        return new ReportRequest(
                entity.getId(),
                entity.getUserId(),
                ReportType.valueOf(entity.getReportType()),
                params,
                ReportStatus.valueOf(entity.getStatus()),
                entity.getFileUrl(),
                ReportFormat.valueOf(entity.getFormat()),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getFailedReason(),
                entity.getVersion()
        );
    }
}
