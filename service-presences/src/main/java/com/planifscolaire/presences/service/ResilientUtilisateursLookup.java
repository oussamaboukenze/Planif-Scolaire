package com.planifscolaire.presences.service;

import feign.FeignException;

import com.planifscolaire.presences.client.UtilisateursClient;
import com.planifscolaire.presences.dto.UserDto;
import com.planifscolaire.presences.exception.RemoteServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class ResilientUtilisateursLookup {
    private final UtilisateursClient client;

    public ResilientUtilisateursLookup(UtilisateursClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "utilisateursEtudiant", fallbackMethod = "userFallback")
    @Retry(name = "utilisateursEtudiant")
    public UserDto user(Long id) {
        return client.user(id);
    }

    private UserDto userFallback(Long id, Throwable cause) {
        if (cause instanceof FeignException remote && remote.status() >= 400 && remote.status() < 500) {
            throw remote;
        }
        throw new RemoteServiceUnavailableException("indisponible");
    }
}
