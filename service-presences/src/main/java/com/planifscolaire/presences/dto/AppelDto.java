package com.planifscolaire.presences.dto;

import com.planifscolaire.presences.enums.StatutAppel;

import jakarta.validation.constraints.*;

import java.time.*;

public class AppelDto {
    private Long id;

    private Long coursId;

    private Long classeId;

    private LocalDate date;

    private StatutAppel statut;

    private int nbAbsences;

    public AppelDto() {}

    public AppelDto(
            Long id,
            Long coursId,
            Long classeId,
            LocalDate date,
            StatutAppel statut,
            int nbAbsences) {
        this.id = id;
        this.coursId = coursId;
        this.classeId = classeId;
        this.date = date;
        this.statut = statut;
        this.nbAbsences = nbAbsences;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public StatutAppel getStatut() {
        return statut;
    }

    public void setStatut(StatutAppel statut) {
        this.statut = statut;
    }

    public int getNbAbsences() {
        return nbAbsences;
    }

    public void setNbAbsences(int nbAbsences) {
        this.nbAbsences = nbAbsences;
    }
}
