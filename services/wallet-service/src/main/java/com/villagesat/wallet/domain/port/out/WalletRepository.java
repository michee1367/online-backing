package com.villagesat.wallet.domain.port.out;

import com.villagesat.wallet.domain.model.Wallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByIdAndUserId(UUID id, UUID userId);

    //Optional<Wallet> findOrCreateWalletSystem();

    List<Wallet> findByUserId(UUID userId);

    boolean existsByUserIdAndCurrency(UUID userId, String currency);
    public Optional<Wallet> findByUserIdAndCurrency(UUID userId, String currency) ;

    boolean existsByAccountNumber(String accountNumber);

    Optional<Wallet> findByAccountNumber(String accountNumber);
}
