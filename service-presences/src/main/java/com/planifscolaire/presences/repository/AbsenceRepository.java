package com.planifscolaire.presences.repository;

import com.planifscolaire.presences.entite.Absence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByAppelId(Long appelId);

    boolean existsByAppelIdAndEtudiantId(Long appelId, Long etudiantId);

    boolean existsByAppelIdAndEtudiantNomIgnoreCase(Long appelId, String etudiantNom);
}
