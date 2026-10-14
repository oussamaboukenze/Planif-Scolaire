package com.planifscolaire.planning;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.planifscolaire.planning.client.ReferentielClient;
import com.planifscolaire.planning.dto.ClasseDto;
import com.planifscolaire.planning.dto.CreateCoursDto;
import com.planifscolaire.planning.dto.EnseignantDto;
import com.planifscolaire.planning.enums.StatutCours;
import com.planifscolaire.planning.event.AbsenceEvent;
import com.planifscolaire.planning.event.Events;
import com.planifscolaire.planning.exception.ApiException;
import com.planifscolaire.planning.repository.CoursRepository;
import com.planifscolaire.planning.repository.OutboxRepository;
import com.planifscolaire.planning.repository.ProcessedEvents;
import com.planifscolaire.planning.service.CoursService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

@SpringBootTest
class CoursServiceTest {
    @Autowired CoursService service;
    @Autowired CoursRepository cours;
    @Autowired OutboxRepository outbox;
    @Autowired ProcessedEvents processed;
    @MockitoBean ReferentielClient referentiel;
    final LocalDateTime date = LocalDateTime.of(2026, 10, 14, 9, 0);

    @BeforeEach
    void setup() {
        outbox.deleteAll();
        processed.deleteAll();
        cours.deleteAll();
        when(referentiel.classe(anyLong())).thenReturn(new ClasseDto(1L, "6A", "6e", 30));
        when(referentiel.enseignant(anyLong()))
                .thenReturn(new EnseignantDto(1L, "Ali", "Maths", 20));
    }

    CreateCoursDto request(long classe, long enseignant, String salle) {
        return new CreateCoursDto(classe, enseignant, "Maths", date, salle);
    }

    @Test
    void creationPersistsCourseAndEventTogether() {
        var c = service.create(request(1, 1, "A1"));
        assertThat(c.getStatut()).isEqualTo(StatutCours.PLANIFIE);
        assertThat(c.getNbAbsencesSignalees()).isZero();
        assertThat(outbox.findAll())
                .singleElement()
                .satisfies(
                        e -> {
                            assertThat(e.getTopic()).isEqualTo(Events.SCHEDULED);
                            assertThat(e.getPayload()).contains("coursId", "classeId", "dateHeure");
                        });
        verify(referentiel).classe(1L);
        verify(referentiel).enseignant(1L);
    }

    @Test
    void rejectsTeacherClassAndRoomConflicts() {
        service.create(request(1, 1, "A1"));
        assertThatThrownBy(() -> service.create(request(2, 1, "A2")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create(request(1, 2, "A2")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create(request(2, 2, " a1 ")))
                .isInstanceOf(ApiException.class);
        assertThat(cours.count()).isEqualTo(1);
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    void cancellationReleasesSlotAndIsIdempotent() {
        var c = service.create(request(1, 1, "A1"));
        service.transition(c.getId(), StatutCours.ANNULE);
        service.transition(c.getId(), StatutCours.ANNULE);
        assertThat(outbox.findAll().stream().filter(e -> e.getTopic().equals(Events.CANCELLED)))
                .hasSize(1);
        assertThat(service.create(request(1, 1, "A1")).getStatut()).isEqualTo(StatutCours.PLANIFIE);
        assertThatThrownBy(() -> service.transition(c.getId(), StatutCours.REALISE))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void realisedIsTerminal() {
        var c = service.create(request(1, 1, "A1"));
        service.transition(c.getId(), StatutCours.REALISE);
        assertThatThrownBy(() -> service.transition(c.getId(), StatutCours.ANNULE))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.transition(c.getId(), StatutCours.PLANIFIE))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void duplicateEventDoesNotDoubleCount() {
        var c = service.create(request(1, 1, "A1"));
        service.transition(c.getId(), StatutCours.REALISE);
        var event = new AbsenceEvent(UUID.randomUUID(), c.getId(), 1L);
        service.onAbsence(event);
        service.onAbsence(event);
        service.onAbsence(new AbsenceEvent(UUID.randomUUID(), c.getId(), 2L));
        assertThat(service.get(c.getId()).getNbAbsencesSignalees()).isEqualTo(2);
    }

    @Test
    void remoteFailureDoesNotCreateCourseOrEvent() {
        when(referentiel.classe(1L)).thenThrow(new RuntimeException("indisponible"));
        assertThatThrownBy(() -> service.create(request(1, 1, "A1"))).hasMessage("indisponible");
        assertThat(cours.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    @Test
    void subjectMustMatchTeacher() {
        assertThatThrownBy(() -> service.create(new CreateCoursDto(1L, 1L, "Histoire", date, "A1")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void concurrentCreationsCannotReserveSameSlot() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> create =
                () -> {
                    start.await();
                    try {
                        service.create(request(1, 1, "A1"));
                        return true;
                    } catch (ApiException e) {
                        return false;
                    }
                };
        try {
            var a = pool.submit(create);
            var b = pool.submit(create);
            start.countDown();
            assertThat(java.util.List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(cours.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
