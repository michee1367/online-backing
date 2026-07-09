package com.villagesat.notification.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.notification.domain.model.Notification;
import com.villagesat.notification.domain.port.in.NotificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications multi-canal")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envoyer une notification manuellement (admin)")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        Notification notification = notificationUseCase.send(new NotificationUseCase.SendNotificationCommand(
                request.userId(),
                request.channel(),
                request.templateCode(),
                request.recipientAddress(),
                request.subject(),
                request.variables(),
                request.priority() != null ? request.priority() : Notification.Priority.NORMAL
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(notification));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Lister mes notifications")
    public List<NotificationResponse> listMyNotifications() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return notificationUseCase.getByUserId(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Obtenir une notification par ID")
    public NotificationResponse getNotification(@PathVariable UUID id) {
        return NotificationResponse.from(notificationUseCase.getById(id));
    }

    public record SendNotificationRequest(
            @NotNull UUID userId,
            @NotNull Notification.Channel channel,
            String templateCode,
            @NotBlank String recipientAddress,
            String subject,
            Map<String, String> variables,
            Notification.Priority priority
    ) {}

    public record NotificationResponse(
            UUID id,
            UUID userId,
            String channel,
            String templateCode,
            String recipientAddress,
            String subject,
            String body,
            String status,
            String priority,
            Instant createdAt,
            Instant sentAt,
            String failedReason
    ) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.id(), n.userId(), n.channel().name(), n.templateCode(),
                    n.recipientAddress(), n.subject(), n.body(), n.status().name(),
                    n.priority().name(), n.createdAt(), n.sentAt(), n.failedReason()
            );
        }
    }
}
