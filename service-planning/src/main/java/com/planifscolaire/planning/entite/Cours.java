package com.planifscolaire.planning.entite;

import com.planifscolaire.planning.enums.StatutCours;

import jakarta.persistence.*;

import java.time.*;

@Entity
public class Cours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long classeId;

    @Column(nullable = false)
    private Long enseignantId;

    @Column(nullable = false)
    private String matiere;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCours statut = StatutCours.PLANIFIE;

    @Column(nullable = false)
    private int nbAbsencesSignalees;

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Cours() {}

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
