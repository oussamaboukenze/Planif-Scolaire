package com.planifscolaire.servicereferentiel.service;

import com.planifscolaire.servicereferentiel.entite.Enseignant;
import com.planifscolaire.servicereferentiel.exception.RessourceIntrouvableException;
import com.planifscolaire.servicereferentiel.repository.EnseignantRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnseignantService {
    private final EnseignantRepository repository;

    public EnseignantService(EnseignantRepository repository) {
        this.repository = repository;
    }

    public List<Enseignant> getAll() {
        return repository.findAll();
    }

    public Enseignant getById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Enseignant introuvable"));
    }

    @Transactional
    public Enseignant create(Enseignant values) {
        return save(new Enseignant(), values);
    }

    @Transactional
    public Enseignant update(Long id, Enseignant values) {
        return save(getById(id), values);
    }

    private Enseignant save(Enseignant entity, Enseignant values) {
        entity.setNom(values.getNom().strip());
        entity.setMatiere(values.getMatiere().strip());
        entity.setChargeMaxHeures(values.getChargeMaxHeures());
        return repository.save(entity);
    }
}
