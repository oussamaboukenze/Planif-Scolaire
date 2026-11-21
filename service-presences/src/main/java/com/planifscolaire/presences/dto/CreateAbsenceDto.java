package com.planifscolaire.presences.dto;

import jakarta.validation.constraints.*;

public class CreateAbsenceDto {
    @Positive private Long etudiantId;

    @Size(max = 255)
    private String etudiantNom;

    @NotNull
    @Size(max = 255)
    private String motif;

    private boolean justifiee;

    public CreateAbsenceDto() {}

    /** Il faut identifier l'étudiant : par son id (vérifié dans service-utilisateurs) ou par son nom. */
    @AssertTrue(message = "etudiantId ou etudiantNom est obligatoire")
    public boolean isEtudiantNomOuIdRenseigne() {
        return etudiantId != null || (etudiantNom != null && !etudiantNom.isBlank());
    }

    public CreateAbsenceDto(Long etudiantId, String motif, boolean justifiee) {
        this.etudiantId = etudiantId;
        this.motif = motif;
        this.justifiee = justifiee;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(Long etudiantId) {
        this.etudiantId = etudiantId;
    }

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
