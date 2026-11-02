package com.planifscolaire.notification.dto;

import com.planifscolaire.notification.domain.NotificationDelivery;
import com.planifscolaire.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID eventId,
        Long coursId,
        String eventType,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt,
        String lastError) {
    public static NotificationResponse from(NotificationDelivery delivery) {
        return new NotificationResponse(
                delivery.getEventId(),
                delivery.getCoursId(),
                delivery.getEventType(),
                delivery.getStatus(),
                delivery.getCreatedAt(),
                delivery.getSentAt(),
                delivery.getLastError());
    }
}