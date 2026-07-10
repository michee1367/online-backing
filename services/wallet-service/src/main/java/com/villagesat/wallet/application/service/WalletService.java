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
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/*
  uuid-wallet:
    user-system: 11111111-1111-1111-8111-111111111111
    wallet-usd-system: 11111111-1111-4111-8111-111111111111
    wallet-cdf-system: 11111111-1111-5111-8111-111111111111 */

@Service
@Transactional
public class WalletService implements WalletUseCase {

    private static final int ACCOUNT_NUMBER_MAX_ATTEMPTS = 20;

    private final WalletRepository walletRepository;
    private final BalanceRepository balanceRepository;
    private final WalletEventPublisher eventPublisher;

    private final String uuidUserSystem;
    private final String uuidWalletUSD;
    private final String uuidWalletCDF;


    public WalletService(WalletRepository walletRepository,
                         BalanceRepository balanceRepository,
                         WalletEventPublisher eventPublisher,
                        @Value("${villagesat.uuid-wallet.user-system}") String uuidUserSystem,
                        @Value("${villagesat.uuid-wallet.wallet-usd-system}") String uuidWalletUSD,
                        @Value("${villagesat.uuid-wallet.wallet-cdf-system}") String uuidWalletCDF) 
    {

        this.walletRepository = walletRepository;
        this.balanceRepository = balanceRepository;
        this.eventPublisher = eventPublisher;

        this.uuidUserSystem = uuidUserSystem;
        this.uuidWalletUSD = uuidWalletUSD;
        this.uuidWalletCDF = uuidWalletCDF;

    }

    @Override
    public Wallet createWallet(CreateWalletCommand command) {

        if(command.type().equals(Wallet.WalletType.SYSTEM)) {
            throw new NotCreateSystemException();
        }
        
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
    public Wallet getSystemWallet(String currency) {
        // 1. Protection contre le NullPointerException si currency est null
        if (currency == null) {
            throw new InvalidCurrencyException();
        }

        String walletStrId;
        String label;

        // 2. Utilisation de .equalsIgnoreCase() pour simplifier et sécuriser
        if (currency.equalsIgnoreCase("USD")) {
            walletStrId = this.uuidWalletUSD;
            label = "Wallet system USD";
        } else if (currency.equalsIgnoreCase("CDF")) {
            walletStrId = this.uuidWalletCDF;
            label = "Wallet system CDF";
        } else {
            throw new InvalidCurrencyException();
        }

        UUID walletId = UUID.fromString(walletStrId);
        UUID uuidUserSystemLocal = UUID.fromString(this.uuidUserSystem);

        // 3. Récupération avec le .orElse(null) qu'on a vu ensemble
        Wallet wallet = walletRepository.findByIdAndUserId(walletId, uuidUserSystemLocal).orElse(null);

        // 4. Initialisation à la demande si le wallet n'existe pas encore
        if (wallet == null) {
            WalletKycLimits limits = WalletKycLimits.forLevel(3);
            
            wallet = new Wallet(
                    walletId,
                    uuidUserSystemLocal, // Réutilisation de la variable déjà castée en UUID
                    generateUniqueAccountNumber(),
                    currency.toUpperCase(), // On s'assure d'enregistrer en majuscules
                    Wallet.WalletType.SYSTEM,
                    label,
                    Wallet.WalletStatus.ACTIVE,
                    0,
                    limits.dailyLimit(),
                    limits.monthlyLimit(),
                    Instant.now(),
                    0L
            );

            wallet = walletRepository.save(wallet);

            // Attention ici : utilisez wallet.getId() ou wallet.id() selon votre implémentation de l'entité
            Balance initialBalance = new Balance(
                    wallet.id(), 
                    BigDecimal.ZERO, 
                    BigDecimal.ZERO,
                    BigDecimal.ZERO, 
                    null, 
                    0L
            );
            balanceRepository.save(initialBalance);

            eventPublisher.publishWalletCreated(wallet);
        }
        
        return wallet;
    }

    
    
    @Override
    public Wallet getUserWallet(UUID userId, String currency, Wallet.WalletType walletType) {

        if(walletType.equals(Wallet.WalletType.SYSTEM)) {
            throw new NotCreateSystemException();
        }
        // 1. Protection contre le NullPointerException si currency est null
        if (currency == null) {
            throw new InvalidCurrencyException();
        }

        String label = "Wallet for %s and %s".formatted(userId.toString(), currency);


        // 3. Récupération avec le .orElse(null) qu'on a vu ensemble
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, currency.toUpperCase()).orElse(null);


        // 4. Initialisation à la demande si le wallet n'existe pas encore
        if (wallet == null) {
            WalletKycLimits limits = WalletKycLimits.forLevel(0);
            UUID walletId = UUID.randomUUID();
            
            wallet = new Wallet(
                    walletId,
                    userId, // Réutilisation de la variable déjà castée en UUID
                    generateUniqueAccountNumber(),
                    currency.toUpperCase(), // On s'assure d'enregistrer en majuscules
                    walletType,
                    label,
                    Wallet.WalletStatus.ACTIVE,
                    0,
                    limits.dailyLimit(),
                    limits.monthlyLimit(),
                    Instant.now(),
                    0L
            );

            wallet = walletRepository.save(wallet);

            // Attention ici : utilisez wallet.getId() ou wallet.id() selon votre implémentation de l'entité
            Balance initialBalance = new Balance(
                    wallet.id(), 
                    BigDecimal.ZERO, 
                    BigDecimal.ZERO,
                    BigDecimal.ZERO, 
                    null, 
                    0L
            );
            balanceRepository.save(initialBalance);

            eventPublisher.publishWalletCreated(wallet);
        }
        
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
    public static class NotCreateSystemException extends RuntimeException {
        public NotCreateSystemException() {
            super("Vous ne pouvez pas créer un wallet system");
        }
    }
    public static class InvalidCurrencyException extends RuntimeException {
        public InvalidCurrencyException() {
            super("La devise n'existe pas. Veuillez mettre soit USD soit CDF ");
        }
    }
}
