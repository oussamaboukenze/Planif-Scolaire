package com.planifscolaire.planning.dto;

import com.planifscolaire.planning.enums.StatutCours;

import jakarta.validation.constraints.*;

import java.time.*;

public class CoursDto {
    private Long id;

    private Long classeId;

    private Long enseignantId;

    private String matiere;

    private LocalDateTime dateHeure;

    private String salle;

    private StatutCours statut;

    private int nbAbsencesSignalees;

    public CoursDto() {}

    public CoursDto(
            Long id,
            Long classeId,
            Long enseignantId,
            String matiere,
            LocalDateTime dateHeure,
            String salle,
            StatutCours statut,
            int nbAbsencesSignalees) {
        this.id = id;
        this.classeId = classeId;
        this.enseignantId = enseignantId;
        this.matiere = matiere;
        this.dateHeure = dateHeure;
        this.salle = salle;
        this.statut = statut;
        this.nbAbsencesSignalees = nbAbsencesSignalees;
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

    public Long getEnseignantId() {
        return enseignantId;
    }

    public void setEnseignantId(Long enseignantId) {
        this.enseignantId = enseignantId;
    }

    public String getMatiere() {
        return matiere;
    }

    public void setMatiere(String matiere) {
        this.matiere = matiere;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String salle) {
        this.salle = salle;
    }

    public StatutCours getStatut() {
        return statut;
    }

    public void setStatut(StatutCours statut) {
        this.statut = statut;
    }

    public int getNbAbsencesSignalees() {
        return nbAbsencesSignalees;
    }

    public void setNbAbsencesSignalees(int nbAbsencesSignalees) {
        this.nbAbsencesSignalees = nbAbsencesSignalees;
    }
}
