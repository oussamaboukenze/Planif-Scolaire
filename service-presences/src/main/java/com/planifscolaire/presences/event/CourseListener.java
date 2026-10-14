package com.planifscolaire.presences.event;

import com.planifscolaire.presences.service.PresenceService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CourseListener {
    private final PresenceService service;
    private final EventCodec json;

    public CourseListener(PresenceService service, EventCodec json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Events.SCHEDULED)
    public void scheduled(String message) {
        service.onCourse(json.read(message, CourseEvent.class), false);
    }

    @KafkaListener(topics = Events.CANCELLED)
    public void cancelled(String message) {
        service.onCourse(json.read(message, CourseEvent.class), true);
    }
}
