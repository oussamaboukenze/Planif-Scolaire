package com.planifscolaire.planning.service;

import com.planifscolaire.planning.client.ReferentielClient;
import com.planifscolaire.planning.dto.CreateCoursDto;
import com.planifscolaire.planning.dto.CoursDtoFull;
import com.planifscolaire.planning.dto.EnseignantDto;
import com.planifscolaire.planning.entite.Cours;
import com.planifscolaire.planning.entite.ProcessedEvent;
import com.planifscolaire.planning.enums.StatutCours;
import com.planifscolaire.planning.event.AbsenceEvent;
import com.planifscolaire.planning.event.CourseEvent;
import com.planifscolaire.planning.event.EventOutbox;
import com.planifscolaire.planning.event.Events;
import com.planifscolaire.planning.exception.ConflitMetierException;
import com.planifscolaire.planning.exception.RessourceIntrouvableException;
import com.planifscolaire.planning.mapper.CoursMapper;
import com.planifscolaire.planning.mapper.CoursFullMapper;
import com.planifscolaire.planning.repository.CoursRepository;
import com.planifscolaire.planning.repository.OperationLocks;
import com.planifscolaire.planning.repository.ProcessedEvents;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CoursService {
    private final CoursRepository repository;
    private final ReferentielClient referentiel;
    private final OperationLocks locks;
    private final EventOutbox outbox;
    private final ProcessedEvents processed;
    private final CoursMapper mapper;
    private final CoursFullMapper fullMapper;

    public CoursService(
            CoursRepository repository,
            ReferentielClient referentiel,
            OperationLocks locks,
            EventOutbox outbox,
            ProcessedEvents processed,
            CoursMapper mapper,
            CoursFullMapper fullMapper) {
        this.repository = repository;
        this.referentiel = referentiel;
        this.locks = locks;
        this.outbox = outbox;
        this.processed = processed;
        this.mapper = mapper;
        this.fullMapper = fullMapper;
    }

    public List<Cours> getAll(Long classeId) {
        return classeId == null
                ? repository.findAll()
                : repository.findByClasseIdOrderByDateHeureAsc(classeId);
    }

    public Cours get(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Cours introuvable"));
    }

    @Transactional(readOnly = true)
    public CoursDtoFull getDetail(Long id) {
        Cours cours = get(id);
        CoursDtoFull detail = fullMapper.toDto(cours);
        detail.setClasse(referentiel.classe(cours.getClasseId()));
        detail.setEnseignant(referentiel.enseignant(cours.getEnseignantId()));
        return detail;
    }

    @Transactional
    public Cours create(CreateCoursDto request, String idempotencyKey) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("Clé d'idempotence invalide");
        }

        if (repository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new IllegalArgumentException("Opération déjà traitée");
        }
        referentiel.classe(request.getClasseId());
        EnseignantDto enseignant = referentiel.enseignant(request.getEnseignantId());
        if (!enseignant.getMatiere().equalsIgnoreCase(request.getMatiere().strip())) {
            throw new ConflitMetierException(
                    "La matière ne correspond pas à celle de l'enseignant");
        }
        locks.acquire();
        if (!repository
                .conflicts(
                        request.getDateHeure(),
                        request.getEnseignantId(),
                        request.getClasseId(),
                        request.getSalle().strip())
                .isEmpty()) {
            throw new ConflitMetierException(
                    "Enseignant, classe ou salle déjà occupé sur ce créneau");
        }
        Cours cours = mapper.toEntity(request);
        cours.setIdempotencyKey(idempotencyKey);
        cours.setMatiere(request.getMatiere().strip());
        cours.setSalle(request.getSalle().strip());
        repository.save(cours);
        outbox.add(
                Events.SCHEDULED,
                cours.getId(),
                new CourseEvent(
                        UUID.randomUUID(),
                        cours.getId(),
                        cours.getClasseId(),
                        cours.getDateHeure()));
        return cours;
    }

    @Transactional
    public Cours transition(Long id, StatutCours statut) {
        locks.acquire();
        Cours cours = get(id);
        if (cours.getStatut() == statut) {
            return cours;
        }
        if (cours.getStatut() != StatutCours.PLANIFIE || statut == StatutCours.PLANIFIE) {
            throw new ConflitMetierException(
                    "Transition interdite : " + cours.getStatut() + " → " + statut);
        }
        cours.setStatut(statut);
        if (statut == StatutCours.ANNULE) {
            outbox.add(
                    Events.CANCELLED,
                    cours.getId(),
                    new CourseEvent(
                            UUID.randomUUID(),
                            cours.getId(),
                            cours.getClasseId(),
                            cours.getDateHeure()));
        }
        return cours;
    }

    @Transactional
    public void onAbsence(AbsenceEvent event) {
        locks.acquire();
        if (processed.existsById(event.getEventId())) {
            return;
        }
        Cours cours = get(event.getCoursId());
        cours.setNbAbsencesSignalees(cours.getNbAbsencesSignalees() + 1);
        processed.save(new ProcessedEvent(event.getEventId()));
    }
}
