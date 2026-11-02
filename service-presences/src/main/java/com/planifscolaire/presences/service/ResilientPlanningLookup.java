package com.planifscolaire.presences.service;

import feign.FeignException;

import com.planifscolaire.presences.client.PlanningClient;
import com.planifscolaire.presences.dto.CoursDto;
import com.planifscolaire.presences.exception.RemoteServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class ResilientPlanningLookup {
    private final PlanningClient client;

    public ResilientPlanningLookup(PlanningClient client) {
        this.client = client;
    }

    @CircuitBreaker(name = "planningCours", fallbackMethod = "coursFallback")
    @Retry(name = "planningCours")
    public CoursDto cours(Long id) {
        return client.cours(id);
    }

    private CoursDto coursFallback(Long id, Throwable cause) {
        if (cause instanceof FeignException remote && remote.status() >= 400 && remote.status() < 500) {
            throw remote;
        }
        throw new RemoteServiceUnavailableException("indisponible");
    }
}