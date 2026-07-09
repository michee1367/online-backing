package com.villagesat.mobilemoney.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletCreditPort {
    void creditWallet(UUID walletId, BigDecimal amount, String currency, UUID transactionId)
        throws WalletBusinessException, RemoteWalletUnavailableException, RemoteInsufficientFundsException;
}
