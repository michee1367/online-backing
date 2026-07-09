package com.villagesat.transaction.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.transaction.application.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransferService.FraudBlockedException.class)
    public ResponseEntity<ApiError> handleFraud(TransferService.FraudBlockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "FRAUD_BLOCKED", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(TransferService.DuplicateTransactionException.class)
    public ResponseEntity<ApiError> handleDuplicate(TransferService.DuplicateTransactionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                "TXN_DUPLICATE", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
