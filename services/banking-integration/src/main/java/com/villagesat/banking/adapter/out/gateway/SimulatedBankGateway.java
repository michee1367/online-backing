package com.villagesat.banking.adapter.out.gateway;

import com.villagesat.banking.domain.model.BankTransfer;
import com.villagesat.banking.domain.port.out.BankGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedBankGateway implements BankGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedBankGateway.class);

    @Override
    public TransferResponse initiateSwiftTransfer(BankTransfer transfer) {
        long delay = ThreadLocalRandom.current().nextLong(200, 800);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String externalRef = "SWIFT-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        log.info("Simulated SWIFT transfer: ref={} externalRef={} amount={} {} delay={}ms",
                transfer.getReference(), externalRef, transfer.getAmount(), transfer.getCurrency(), delay);

        return new TransferResponse(true, externalRef, null);
    }

    @Override
    public TransferStatusResponse checkTransferStatus(String externalRef) {
        log.info("Simulated status check for externalRef={}", externalRef);
        return new TransferStatusResponse("COMPLETED", externalRef);
    }
}
