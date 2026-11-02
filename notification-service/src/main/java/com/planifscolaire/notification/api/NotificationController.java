package com.planifscolaire.notification.api;

import com.planifscolaire.notification.dto.NotificationResponse;
import com.planifscolaire.notification.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/cours/{coursId}")
    public List<NotificationResponse> byCourse(@PathVariable Long coursId) {
        return service.findByCours(coursId);
    }

    @GetMapping("/{eventId}")
    public NotificationResponse byEvent(@PathVariable UUID eventId) {
        try {
            return service.findByEventId(eventId);
        } catch (com.planifscolaire.notification.service.NotificationNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }
}