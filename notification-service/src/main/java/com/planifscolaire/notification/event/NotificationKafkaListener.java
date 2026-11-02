package com.planifscolaire.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planifscolaire.notification.service.NotificationService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificationKafkaListener {
    private final ObjectMapper objectMapper;
    private final NotificationService service;

    public NotificationKafkaListener(ObjectMapper objectMapper, NotificationService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @KafkaListener(topics = "course-scheduled")
    public void courseScheduled(String message) throws IOException {
        service.sendCourseNotification(read(message, CourseEvent.class), "COURSE_SCHEDULED");
    }

    @KafkaListener(topics = "course-cancelled")
    public void courseCancelled(String message) throws IOException {
        service.sendCourseNotification(read(message, CourseEvent.class), "COURSE_CANCELLED");
    }

    @KafkaListener(topics = "absence-recorded")
    public void absenceRecorded(String message) throws IOException {
        service.sendAbsenceNotification(read(message, AbsenceEvent.class));
    }

    private <T> T read(String message, Class<T> eventType) throws IOException {
        return objectMapper.readValue(message, eventType);
    }
}