package com.planifscolaire.utilisateurs.service;

import com.planifscolaire.utilisateurs.entite.User;
import com.planifscolaire.utilisateurs.exception.ConflitMetierException;
import com.planifscolaire.utilisateurs.exception.RessourceIntrouvableException;
import com.planifscolaire.utilisateurs.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> getAll() {
        return repository.findAll();
    }

    public User getById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
    }

    @Transactional
    public User create(User values) {
        if (repository.existsByEmailIgnoreCase(values.getEmail().strip())) {
            throw new ConflitMetierException("Cet email est déjà utilisé");
        }
        return save(new User(), values);
    }

    @Transactional
    public User update(Long id, User values) {
        User entity = getById(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(values.getEmail().strip(), id)) {
            throw new ConflitMetierException("Cet email est déjà utilisé");
        }
        return save(entity, values);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getById(id));
    }

    private User save(User entity, User values) {
        entity.setNom(values.getNom().strip());
        entity.setEmail(values.getEmail().strip().toLowerCase());
        entity.setRole(values.getRole());
        entity.setActif(values.isActif());
        return repository.save(entity);
    }
}
