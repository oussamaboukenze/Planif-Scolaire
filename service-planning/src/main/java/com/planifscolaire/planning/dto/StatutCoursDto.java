package com.planifscolaire.planning.dto;

import com.planifscolaire.planning.enums.StatutCours;

import jakarta.validation.constraints.NotNull;

public class StatutCoursDto {
    @NotNull private StatutCours statut;

    public StatutCoursDto() {}

    public StatutCoursDto(StatutCours statut) {
        this.statut = statut;
    }

    public StatutCours getStatut() {
        return statut;
    }

    public void setStatut(StatutCours statut) {
        this.statut = statut;
    }
}
