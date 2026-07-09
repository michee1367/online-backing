package com.villagesat.mobilemoney.adapter.out.gateway;

import com.villagesat.mobilemoney.domain.model.MobileMoneyProvider;
import com.villagesat.mobilemoney.domain.port.out.MobileMoneyGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedMobileMoneyGateway implements MobileMoneyGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedMobileMoneyGateway.class);

    @Override
    public GatewayResponse sendRequest(MobileMoneyProvider provider, GatewayRequest request) {
        long delay = ThreadLocalRandom.current().nextLong(100, 500);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String providerRef = provider.name() + "-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Simulated {} response for ref={} providerRef={} delay={}ms",
                provider, request.externalRef(), providerRef, delay);

        return new GatewayResponse(true, providerRef, null);
    }
}
