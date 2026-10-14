package com.planifscolaire.planning.repository;

import com.planifscolaire.planning.entite.ProcessedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEvents extends JpaRepository<ProcessedEvent, UUID> {}
