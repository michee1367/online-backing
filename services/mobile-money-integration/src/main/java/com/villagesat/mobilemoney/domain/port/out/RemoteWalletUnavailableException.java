package com.villagesat.mobilemoney.domain.port.out;
import java.util.UUID;

public class RemoteWalletUnavailableException extends WalletBusinessException {
    public RemoteWalletUnavailableException(UUID walletId, String reason) {
        super("Le portefeuille " + walletId + " est indisponible : " + reason);
    }
}
