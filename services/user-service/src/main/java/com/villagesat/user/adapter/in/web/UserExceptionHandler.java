package com.villagesat.user.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.user.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(UserService.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "USER_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(UserService.UserException.class)
    public ResponseEntity<ApiError> handleUser(UserService.UserException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                ex.getCode(), ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
