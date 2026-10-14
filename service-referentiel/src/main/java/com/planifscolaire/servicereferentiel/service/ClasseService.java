package com.planifscolaire.servicereferentiel.service;

import com.planifscolaire.servicereferentiel.entite.Classe;
import com.planifscolaire.servicereferentiel.exception.RessourceIntrouvableException;
import com.planifscolaire.servicereferentiel.repository.ClasseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClasseService {
    private final ClasseRepository repository;

    public ClasseService(ClasseRepository repository) {
        this.repository = repository;
    }

    public List<Classe> getAll() {
        return repository.findAll();
    }

    public Classe getById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Classe introuvable"));
    }

    @Transactional
    public Classe create(Classe values) {
        return save(new Classe(), values);
    }

    @Transactional
    public Classe update(Long id, Classe values) {
        return save(getById(id), values);
    }

    private Classe save(Classe entity, Classe values) {
        entity.setNom(values.getNom().strip());
        entity.setNiveau(values.getNiveau().strip());
        entity.setEffectifMax(values.getEffectifMax());
        return repository.save(entity);
    }
}
