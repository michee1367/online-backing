package com.villagesat.wallet.adapter.in.web;

import com.villagesat.wallet.domain.port.in.BalanceUseCase;
import com.villagesat.wallet.domain.port.in.WalletKycUseCase;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Endpoints internes — accessibles uniquement via service mesh (mTLS).
 * Non exposés via API Gateway public.
 */
@RestController
@RequestMapping("/internal/wallets")
public class InternalWalletController {

    private final BalanceUseCase balanceUseCase;
    private final WalletKycUseCase walletKycUseCase;
    private final WalletUseCase walletUseCase;

    public InternalWalletController(BalanceUseCase balanceUseCase,
                                    WalletKycUseCase walletKycUseCase,
                                    WalletUseCase walletUseCase) {
        this.balanceUseCase = balanceUseCase;
        this.walletKycUseCase = walletKycUseCase;
        this.walletUseCase = walletUseCase;
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<InternalWalletResponse> getWallet(@PathVariable("walletId") UUID walletId) {
        var wallet = walletUseCase.getWalletById(walletId);
        return ResponseEntity.ok(new InternalWalletResponse(wallet.id(), wallet.currency()));
    }

    @PostMapping("/{walletId}/debit")
    public ResponseEntity<Void> debit(@PathVariable("walletId") UUID walletId,
                                      @Valid @RequestBody InternalOperationRequest request) {
        balanceUseCase.debit(walletId, request.transactionId(),
                new BigDecimal(request.amount()), request.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{walletId}/credit")
    public ResponseEntity<Void> credit(@PathVariable("walletId") UUID walletId,
                                       @Valid @RequestBody InternalOperationRequest request) {
        balanceUseCase.credit(walletId, request.transactionId(),
                new BigDecimal(request.amount()), request.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/kyc-limits")
    public ResponseEntity<KycLimitsResponse> applyKycLimits(@PathVariable("userId") UUID userId,
                                                            @Valid @RequestBody ApplyKycLimitsRequest request) {
        var wallets = walletKycUseCase.applyKycLimits(userId, request.kycLevel());
        return ResponseEntity.ok(new KycLimitsResponse(userId, request.kycLevel(), wallets.size()));
    }

    public record ApplyKycLimitsRequest(@NotNull Integer kycLevel) {}

    public record KycLimitsResponse(UUID userId, int kycLevel, int walletsUpdated) {}

    public record InternalWalletResponse(UUID walletId, String currency) {}

    public record InternalOperationRequest(
            @NotNull UUID transactionId,
            @NotBlank String amount,
            String description
    ) {}
}
