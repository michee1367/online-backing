package com.villagesat.notification.adapter.out.gateway;

import com.villagesat.notification.domain.port.out.EmailGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedEmailGateway implements EmailGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailGateway.class);

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[EMAIL SIMULATED] To: {} | Subject: {} | Body: {}", to, subject, body);
    }
}
