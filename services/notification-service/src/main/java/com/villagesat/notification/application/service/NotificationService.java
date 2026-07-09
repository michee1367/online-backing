package com.villagesat.notification.application.service;

import com.villagesat.notification.domain.model.Notification;
import com.villagesat.notification.domain.port.in.NotificationUseCase;
import com.villagesat.notification.domain.port.out.EmailGatewayPort;
import com.villagesat.notification.domain.port.out.NotificationRepository;
import com.villagesat.notification.domain.port.out.PushGatewayPort;
import com.villagesat.notification.domain.port.out.SmsGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final TemplateResolver templateResolver;
    private final SmsGatewayPort smsGateway;
    private final EmailGatewayPort emailGateway;
    private final PushGatewayPort pushGateway;

    public NotificationService(NotificationRepository notificationRepository,
                               TemplateResolver templateResolver,
                               SmsGatewayPort smsGateway,
                               EmailGatewayPort emailGateway,
                               PushGatewayPort pushGateway) {
        this.notificationRepository = notificationRepository;
        this.templateResolver = templateResolver;
        this.smsGateway = smsGateway;
        this.emailGateway = emailGateway;
        this.pushGateway = pushGateway;
    }

    @Override
    public Notification send(SendNotificationCommand command) {
        String body = resolveBody(command.templateCode(), command.variables());
        String subject = command.subject();

        Notification notification = new Notification(
                UUID.randomUUID(),
                command.userId(),
                command.channel(),
                command.templateCode(),
                command.recipientAddress(),
                subject,
                body,
                Notification.Status.PENDING,
                command.priority() != null ? command.priority() : Notification.Priority.NORMAL,
                command.variables() != null ? command.variables() : Map.of(),
                Instant.now(),
                null,
                null
        );

        notification = notificationRepository.save(notification);

        try {
            dispatch(notification);
            notification = notification.markSent(Instant.now());
            log.info("Notification {} sent via {} to {}", notification.id(), notification.channel(),
                    notification.recipientAddress());
        } catch (Exception e) {
            notification = notification.markFailed(e.getMessage());
            log.error("Failed to send notification {} via {}: {}", notification.id(),
                    notification.channel(), e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getById(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    private String resolveBody(String templateCode, Map<String, String> variables) {
        if (templateCode != null && variables != null) {
            String resolved = templateResolver.resolve(templateCode, variables);
            if (resolved != null) {
                return resolved;
            }
        }
        return variables != null ? variables.getOrDefault("body", "") : "";
    }

    private void dispatch(Notification notification) {
        switch (notification.channel()) {
            case SMS -> smsGateway.sendSms(notification.recipientAddress(), notification.body());
            case EMAIL -> emailGateway.sendEmail(notification.recipientAddress(),
                    notification.subject(), notification.body());
            case PUSH -> pushGateway.sendPush(notification.userId(),
                    notification.subject(), notification.body());
        }
    }

    public static class NotificationNotFoundException extends RuntimeException {
        public NotificationNotFoundException(UUID id) {
            super("Notification not found: %s".formatted(id));
        }
    }
}
