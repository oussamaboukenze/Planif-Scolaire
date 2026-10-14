package com.planifscolaire.presences.repository;

import com.planifscolaire.presences.entite.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEvents extends JpaRepository<ProcessedEvent, UUID> {}
