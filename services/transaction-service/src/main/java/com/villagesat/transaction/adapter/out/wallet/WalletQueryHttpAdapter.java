package com.villagesat.transaction.adapter.out.wallet;

import com.villagesat.transaction.domain.port.out.WalletQueryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WalletQueryHttpAdapter implements WalletQueryPort {

    private final WalletClient walletClient;

    public WalletQueryHttpAdapter(WalletClient walletClient) {
        this.walletClient = walletClient;
    }

    @Override
    public WalletInfo findById(UUID walletId) {
        WalletClient.WalletResponse response = walletClient.getWallet(walletId);
        return new WalletInfo(response.walletId(), response.currency());
    }
}
