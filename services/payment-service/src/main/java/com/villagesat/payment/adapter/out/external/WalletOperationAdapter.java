package com.villagesat.payment.adapter.out.external;

import com.villagesat.payment.domain.port.out.WalletOperationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adaptateur simulé pour les opérations wallet.
 * En production, cet adaptateur appellera le wallet-service via HTTP ou messaging.
 */
@Component
public class WalletOperationAdapter implements WalletOperationPort {

    private static final Logger log = LoggerFactory.getLogger(WalletOperationAdapter.class);

    @Override
    public void debitCustomer(UUID walletId, BigDecimal amount, String reference) {
        log.info("[SIMULATED] Debit customer wallet {}: amount={}, reference={}",
                walletId, amount, reference);
    }

    @Override
    public void creditMerchant(UUID merchantId, BigDecimal amount, String reference) {
        log.info("[SIMULATED] Credit merchant {}: amount={}, reference={}",
                merchantId, amount, reference);
    }
}
