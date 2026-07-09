package com.villagesat.transaction.domain.port.out;

import java.util.UUID;

public interface WalletQueryPort {

    WalletInfo findById(UUID walletId);

    record WalletInfo(UUID id, String currency) {}
}
