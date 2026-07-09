package com.villagesat.wallet.domain.port.out;

import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.Wallet;

import java.util.UUID;

public interface WalletEventPublisher {

    void publishWalletCreated(Wallet wallet);

    void publishBalanceUpdated(UUID walletId, Balance balance);

    void publishLimitsUpdated(Wallet wallet, int previousKycLevel);
}
