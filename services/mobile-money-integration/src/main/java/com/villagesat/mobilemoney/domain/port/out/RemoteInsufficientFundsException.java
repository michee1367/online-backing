package com.villagesat.mobilemoney.domain.port.out;
import java.util.UUID;

public class RemoteInsufficientFundsException extends WalletBusinessException {
    public RemoteInsufficientFundsException(UUID walletId) {
        super("Le solde du portefeuille distant " + walletId + " est insuffisant.");
    }
}

