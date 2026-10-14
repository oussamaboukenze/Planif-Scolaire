package com.planifscolaire.presences.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class CourseEvent {
    private UUID eventId;

    private Long coursId;

    private Long classeId;

    private LocalDateTime dateHeure;

    public CourseEvent() {}

    public CourseEvent(UUID eventId, Long coursId, Long classeId, LocalDateTime dateHeure) {
        this.eventId = eventId;
        this.coursId = coursId;
        this.classeId = classeId;
        this.dateHeure = dateHeure;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getCoursId() {
        return coursId;
    }

    public void setCoursId(Long coursId) {
        this.coursId = coursId;
    }

    public Long getClasseId() {
        return classeId;
    }

    public void setClasseId(Long classeId) {
        this.classeId = classeId;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }
}
