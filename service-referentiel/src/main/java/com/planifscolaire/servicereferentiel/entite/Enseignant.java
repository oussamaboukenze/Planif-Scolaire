package com.planifscolaire.servicereferentiel.entite;

import jakarta.persistence.*;

import java.time.*;

@Entity
public class Enseignant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String matiere;

    @Column(nullable = false)
    private int chargeMaxHeures;

    public Enseignant() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getMatiere() {
        return matiere;
    }

    public void setMatiere(String matiere) {
        this.matiere = matiere;
    }

    public int getChargeMaxHeures() {
        return chargeMaxHeures;
    }

    public void setChargeMaxHeures(int chargeMaxHeures) {
        this.chargeMaxHeures = chargeMaxHeures;
    }
}
