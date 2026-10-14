package com.planifscolaire.presences.entite;

import jakarta.persistence.*;

@Entity
public class OperationLock {
    @Id private Long id;

    public OperationLock() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
