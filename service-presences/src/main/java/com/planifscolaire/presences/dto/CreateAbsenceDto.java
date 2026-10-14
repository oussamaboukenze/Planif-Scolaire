package com.planifscolaire.presences.dto;

import jakarta.validation.constraints.*;

public class CreateAbsenceDto {
    @NotBlank
    @Size(max = 255)
    private String etudiantNom;

    @NotNull
    @Size(max = 255)
    private String motif;

    private boolean justifiee;

    public CreateAbsenceDto() {}

    public CreateAbsenceDto(String etudiantNom, String motif, boolean justifiee) {
        this.etudiantNom = etudiantNom;
        this.motif = motif;
        this.justifiee = justifiee;
    }

    public String getEtudiantNom() {
        return etudiantNom;
    }

    public void setEtudiantNom(String etudiantNom) {
        this.etudiantNom = etudiantNom;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public boolean getJustifiee() {
        return justifiee;
    }

    public void setJustifiee(boolean justifiee) {
        this.justifiee = justifiee;
    }
}
