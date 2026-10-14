package com.planifscolaire.planning.repository;

import com.planifscolaire.planning.entite.OperationLock;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;

public interface OperationLocks extends JpaRepository<OperationLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from OperationLock l where l.id = 1")
    OperationLock acquire();
}
