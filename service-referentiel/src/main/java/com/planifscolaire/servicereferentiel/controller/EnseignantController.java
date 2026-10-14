package com.planifscolaire.servicereferentiel.controller;

import com.planifscolaire.servicereferentiel.dto.EnseignantDto;
import com.planifscolaire.servicereferentiel.entite.Enseignant;
import com.planifscolaire.servicereferentiel.mapper.EnseignantMapper;
import com.planifscolaire.servicereferentiel.service.EnseignantService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {
    private final EnseignantService service;
    private final EnseignantMapper mapper;

    public EnseignantController(EnseignantService service, EnseignantMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<EnseignantDto>> getAll() {
        return ResponseEntity.ok(service.getAll().stream().map(mapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnseignantDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<EnseignantDto> create(@Valid @RequestBody EnseignantDto dto) {
        Enseignant entity = service.create(mapper.toEntity(dto));
        return ResponseEntity.created(URI.create("/api/enseignants/" + entity.getId()))
                .body(mapper.toDto(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnseignantDto> update(
            @PathVariable Long id, @Valid @RequestBody EnseignantDto dto) {
        return ResponseEntity.ok(mapper.toDto(service.update(id, mapper.toEntity(dto))));
    }
}
