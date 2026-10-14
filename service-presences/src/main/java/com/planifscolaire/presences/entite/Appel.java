package com.planifscolaire.presences.entite;

import com.planifscolaire.presences.enums.StatutAppel;

import jakarta.persistence.*;

import java.time.*;

@Entity
public class Appel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long coursId;

    @Column(nullable = false)
    private Long classeId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAppel statut = StatutAppel.OUVERT;

    @Column(nullable = false)
    private int nbAbsences;

    public Appel() {}

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
