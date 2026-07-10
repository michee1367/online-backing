package com.villagesat.wallet.domain.port.in;

import com.villagesat.wallet.domain.model.Wallet;

import java.util.List;
import java.util.UUID;

public interface WalletUseCase {

    Wallet createWallet(CreateWalletCommand command);

    Wallet getSystemWallet(String currency);

    Wallet getUserWallet(UUID userId, String currency, Wallet.WalletType walletType);

    Wallet getWallet(UUID walletId, UUID userId);

    List<Wallet> listWallets(UUID userId);

    Wallet lookupByAccountNumber(String accountNumber);

    record CreateWalletCommand(UUID userId, String currency, Wallet.WalletType type, String label) {}
}
