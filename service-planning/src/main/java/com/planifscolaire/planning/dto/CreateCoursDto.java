package com.planifscolaire.planning.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CreateCoursDto {
    @NotNull @Positive private Long classeId;

    @NotNull @Positive private Long enseignantId;

    @NotBlank
    @Size(max = 255)
    private String matiere;

    @NotNull private LocalDateTime dateHeure;

    @NotBlank
    @Size(max = 255)
    private String salle;

    public CreateCoursDto() {}

    public CreateCoursDto(
            Long classeId,
            Long enseignantId,
            String matiere,
            LocalDateTime dateHeure,
            String salle) {
        this.classeId = classeId;
        this.enseignantId = enseignantId;
        this.matiere = matiere;
        this.dateHeure = dateHeure;
        this.salle = salle;
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
}
