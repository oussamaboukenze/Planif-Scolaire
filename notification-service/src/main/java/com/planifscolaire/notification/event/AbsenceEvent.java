package com.planifscolaire.notification.event;

import java.util.UUID;

public record AbsenceEvent(UUID eventId, Long coursId, Long absenceId) {}