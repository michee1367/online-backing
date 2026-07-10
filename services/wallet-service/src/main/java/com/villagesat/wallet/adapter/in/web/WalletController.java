package com.villagesat.wallet.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.wallet.application.service.BalanceService;
import com.villagesat.wallet.application.service.WalletService;
import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import com.villagesat.wallet.domain.port.out.BalanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Gestion des wallets multi-devises")
public class WalletController {

    private final WalletUseCase walletUseCase;
    private final BalanceService balanceService;
    private final BalanceRepository balanceRepository;

    public WalletController(WalletUseCase walletUseCase,
                            BalanceService balanceService,
                            BalanceRepository balanceRepository) {
        this.walletUseCase = walletUseCase;
        this.balanceService = balanceService;
        this.balanceRepository = balanceRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Créer un wallet")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = walletUseCase.createWallet(new WalletUseCase.CreateWalletCommand(
                userId, request.currency(), request.type(), request.label()));
        Balance balance = balanceRepository.findByWalletId(wallet.id())
                .orElse(zeroBalance(wallet.id()));
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.from(wallet, balance));
    }
    @GetMapping("/system/currencies/{currency}")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Créer un wallet")
    public ResponseEntity<WalletResponse> getSystemWallet(@PathVariable("currency") String  currency) {

        //UUID userId = SecurityUtils.getCurrentUserId();

        Wallet wallet = walletUseCase.getSystemWallet(currency);
        Balance balance = balanceRepository.findByWalletId(wallet.id())
                .orElse(zeroBalance(wallet.id()));
                
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.from(wallet, balance));
    }

    @GetMapping("/users/{userId}/currencies/{currency}/types/{type}")
    @PreAuthorize("hasRole('SERVICE')")
    @Operation(summary = "Créer un wallet")
    public ResponseEntity<WalletResponse> getUserWallet(
        @PathVariable("userId") UUID  userId,
        @PathVariable("currency") String  currency,
        @PathVariable("type") Wallet.WalletType  type
    ) {

        //UUID userId = SecurityUtils.getCurrentUserId();

        Wallet wallet = walletUseCase.getUserWallet(userId, currency, type);
        Balance balance = balanceRepository.findByWalletId(wallet.id())
                .orElse(zeroBalance(wallet.id()));
                
        return ResponseEntity.status(HttpStatus.CREATED).body(WalletResponse.from(wallet, balance));
    }

    //getUserWallet(UUID userId, String currency, Wallet.WalletType walletType)

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<WalletResponse> listWallets() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Wallet> wallets = walletUseCase.listWallets(userId);
        Map<UUID, Balance> balances = balanceRepository.findByWalletIdIn(
                        wallets.stream().map(Wallet::id).toList()).stream()
                .collect(Collectors.toMap(Balance::walletId, Function.identity()));
        return wallets.stream()
                .map(w -> WalletResponse.from(w, balances.getOrDefault(w.id(), zeroBalance(w.id()))))
                .toList();
    }

    @GetMapping("/lookup/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Rechercher un wallet par numéro (6 chiffres)")
    public WalletLookupResponse lookupByAccountNumber(
            @PathVariable("accountNumber") @Pattern(regexp = "[1-6]\\d{5}") String accountNumber) {
        Wallet wallet = walletUseCase.lookupByAccountNumber(accountNumber);
        return new WalletLookupResponse(wallet.id(), wallet.accountNumber(), wallet.currency());
    }

    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public WalletResponse getWallet(@PathVariable("walletId") UUID walletId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = walletUseCase.getWallet(walletId, userId);
        Balance balance = balanceService.getBalance(walletId, userId);
        return WalletResponse.from(wallet, balance);
    }

    @GetMapping("/{walletId}/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public BalanceResponse getBalance(@PathVariable("walletId") UUID walletId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = walletUseCase.getWallet(walletId, userId);
        Balance balance = balanceService.getBalance(walletId, userId);
        return BalanceResponse.from(wallet, balance);
    }

    private static Balance zeroBalance(UUID walletId) {
        return new Balance(walletId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0L);
    }

    public record CreateWalletRequest(
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
            Wallet.WalletType type,
            @Size(max = 100) String label
    ) {}

    public record WalletResponse(
            UUID walletId,
            UUID userId,
            String accountNumber,
            String currency,
            String type,
            String label,
            String status,
            int kycLevel,
            String dailyLimit,
            String monthlyLimit,
            String balance,
            String availableBalance,
            String pendingBalance
    ) {
        static WalletResponse from(Wallet w, Balance b) {
            return new WalletResponse(
                    w.id(), w.userId(), w.accountNumber(), w.currency(), w.type().name(), w.label(),
                    w.status().name(), w.kycLevel(), w.dailyLimit().toPlainString(),
                    w.monthlyLimit().toPlainString(),
                    b.balance().toPlainString(), b.availableBalance().toPlainString(),
                    b.pendingBalance().toPlainString());
        }
    }

    public record WalletLookupResponse(
            UUID walletId,
            String accountNumber,
            String currency
    ) {}

    public record BalanceResponse(
            UUID walletId,
            String balance,
            String availableBalance,
            String pendingBalance,
            String currency,
            Instant lastUpdated
    ) {
        static BalanceResponse from(Wallet wallet, Balance balance) {
            Instant lastUpdated = balance.lastTransactionAt() != null
                    ? balance.lastTransactionAt()
                    : Instant.now();
            return new BalanceResponse(
                    balance.walletId(),
                    balance.balance().toPlainString(),
                    balance.availableBalance().toPlainString(),
                    balance.pendingBalance().toPlainString(),
                    wallet.currency(),
                    lastUpdated);
        }
    }
}
