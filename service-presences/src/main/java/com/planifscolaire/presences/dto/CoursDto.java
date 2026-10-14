package com.planifscolaire.presences.dto;

import java.time.LocalDateTime;

public class CoursDto {
    private Long id;

    private Long classeId;

    private LocalDateTime dateHeure;

    private String statut;

    public CoursDto() {}

    public CoursDto(Long id, Long classeId, LocalDateTime dateHeure, String statut) {
        this.id = id;
        this.classeId = classeId;
        this.dateHeure = dateHeure;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
