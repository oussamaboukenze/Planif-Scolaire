package com.planifscolaire.notification.service;

import com.planifscolaire.notification.domain.NotificationDelivery;
import com.planifscolaire.notification.domain.NotificationStatus;
import com.planifscolaire.notification.dto.NotificationResponse;
import com.planifscolaire.notification.event.AbsenceEvent;
import com.planifscolaire.notification.event.CourseEvent;
import com.planifscolaire.notification.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final JavaMailSender mailSender;
    private final String recipient;

    public NotificationService(
            NotificationRepository repository,
            JavaMailSender mailSender,
            @Value("${app.notification.recipient}") String recipient) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.recipient = recipient;
    }

    public NotificationResponse sendCourseNotification(CourseEvent event, String eventType) {
        String subject = "Planif-Scolaire: " + eventType;
        String date = event.dateHeure().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String body = "Cours #" + event.coursId() + " pour la classe #" + event.classeId()
                + " le " + date + ".";
        // V5 : on notifie l'enseignant ; sinon l'adresse par défaut de la configuration
        String to = event.enseignantEmail() == null || event.enseignantEmail().isBlank()
                ? recipient
                : event.enseignantEmail();
        return deliver(event.eventId(), event.coursId(), eventType, subject, body, to);
    }

    public NotificationResponse sendAbsenceNotification(AbsenceEvent event) {
        String eventType = "ABSENCE_RECORDED";
        String subject = "Planif-Scolaire: absence enregistree";
        String body = "Une absence (#" + event.absenceId() + ") a ete enregistree pour le cours #"
                + event.coursId() + ".";
        return deliver(event.eventId(), event.coursId(), eventType, subject, body, recipient);
    }

    public List<NotificationResponse> findByCours(Long coursId) {
        return repository.findByCoursIdOrderByCreatedAtDesc(coursId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public NotificationResponse findByEventId(UUID eventId) {
        return repository.findById(eventId)
                .map(NotificationResponse::from)
                .orElseThrow(() -> new NotificationNotFoundException(eventId));
    }

    private NotificationResponse deliver(
            UUID eventId, Long coursId, String eventType, String subject, String body, String to) {
        NotificationDelivery delivery = repository.findById(eventId).orElseGet(
                () -> new NotificationDelivery(eventId, coursId, eventType, to, subject));
        if (delivery.getStatus() == NotificationStatus.SENT) {
            return NotificationResponse.from(delivery);
        }

        delivery.markPending();
        repository.saveAndFlush(delivery);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            delivery.markSent();
            return NotificationResponse.from(repository.saveAndFlush(delivery));
        } catch (MailException exception) {
            delivery.markFailed(exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            repository.saveAndFlush(delivery);
            throw exception;
        }
    }
}