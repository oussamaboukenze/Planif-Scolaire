package com.planifscolaire.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {
    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Long coursId;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @Column(length = 1000)
    private String lastError;

    protected NotificationDelivery() {}

    public NotificationDelivery(
            UUID eventId, Long coursId, String eventType, String recipient, String subject) {
        this.eventId = eventId;
        this.coursId = coursId;
        this.eventType = eventType;
        this.recipient = recipient;
        this.subject = subject;
        this.status = NotificationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getCoursId() {
        return coursId;
    }

    public String getEventType() {
        return eventType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markPending() {
        status = NotificationStatus.PENDING;
        lastError = null;
    }

    public void markSent() {
        status = NotificationStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        status = NotificationStatus.FAILED;
        lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}