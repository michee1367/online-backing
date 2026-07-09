package com.villagesat.transaction.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.transaction.application.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transferts et opérations financières")
public class TransactionController {

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') and @securityUtils.isTransferMfaVerified()")
    @Operation(summary = "Transfert interne P2P", description = "Requiert Idempotency-Key header")
    public ResponseEntity<TransferService.TransferResult> transfer(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        SecurityUtils.getCurrentUserId(); // Ensure authenticated

        var result = transferService.executeTransfer(new TransferService.TransferCommand(
                idempotencyKey,
                request.sourceWalletId(),
                request.destinationWalletId(),
                new BigDecimal(request.amount()),
                request.currency(),
                request.description()));

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    public record TransferRequest(
            @NotNull UUID sourceWalletId,
            @NotNull UUID destinationWalletId,
            @NotBlank @Pattern(regexp = "^\\d+(\\.\\d{1,4})?$") String amount,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            String description
    ) {}
}
