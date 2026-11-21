package com.planifscolaire.notification.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseEvent(
        UUID eventId, Long coursId, Long classeId, LocalDateTime dateHeure, String enseignantEmail) {

    public CourseEvent(UUID eventId, Long coursId, Long classeId, LocalDateTime dateHeure) {
        this(eventId, coursId, classeId, dateHeure, null);
    }
}
