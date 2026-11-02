package com.planifscolaire.notification.service;

import com.planifscolaire.notification.domain.NotificationDelivery;
import com.planifscolaire.notification.domain.NotificationStatus;
import com.planifscolaire.notification.event.CourseEvent;
import com.planifscolaire.notification.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock JavaMailSender mailSender;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, mailSender, "demo@planifscolaire.local");
    }

    @Test
    void sendsMailAndPersistsSuccessfulDelivery() {
        UUID eventId = UUID.randomUUID();
        when(repository.findById(eventId)).thenReturn(Optional.empty());
        stubSave();

        var response = service.sendCourseNotification(
                new CourseEvent(eventId, 42L, 3L, LocalDateTime.of(2026, 11, 10, 9, 0)),
                "COURSE_SCHEDULED");

        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.sentAt()).isNotNull();
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("demo@planifscolaire.local");
        verify(repository, times(2)).saveAndFlush(any(NotificationDelivery.class));
    }

    @Test
    void doesNotSendAnEventThatWasAlreadyDelivered() {
        UUID eventId = UUID.randomUUID();
        NotificationDelivery delivery = new NotificationDelivery(
                eventId, 42L, "COURSE_SCHEDULED", "demo@planifscolaire.local", "scheduled");
        delivery.markSent();
        when(repository.findById(eventId)).thenReturn(Optional.of(delivery));

        var response = service.sendCourseNotification(
                new CourseEvent(eventId, 42L, 3L, LocalDateTime.of(2026, 11, 10, 9, 0)),
                "COURSE_SCHEDULED");

        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(repository, never()).saveAndFlush(any(NotificationDelivery.class));
    }

    @Test
    void recordsFailedDeliveryBeforePropagatingMailFailure() {
        UUID eventId = UUID.randomUUID();
        when(repository.findById(eventId)).thenReturn(Optional.empty());
        stubSave();
        doThrow(new MailSendException("Mailpit unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendCourseNotification(
                new CourseEvent(eventId, 42L, 3L, LocalDateTime.of(2026, 11, 10, 9, 0)),
                "COURSE_SCHEDULED"))
                .isInstanceOf(MailSendException.class);

        ArgumentCaptor<NotificationDelivery> delivery =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, times(2)).saveAndFlush(delivery.capture());
        assertThat(delivery.getAllValues().get(1).getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(delivery.getAllValues().get(1).getLastError()).contains("Mailpit unavailable");
    }

        private void stubSave() {
                when(repository.saveAndFlush(any(NotificationDelivery.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }
}