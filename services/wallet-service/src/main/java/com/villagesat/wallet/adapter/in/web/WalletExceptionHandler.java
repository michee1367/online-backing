package com.villagesat.wallet.adapter.in.web;

import com.villagesat.common.error.ApiError;
import com.villagesat.wallet.application.service.BalanceService;
import com.villagesat.wallet.application.service.WalletService;
import com.villagesat.wallet.domain.model.InsufficientFundsException;
import com.villagesat.wallet.domain.model.TransactionLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class WalletExceptionHandler {

    @ExceptionHandler(BalanceService.WalletNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BalanceService.WalletNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "WALLET_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError(
                "WALLET_INSUFFICIENT_FUNDS", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(BalanceService.WalletFrozenException.class)
    public ResponseEntity<ApiError> handleFrozen(BalanceService.WalletFrozenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                "ACCOUNT_FROZEN", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(WalletService.DuplicateWalletException.class)
    public ResponseEntity<ApiError> handleDuplicate(WalletService.DuplicateWalletException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                "WALLET_DUPLICATE", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(WalletService.InvalidAccountNumberException.class)
    public ResponseEntity<ApiError> handleInvalidAccountNumber(WalletService.InvalidAccountNumberException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                "WALLET_INVALID_ACCOUNT_NUMBER", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(WalletService.WalletNotFoundByAccountNumberException.class)
    public ResponseEntity<ApiError> handleAccountNumberNotFound(WalletService.WalletNotFoundByAccountNumberException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                "WALLET_NOT_FOUND", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }

    @ExceptionHandler(TransactionLimitExceededException.class)
    public ResponseEntity<ApiError> handleLimitExceeded(TransactionLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError(
                "TRANSACTION_LIMIT_EXCEEDED", ex.getMessage(), UUID.randomUUID(), Instant.now()));
    }
}
