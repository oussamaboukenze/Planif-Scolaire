package com.planifscolaire.notification.service;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID eventId) {
        super("Notification introuvable pour l'evenement " + eventId);
    }
}