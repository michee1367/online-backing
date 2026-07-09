package com.villagesat.notification.adapter.out.gateway;

import com.villagesat.notification.domain.port.out.SmsGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedSmsGateway implements SmsGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSmsGateway.class);

    @Override
    public void sendSms(String phone, String message) {
        log.info("[SMS SIMULATED] To: {} | Message: {}", phone, message);
    }
}
