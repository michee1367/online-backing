package com.villagesat.payment.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.payment.application.service.MerchantService;
import com.villagesat.payment.application.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(MerchantService.MerchantNotFoundException.class)
    public ResponseEntity<ApiError> handleMerchantNotFound(MerchantService.MerchantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "MERCHANT_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(MerchantService.DuplicateMerchantException.class)
    public ResponseEntity<ApiError> handleDuplicateMerchant(MerchantService.DuplicateMerchantException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                "MERCHANT_DUPLICATE", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(PaymentService.PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentService.PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "PAYMENT_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(PaymentService.InvalidPaymentStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(PaymentService.InvalidPaymentStateException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError(
                "INVALID_PAYMENT_STATE", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(PaymentService.MerchantNotActiveException.class)
    public ResponseEntity<ApiError> handleMerchantNotActive(PaymentService.MerchantNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "MERCHANT_NOT_ACTIVE", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(PaymentService.UnauthorizedPaymentAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorized(PaymentService.UnauthorizedPaymentAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "PAYMENT_UNAUTHORIZED", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
