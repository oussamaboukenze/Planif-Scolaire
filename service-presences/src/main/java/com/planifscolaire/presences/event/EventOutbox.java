package com.planifscolaire.presences.event;

import com.planifscolaire.presences.entite.OutboxEvent;
import com.planifscolaire.presences.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class EventOutbox {
    private final OutboxRepository repository;
    private final EventCodec json;

    public EventOutbox(OutboxRepository repository, EventCodec json) {
        this.repository = repository;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void add(String topic, Long coursId, Object payload) {
        repository.save(new OutboxEvent(topic, coursId.toString(), json.write(payload)));
    }
}
