package com.planifscolaire.notification.repository;

import com.planifscolaire.notification.domain.NotificationDelivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationDelivery, UUID> {
    List<NotificationDelivery> findByCoursIdOrderByCreatedAtDesc(Long coursId);
}