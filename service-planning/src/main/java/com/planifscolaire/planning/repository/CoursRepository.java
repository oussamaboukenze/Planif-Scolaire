package com.planifscolaire.planning.repository;

import com.planifscolaire.planning.entite.Cours;

import org.springframework.data.jpa.repository.*;

import java.time.LocalDateTime;
import java.util.List;

public interface CoursRepository extends JpaRepository<Cours, Long> {
    @Query(
            "select c from Cours c where c.dateHeure = :date and c.statut <>"
                + " com.planifscolaire.planning.enums.StatutCours.ANNULE and (c.enseignantId ="
                + " :enseignant or c.classeId = :classe or lower(c.salle) = lower(:salle))")
    List<Cours> conflicts(LocalDateTime date, Long enseignant, Long classe, String salle);

    List<Cours> findByClasseIdOrderByDateHeureAsc(Long classeId);
}
