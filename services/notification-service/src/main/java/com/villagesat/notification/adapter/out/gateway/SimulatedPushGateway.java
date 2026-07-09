package com.villagesat.notification.adapter.out.gateway;

import com.villagesat.notification.domain.port.out.PushGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedPushGateway implements PushGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPushGateway.class);

    @Override
    public void sendPush(UUID userId, String title, String body) {
        log.info("[PUSH SIMULATED] UserId: {} | Title: {} | Body: {}", userId, title, body);
    }
}
