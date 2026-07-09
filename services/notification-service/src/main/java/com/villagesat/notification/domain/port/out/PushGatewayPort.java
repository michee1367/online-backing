package com.villagesat.notification.domain.port.out;

import java.util.UUID;

public interface PushGatewayPort {

    void sendPush(UUID userId, String title, String body);
}
