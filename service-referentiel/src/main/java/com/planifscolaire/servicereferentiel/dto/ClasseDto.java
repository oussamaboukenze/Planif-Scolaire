package com.planifscolaire.servicereferentiel.dto;

import jakarta.validation.constraints.*;

import java.time.*;

public class ClasseDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String nom;

    @NotBlank
    @Size(max = 255)
    private String niveau;

    @Positive private int effectifMax;

    public ClasseDto() {}

    public ClasseDto(Long id, String nom, String niveau, int effectifMax) {
        this.id = id;
        this.nom = nom;
        this.niveau = niveau;
        this.effectifMax = effectifMax;
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

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public int getEffectifMax() {
        return effectifMax;
    }

    public void setEffectifMax(int effectifMax) {
        this.effectifMax = effectifMax;
    }
}
