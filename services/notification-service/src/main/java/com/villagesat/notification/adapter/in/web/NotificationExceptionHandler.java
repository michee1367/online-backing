package com.villagesat.notification.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.notification.application.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationService.NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotificationService.NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "NOTIFICATION_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
