package com.planifscolaire.planning.dto;

import java.util.ArrayList;
import java.util.List;

public class CoursDtoFull extends CoursDto {
    private ClasseDto classe;
    private EnseignantDto enseignant;
    private List<AbsenceDto> absences = new ArrayList<>();

    public List<AbsenceDto> getAbsences() {
        return absences;
    }

    public void setAbsences(List<AbsenceDto> absences) {
        this.absences = absences;
    }

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
