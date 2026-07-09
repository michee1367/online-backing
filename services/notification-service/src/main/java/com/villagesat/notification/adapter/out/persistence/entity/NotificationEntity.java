package com.villagesat.notification.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;
import com.villagesat.notification.domain.model.Notification;
import java.time.Instant;

@Entity
@Table(name = "notifications", schema = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Notification.Channel channel;

    @Column(name = "template_code", length = 50)
    private String templateCode;

    @Column(name = "recipient_address", nullable = false, length = 255)
    private String recipientAddress;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Notification.Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Notification.Priority priority;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "failed_reason", length = 500)
    private String failedReason;

    @Version // Mappe la colonne 'version BIGINT' pour l'optimistic locking
    @Column(nullable = false)
    private Long version;

    // --- ENUMS INTERNES alignés sur ton NotificationMapper et ta table ---

    public enum ChannelEntity {
        SMS, EMAIL, PUSH, WHATSAPP
    }

    public enum StatusEntity {
        PENDING, SENT, FAILED
    }

    public enum PriorityEntity {
        LOW, NORMAL, HIGH, CRITICAL // 'NORMAL' correspond au DEFAULT de ton SQL
    }

    // --- PrePersist pour respecter les valeurs par défaut de ton SQL au cas où Java envoie null ---
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = Notification.Status.PENDING;
        }
        if (this.priority == null) {
            this.priority = Notification.Priority.NORMAL;
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.metadata == null || this.metadata.isBlank()) {
            this.metadata = "{}";
        }
    }

    //region Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Notification.Channel getChannel() { return channel; }
    public void setChannel(Notification.Channel channel) { this.channel = channel; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Notification.Status getStatus() { return status; }
    public void setStatus(Notification.Status status) { this.status = status; }

    public Notification.Priority getPriority() { return priority; }
    public void setPriority(Notification.Priority priority) { this.priority = priority; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    //endregion
}