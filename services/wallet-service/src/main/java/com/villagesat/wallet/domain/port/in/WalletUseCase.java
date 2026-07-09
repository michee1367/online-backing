package com.villagesat.wallet.domain.port.in;

import com.villagesat.wallet.domain.model.Wallet;

import java.util.List;
import java.util.UUID;

public interface WalletUseCase {

    Wallet createWallet(CreateWalletCommand command);

    Wallet getWallet(UUID walletId, UUID userId);

    Wallet getWalletById(UUID walletId);

    List<Wallet> listWallets(UUID userId);

    Wallet lookupByAccountNumber(String accountNumber);

    record CreateWalletCommand(UUID userId, String currency, Wallet.WalletType type, String label) {}
}
