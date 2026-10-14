package com.planifscolaire.servicereferentiel.dto;

import jakarta.validation.constraints.*;

import java.time.*;

public class EnseignantDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String nom;

    @NotBlank
    @Size(max = 255)
    private String matiere;

    @Positive private int chargeMaxHeures;

    public EnseignantDto() {}

    public EnseignantDto(Long id, String nom, String matiere, int chargeMaxHeures) {
        this.id = id;
        this.nom = nom;
        this.matiere = matiere;
        this.chargeMaxHeures = chargeMaxHeures;
    }

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
