package com.villagesat.wallet.domain.port.in;

import com.villagesat.wallet.domain.model.Wallet;

import java.util.List;
import java.util.UUID;

public interface WalletKycUseCase {

    /**
     * Met à jour les plafonds de tous les wallets actifs d'un utilisateur selon son niveau KYC.
     */
    List<Wallet> applyKycLimits(UUID userId, int kycLevel);
}
