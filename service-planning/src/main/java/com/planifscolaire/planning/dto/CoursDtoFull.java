package com.planifscolaire.planning.dto;

public class CoursDtoFull extends CoursDto {
    private ClasseDto classe;
    private EnseignantDto enseignant;

    public ClasseDto getClasse() {
        return classe;
    }

    public void setClasse(ClasseDto classe) {
        this.classe = classe;
    }

    public EnseignantDto getEnseignant() {
        return enseignant;
    }

    public void setEnseignant(EnseignantDto enseignant) {
        this.enseignant = enseignant;
    }
}
