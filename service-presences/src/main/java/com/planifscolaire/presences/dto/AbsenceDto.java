package com.planifscolaire.presences.dto;

import jakarta.validation.constraints.*;

import java.time.*;

public class AbsenceDto {
    private Long id;

    private Long appelId;

    private String etudiantNom;

    private String motif;

    private boolean justifiee;

    public AbsenceDto() {}

    public AbsenceDto(Long id, Long appelId, String etudiantNom, String motif, boolean justifiee) {
        this.id = id;
        this.appelId = appelId;
        this.etudiantNom = etudiantNom;
        this.motif = motif;
        this.justifiee = justifiee;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppelId() {
        return appelId;
    }

    public void setAppelId(Long appelId) {
        this.appelId = appelId;
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
