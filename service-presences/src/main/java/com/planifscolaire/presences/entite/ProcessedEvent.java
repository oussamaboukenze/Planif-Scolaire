package com.planifscolaire.presences.entite;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class ProcessedEvent {
    @Id private UUID id;

    public ProcessedEvent() {}

    public ProcessedEvent(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
