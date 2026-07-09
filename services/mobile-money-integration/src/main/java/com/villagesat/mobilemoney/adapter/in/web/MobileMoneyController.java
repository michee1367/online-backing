package com.villagesat.mobilemoney.adapter.in.web;

import com.villagesat.mobilemoney.adapter.in.web.dto.DepositRequest;
import com.villagesat.mobilemoney.adapter.in.web.dto.ProviderResponse;
import com.villagesat.mobilemoney.adapter.in.web.dto.TransactionResponse;
import com.villagesat.mobilemoney.adapter.in.web.dto.WithdrawalRequest;
import com.villagesat.mobilemoney.domain.port.in.DepositCommand;
import com.villagesat.mobilemoney.domain.port.in.MobileMoneyUseCase;
import com.villagesat.mobilemoney.domain.port.in.WithdrawalCommand;
import com.villagesat.mobilemoney.domain.port.out.ProviderConfigRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mobile-money")
public class MobileMoneyController {

    private final MobileMoneyUseCase mobileMoneyUseCase;
    private final ProviderConfigRepository providerConfigRepository;

    public MobileMoneyController(MobileMoneyUseCase mobileMoneyUseCase,
                                 ProviderConfigRepository providerConfigRepository) {
        this.mobileMoneyUseCase = mobileMoneyUseCase;
        this.providerConfigRepository = providerConfigRepository;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody DepositRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var command = new DepositCommand(
                userId, request.walletId(), request.provider(),
                request.phoneNumber(), request.amount(), request.currency()
        );
        var tx = mobileMoneyUseCase.initiateDeposit(command);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody WithdrawalRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var command = new WithdrawalCommand(
                userId, request.walletId(), request.provider(),
                request.phoneNumber(), request.amount(), request.currency()
        );
        var tx = mobileMoneyUseCase.initiateWithdrawal(command);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> listTransactions(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var transactions = mobileMoneyUseCase.listTransactions(userId)
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {
        var tx = mobileMoneyUseCase.getTransaction(id);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderResponse>> listProviders() {
        var providers = providerConfigRepository.findAllActive()
                .stream()
                .map(ProviderResponse::from)
                .toList();
        return ResponseEntity.ok(providers);
    }
}
