package com.villagesat.compliance.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.compliance.application.service.KycService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class ComplianceExceptionHandler {

    @ExceptionHandler(KycService.ComplianceException.class)
    public ResponseEntity<ApiError> handle(KycService.ComplianceException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "KYC_SANCTIONS_MATCH", "KYC_PENDING_EXISTS" -> HttpStatus.CONFLICT;
            case "KYC_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ApiError(
                ex.getCode(), ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
