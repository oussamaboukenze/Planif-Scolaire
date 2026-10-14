package com.planifscolaire.planning.event;

import com.planifscolaire.planning.service.CoursService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AbsenceListener {
    private final CoursService service;
    private final EventCodec json;

    public AbsenceListener(CoursService service, EventCodec json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Events.ABSENCE)
    public void receive(String message) {
        service.onAbsence(json.read(message, AbsenceEvent.class));
    }
}
