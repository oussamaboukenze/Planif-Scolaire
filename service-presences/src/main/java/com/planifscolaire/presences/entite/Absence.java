package com.planifscolaire.presences.entite;

import jakarta.persistence.*;

import java.time.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"appelId", "etudiantNom"}))
public class Absence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long appelId;

    @Column(nullable = false)
    private String etudiantNom;

    @Column(nullable = false)
    private String motif;

    @Column(nullable = false)
    private boolean justifiee;

    public Absence() {}

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
