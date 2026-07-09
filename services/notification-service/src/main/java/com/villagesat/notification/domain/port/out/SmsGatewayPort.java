package com.villagesat.notification.domain.port.out;

public interface SmsGatewayPort {

    void sendSms(String phone, String message);
}
