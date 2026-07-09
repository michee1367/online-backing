package com.villagesat.notification.domain.port.out;

public interface EmailGatewayPort {

    void sendEmail(String to, String subject, String body);
}
