package com.planifscolaire.presences.service;

import com.planifscolaire.presences.client.PlanningClient;
import com.planifscolaire.presences.dto.CreateAbsenceDto;
import com.planifscolaire.presences.entite.Absence;
import com.planifscolaire.presences.entite.Appel;
import com.planifscolaire.presences.entite.ProcessedEvent;
import com.planifscolaire.presences.enums.StatutAppel;
import com.planifscolaire.presences.event.AbsenceEvent;
import com.planifscolaire.presences.event.CourseEvent;
import com.planifscolaire.presences.event.EventOutbox;
import com.planifscolaire.presences.event.Events;
import com.planifscolaire.presences.exception.ConflitMetierException;
import com.planifscolaire.presences.exception.RessourceIntrouvableException;
import com.planifscolaire.presences.repository.AbsenceRepository;
import com.planifscolaire.presences.repository.AppelRepository;
import com.planifscolaire.presences.repository.OperationLocks;
import com.planifscolaire.presences.repository.ProcessedEvents;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PresenceService {
    private final AppelRepository appels;
    private final AbsenceRepository absences;
    private final PlanningClient planning;
    private final OperationLocks locks;
    private final EventOutbox outbox;
    private final ProcessedEvents processed;

    public PresenceService(
            AppelRepository appels,
            AbsenceRepository absences,
            PlanningClient planning,
            OperationLocks locks,
            EventOutbox outbox,
            ProcessedEvents processed) {
        this.appels = appels;
        this.absences = absences;
        this.planning = planning;
        this.locks = locks;
        this.outbox = outbox;
        this.processed = processed;
    }

    public List<Appel> getAll() {
        return appels.findAll();
    }

    public Appel get(Long id) {
        return appels.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Appel introuvable"));
    }

    public Appel getByCoursId(Long coursId) {
        return appels.findByCoursId(coursId)
                .orElseThrow(
                        () ->
                                new RessourceIntrouvableException(
                                        "Appel pas encore disponible pour ce cours"));
    }

    public List<Absence> getAbsences(Long appelId) {
        get(appelId);
        return absences.findByAppelId(appelId);
    }

    @Transactional
    public void onCourse(CourseEvent event, boolean cancelled) {
        locks.acquire();
        if (processed.existsById(event.getEventId())) {
            return;
        }
        Appel appel =
                appels.findByCoursId(event.getCoursId())
                        .orElseGet(
                                () -> {
                                    Appel nouveau = new Appel();
                                    nouveau.setCoursId(event.getCoursId());
                                    nouveau.setClasseId(event.getClasseId());
                                    nouveau.setDate(event.getDateHeure().toLocalDate());
                                    return nouveau;
                                });
        if (cancelled) {
            appel.setStatut(StatutAppel.CLOTURE);
        }
        appels.save(appel);
        processed.save(new ProcessedEvent(event.getEventId()));
    }

    @Transactional
    public Absence record(Long appelId, CreateAbsenceDto request) {
        locks.acquire();
        Appel appel = get(appelId);
        if (appel.getStatut() == StatutAppel.CLOTURE) {
            throw new ConflitMetierException("La feuille d'appel est clôturée");
        }
        if (!"REALISE".equals(planning.cours(appel.getCoursId()).getStatut())) {
            throw new ConflitMetierException(
                    "Le cours doit être REALISE avant la saisie des absences");
        }
        String nom = request.getEtudiantNom().strip().replaceAll("\\s+", " ");
        if (absences.existsByAppelIdAndEtudiantNomIgnoreCase(appelId, nom)) {
            throw new ConflitMetierException(
                    "Une absence existe déjà pour cet étudiant sur cet appel");
        }
        Absence absence = new Absence();
        absence.setAppelId(appelId);
        absence.setEtudiantNom(nom);
        absence.setMotif(request.getMotif().strip());
        absence.setJustifiee(request.getJustifiee());
        absences.save(absence);
        appel.setNbAbsences(appel.getNbAbsences() + 1);
        outbox.add(
                Events.ABSENCE,
                appel.getCoursId(),
                new AbsenceEvent(UUID.randomUUID(), appel.getCoursId(), absence.getId()));
        return absence;
    }
}
