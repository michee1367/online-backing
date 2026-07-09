package com.villagesat.banking.adapter.in.web;

import com.villagesat.banking.adapter.in.web.dto.BankTransferResponse;
import com.villagesat.banking.adapter.in.web.dto.InitiateTransferRequest;
import com.villagesat.banking.adapter.in.web.dto.LinkAccountRequest;
import com.villagesat.banking.adapter.in.web.dto.LinkedAccountResponse;
import com.villagesat.banking.domain.port.in.BankingUseCase;
import com.villagesat.banking.domain.port.in.InitiateTransferCommand;
import com.villagesat.banking.domain.port.in.LinkAccountCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {

    private final BankingUseCase bankingUseCase;

    public BankingController(BankingUseCase bankingUseCase) {
        this.bankingUseCase = bankingUseCase;
    }

    @PostMapping("/accounts/link")
    public ResponseEntity<LinkedAccountResponse> linkAccount(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody LinkAccountRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var command = new LinkAccountCommand(
                userId, request.bankName(), request.bankCode(),
                request.accountNumber(), request.accountHolderName(), request.currency()
        );
        var account = bankingUseCase.linkAccount(command);
        return ResponseEntity.ok(LinkedAccountResponse.from(account));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<LinkedAccountResponse>> listAccounts(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var accounts = bankingUseCase.listAccounts(userId)
                .stream()
                .map(LinkedAccountResponse::from)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        bankingUseCase.deleteAccount(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfers/initiate")
    public ResponseEntity<BankTransferResponse> initiateTransfer(@AuthenticationPrincipal Jwt jwt,
                                                                  @Valid @RequestBody InitiateTransferRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var command = new InitiateTransferCommand(
                userId, request.walletId(), request.linkedAccountId(),
                request.direction(), request.transferType(),
                request.amount(), request.currency(), request.swiftCode()
        );
        var transfer = bankingUseCase.initiateTransfer(command);
        return ResponseEntity.ok(BankTransferResponse.from(transfer));
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<BankTransferResponse>> listTransfers(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var transfers = bankingUseCase.listTransfers(userId)
                .stream()
                .map(BankTransferResponse::from)
                .toList();
        return ResponseEntity.ok(transfers);
    }

    @GetMapping("/transfers/{id}")
    public ResponseEntity<BankTransferResponse> getTransfer(@PathVariable UUID id) {
        var transfer = bankingUseCase.getTransfer(id);
        return ResponseEntity.ok(BankTransferResponse.from(transfer));
    }
}
