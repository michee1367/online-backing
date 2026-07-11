package com.villagesat.mobilemoney.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletDebitPort {
    // Remplacement de String reference par UUID transactionId
    void debitWallet(UUID walletId, BigDecimal amount, String currency, String reference) 
        throws WalletBusinessException, RemoteWalletUnavailableException, RemoteInsufficientFundsException;
}
