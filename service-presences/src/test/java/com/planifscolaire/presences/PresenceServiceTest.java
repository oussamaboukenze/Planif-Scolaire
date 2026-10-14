package com.planifscolaire.presences;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.planifscolaire.presences.batch.MonthlyExport;
import com.planifscolaire.presences.client.PlanningClient;
import com.planifscolaire.presences.dto.CoursDto;
import com.planifscolaire.presences.dto.CreateAbsenceDto;
import com.planifscolaire.presences.enums.StatutAppel;
import com.planifscolaire.presences.event.CourseEvent;
import com.planifscolaire.presences.event.Events;
import com.planifscolaire.presences.exception.ApiException;
import com.planifscolaire.presences.repository.AbsenceRepository;
import com.planifscolaire.presences.repository.AppelRepository;
import com.planifscolaire.presences.repository.OutboxRepository;
import com.planifscolaire.presences.repository.ProcessedEvents;
import com.planifscolaire.presences.service.PresenceService;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.*;
import java.time.*;
import java.util.UUID;

@SpringBootTest
class PresenceServiceTest {
    @Autowired PresenceService service;
    @Autowired AppelRepository appels;
    @Autowired AbsenceRepository absences;
    @Autowired OutboxRepository outbox;
    @Autowired ProcessedEvents processed;
    @MockitoBean PlanningClient planning;
    final LocalDateTime date = LocalDateTime.of(2026, 10, 14, 9, 0);

    CourseEvent event() {
        return new CourseEvent(UUID.randomUUID(), 1L, 2L, date);
    }

    @BeforeEach
    void setup() {
        absences.deleteAll();
        appels.deleteAll();
        outbox.deleteAll();
        processed.deleteAll();
        when(planning.cours(1L)).thenReturn(new CoursDto(1L, 2L, date, "REALISE"));
    }

    Long createAppel() {
        service.onCourse(event(), false);
        return appels.findByCoursId(1L).orElseThrow().getId();
    }

    @Test
    void duplicateSchedulingCreatesOnlyOneSheet() {
        var event = event();
        service.onCourse(event, false);
        service.onCourse(event, false);
        service.onCourse(event(), false);
        assertThat(appels.count()).isEqualTo(1);
        assertThat(appels.findAll().get(0).getDate()).isEqualTo(date.toLocalDate());
    }

    @Test
    void cancellationBeforeSchedulingNeverReopensSheet() {
        service.onCourse(event(), true);
        service.onCourse(event(), false);
        assertThat(appels.findAll())
                .singleElement()
                .satisfies(a -> assertThat(a.getStatut()).isEqualTo(StatutAppel.CLOTURE));
    }

    @Test
    void cancellationClosesExistingSheet() {
        Long id = createAppel();
        service.onCourse(event(), true);
        assertThat(service.get(id).getStatut()).isEqualTo(StatutAppel.CLOTURE);
        assertThatThrownBy(() -> service.record(id, new CreateAbsenceDto("Sara", "", false)))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(planning);
    }

    @Test
    void recordingUpdatesSheetAndOutboxAtomically() {
        Long id = createAppel();
        var a = service.record(id, new CreateAbsenceDto(" Sara  Ali ", "Maladie", true));
        assertThat(a.getEtudiantNom()).isEqualTo("Sara Ali");
        assertThat(service.get(id).getNbAbsences()).isEqualTo(1);
        assertThat(outbox.findAll())
                .singleElement()
                .satisfies(e -> assertThat(e.getTopic()).isEqualTo(Events.ABSENCE));
    }

    @Test
    void plannedCourseCannotReceiveAbsences() {
        Long id = createAppel();
        when(planning.cours(1L)).thenReturn(new CoursDto(1L, 2L, date, "PLANIFIE"));
        assertThatThrownBy(() -> service.record(id, new CreateAbsenceDto("Sara", "", false)))
                .isInstanceOf(ApiException.class);
        assertThat(absences.count()).isZero();
        assertThat(outbox.count()).isZero();
        assertThat(service.get(id).getNbAbsences()).isZero();
    }

    @Test
    void duplicateStudentIsRejectedWithoutIncrement() {
        Long id = createAppel();
        service.record(id, new CreateAbsenceDto("Sara Ali", "", false));
        assertThatThrownBy(() -> service.record(id, new CreateAbsenceDto(" sara  ali ", "", true)))
                .isInstanceOf(ApiException.class);
        assertThat(absences.count()).isEqualTo(1);
        assertThat(service.get(id).getNbAbsences()).isEqualTo(1);
    }

    @Test
    void unavailablePlanningDoesNotRecordAbsence() {
        Long id = createAppel();
        when(planning.cours(1L)).thenThrow(new RuntimeException("indisponible"));
        assertThatThrownBy(() -> service.record(id, new CreateAbsenceDto("Sara", "", false)))
                .hasMessage("indisponible");
        assertThat(absences.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    @Test
    void monthlyCsvGroupsByClassAndExcludesOtherMonths(@TempDir Path directory) throws Exception {
        Long id = createAppel();
        service.record(id, new CreateAbsenceDto("Sara", "", false));
        service.onCourse(new CourseEvent(UUID.randomUUID(), 2L, 2L, date.plusMonths(1)), false);
        Path file = new MonthlyExport(appels, directory.toString()).export(YearMonth.of(2026, 10));
        assertThat(Files.readString(file)).isEqualTo("mois,classeId,nbAbsences\n2026-10,2,1\n");
    }
}
