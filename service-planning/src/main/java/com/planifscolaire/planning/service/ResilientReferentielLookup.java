package com.planifscolaire.planning.service;

import feign.FeignException;

import com.planifscolaire.planning.client.ReferentielClient;
import com.planifscolaire.planning.dto.ClasseDto;
import com.planifscolaire.planning.dto.EnseignantDto;
import com.planifscolaire.planning.exception.RemoteServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class ResilientReferentielLookup {
    private final ReferentielClient client;

    public ResilientReferentielLookup(ReferentielClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "referentielClasse", fallbackMethod = "classeFallback")
    @Retry(name = "referentielClasse")
    public ClasseDto classe(Long id) {
        return client.classe(id);
    }

    @CircuitBreaker(name = "referentielEnseignant", fallbackMethod = "enseignantFallback")
    @Retry(name = "referentielEnseignant")
    public EnseignantDto enseignant(Long id) {
        return client.enseignant(id);
    }

    private ClasseDto classeFallback(Long id, Throwable cause) {
        throw fallbackException(cause);
    }

    private EnseignantDto enseignantFallback(Long id, Throwable cause) {
        throw fallbackException(cause);
    }

    private RuntimeException fallbackException(Throwable cause) {
        if (cause instanceof FeignException remote && remote.status() >= 400 && remote.status() < 500) {
            return remote;
        }
        return new RemoteServiceUnavailableException("indisponible");
    }
}