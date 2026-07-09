package com.villagesat.payment.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletOperationPort {

    void debitCustomer(UUID walletId, BigDecimal amount, String reference);

    void creditMerchant(UUID merchantId, BigDecimal amount, String reference);
}
