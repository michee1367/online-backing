package com.villagesat.mobilemoney.adapter.out.wallet;

import com.villagesat.mobilemoney.domain.port.out.WalletCreditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedWalletCreditAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedWalletCreditAdapter.class);

    //@Override
    public void creditWallet(UUID walletId, BigDecimal amount, String currency, UUID reference) {
        log.info("Simulated wallet credit: walletId={} amount={} {} ref={}",
                walletId, amount, currency, reference);
    }
}
