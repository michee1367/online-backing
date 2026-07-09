package com.villagesat.notification.domain.port.in;

import com.villagesat.notification.domain.model.Notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationUseCase {

    Notification send(SendNotificationCommand command);

    List<Notification> getByUserId(UUID userId);

    Notification getById(UUID id);

    record SendNotificationCommand(
            UUID userId,
            Notification.Channel channel,
            String templateCode,
            String recipientAddress,
            String subject,
            Map<String, String> variables,
            Notification.Priority priority
    ) {}
}
