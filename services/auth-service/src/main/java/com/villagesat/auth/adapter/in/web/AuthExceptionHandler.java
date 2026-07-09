package com.villagesat.auth.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.auth.application.service.AuthService;
import com.villagesat.auth.application.service.MfaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthService.AuthException ex) {
        HttpStatus status = mapStatus(ex.getCode());
        return ResponseEntity.status(status).body(new ApiError(
                ex.getCode(), ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(MfaService.MfaException.class)
    public ResponseEntity<ApiError> handleMfa(MfaService.MfaException ex) {
        HttpStatus status = mapStatus(ex.getCode());
        return ResponseEntity.status(status).body(new ApiError(
                ex.getCode(), ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    private HttpStatus mapStatus(String code) {
        return switch (code) {
            case "AUTH_INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "AUTH_ACCOUNT_LOCKED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AUTH_INVALID_REFRESH_TOKEN", "AUTH_SESSION_INVALID" -> HttpStatus.UNAUTHORIZED;
            case "MFA_INVALID_CODE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "MFA_ALREADY_VERIFIED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
