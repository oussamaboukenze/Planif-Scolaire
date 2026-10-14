package com.planifscolaire.presences.repository;

import com.planifscolaire.presences.entite.Appel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.*;

public interface AppelRepository extends JpaRepository<Appel, Long> {
    Optional<Appel> findByCoursId(Long coursId);

    List<Appel> findByDateGreaterThanEqualAndDateLessThan(LocalDate start, LocalDate end);
}
