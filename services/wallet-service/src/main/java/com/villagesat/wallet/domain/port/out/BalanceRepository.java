package com.villagesat.wallet.domain.port.out;

import com.villagesat.wallet.domain.model.Balance;

import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository {

    Balance save(Balance balance);

    Optional<Balance> findByWalletId(UUID walletId);

    Optional<Balance> findByWalletIdForUpdate(UUID walletId);

    java.util.List<Balance> findByWalletIdIn(java.util.Collection<UUID> walletIds);
}
