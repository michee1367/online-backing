package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.model.WalletAccountNumberGenerator;
import com.villagesat.wallet.domain.model.WalletKycLimits;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import com.villagesat.wallet.domain.port.out.BalanceRepository;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WalletService implements WalletUseCase {

    private static final int ACCOUNT_NUMBER_MAX_ATTEMPTS = 20;

    private final WalletRepository walletRepository;
    private final BalanceRepository balanceRepository;
    private final WalletEventPublisher eventPublisher;

    public WalletService(WalletRepository walletRepository,
                         BalanceRepository balanceRepository,
                         WalletEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.balanceRepository = balanceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Wallet createWallet(CreateWalletCommand command) {
        if (walletRepository.existsByUserIdAndCurrency(command.userId(), command.currency())) {
            throw new DuplicateWalletException(command.userId(), command.currency());
        }

        WalletKycLimits limits = WalletKycLimits.forLevel(0);
        Wallet wallet = new Wallet(
                UUID.randomUUID(),
                command.userId(),
                generateUniqueAccountNumber(),
                command.currency().toUpperCase(),
                command.type(),
                command.label(),
                Wallet.WalletStatus.ACTIVE,
                0,
                limits.dailyLimit(),
                limits.monthlyLimit(),
                Instant.now(),
                0L
        );

        wallet = walletRepository.save(wallet);

        Balance initialBalance = new Balance(wallet.id(), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, 0L);
        balanceRepository.save(initialBalance);

        eventPublisher.publishWalletCreated(wallet);
        return wallet;
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWallet(UUID walletId, UUID userId) {
        return walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new BalanceService.WalletNotFoundException(walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Wallet> listWallets(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet lookupByAccountNumber(String accountNumber) {
        if (!WalletAccountNumberGenerator.isValid(accountNumber)) {
            throw new InvalidAccountNumberException(accountNumber);
        }
        return walletRepository.findByAccountNumber(accountNumber)
                .filter(w -> w.status() == Wallet.WalletStatus.ACTIVE)
                .orElseThrow(() -> new WalletNotFoundByAccountNumberException(accountNumber));
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_MAX_ATTEMPTS; attempt++) {
            String accountNumber = WalletAccountNumberGenerator.generate();
            if (!walletRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new IllegalStateException("Impossible de générer un numéro wallet unique");
    }

    public static class DuplicateWalletException extends RuntimeException {
        public DuplicateWalletException(UUID userId, String currency) {
            super("Wallet already exists for user %s in currency %s".formatted(userId, currency));
        }
    }

    public static class InvalidAccountNumberException extends RuntimeException {
        public InvalidAccountNumberException(String accountNumber) {
            super("Numéro wallet invalide : %s".formatted(accountNumber));
        }
    }

    public static class WalletNotFoundByAccountNumberException extends RuntimeException {
        public WalletNotFoundByAccountNumberException(String accountNumber) {
            super("Aucun wallet actif pour le numéro %s".formatted(accountNumber));
        }
    }
}
