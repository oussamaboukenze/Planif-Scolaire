package com.planifscolaire.planning.repository;

import com.planifscolaire.planning.entite.OutboxEvent;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEvent> findTop100ByOrderByIdAsc();
}
