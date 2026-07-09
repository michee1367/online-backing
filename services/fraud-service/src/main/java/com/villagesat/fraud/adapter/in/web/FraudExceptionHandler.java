package com.villagesat.fraud.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.fraud.application.service.FraudAlertService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class FraudExceptionHandler {

    @ExceptionHandler(FraudAlertService.AlertNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(FraudAlertService.AlertNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "FRAUD_ALERT_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(FraudAlertService.AlertAlreadyResolvedException.class)
    public ResponseEntity<ApiError> handleAlreadyResolved(FraudAlertService.AlertAlreadyResolvedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                "FRAUD_ALERT_ALREADY_RESOLVED", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
