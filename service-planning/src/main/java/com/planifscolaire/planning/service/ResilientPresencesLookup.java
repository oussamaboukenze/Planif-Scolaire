package com.planifscolaire.planning.service;

import com.planifscolaire.planning.client.PresencesClient;
import com.planifscolaire.planning.dto.AbsenceDto;
import com.planifscolaire.planning.dto.AppelDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResilientPresencesLookup {
    private final PresencesClient client;

    public ResilientPresencesLookup(PresencesClient client) {
        this.client = client;
    }

    /** Absences d'un cours (vue enrichie). Dégradation : liste vide si presences est indisponible. */
    @CircuitBreaker(name = "presencesAbsences", fallbackMethod = "absencesFallback")
    public List<AbsenceDto> absencesDuCours(Long coursId) {
        AppelDto appel = client.appelDuCours(coursId);
        return client.absences(appel.getId());
    }

    private List<AbsenceDto> absencesFallback(Long coursId, Throwable cause) {
        return List.of();
    }
}
