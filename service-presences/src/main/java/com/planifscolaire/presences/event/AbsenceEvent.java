package com.planifscolaire.presences.event;

import java.util.UUID;

public class AbsenceEvent {
    private UUID eventId;

    private Long coursId;

    private Long absenceId;

    public AbsenceEvent() {}

    public AbsenceEvent(UUID eventId, Long coursId, Long absenceId) {
        this.eventId = eventId;
        this.coursId = coursId;
        this.absenceId = absenceId;
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

    public Long getAbsenceId() {
        return absenceId;
    }

    public void setAbsenceId(Long absenceId) {
        this.absenceId = absenceId;
    }
}
