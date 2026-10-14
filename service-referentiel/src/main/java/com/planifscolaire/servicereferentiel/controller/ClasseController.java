package com.planifscolaire.servicereferentiel.controller;

import com.planifscolaire.servicereferentiel.dto.ClasseDto;
import com.planifscolaire.servicereferentiel.entite.Classe;
import com.planifscolaire.servicereferentiel.mapper.ClasseMapper;
import com.planifscolaire.servicereferentiel.service.ClasseService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClasseController {
    private final ClasseService service;
    private final ClasseMapper mapper;

    public ClasseController(ClasseService service, ClasseMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ClasseDto>> getAll() {
        return ResponseEntity.ok(service.getAll().stream().map(mapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClasseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ClasseDto> create(@Valid @RequestBody ClasseDto dto) {
        Classe entity = service.create(mapper.toEntity(dto));
        return ResponseEntity.created(URI.create("/api/classes/" + entity.getId()))
                .body(mapper.toDto(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClasseDto> update(
            @PathVariable Long id, @Valid @RequestBody ClasseDto dto) {
        return ResponseEntity.ok(mapper.toDto(service.update(id, mapper.toEntity(dto))));
    }
}
