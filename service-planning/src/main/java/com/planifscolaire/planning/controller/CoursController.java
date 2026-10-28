package com.planifscolaire.planning.controller;

import com.planifscolaire.planning.dto.CoursDto;
import com.planifscolaire.planning.dto.CoursDtoFull;
import com.planifscolaire.planning.dto.CreateCoursDto;
import com.planifscolaire.planning.dto.StatutCoursDto;
import com.planifscolaire.planning.entite.Cours;
import com.planifscolaire.planning.mapper.CoursMapper;
import com.planifscolaire.planning.service.CoursService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cours")
public class CoursController {
    private final CoursService service;
    private final CoursMapper mapper;

    public CoursController(CoursService service, CoursMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<CoursDto>> getAll(@RequestParam(required = false) Long classeId) {
        return ResponseEntity.ok(service.getAll(classeId).stream().map(mapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoursDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toDto(service.get(id)));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<CoursDto> transition(
            @PathVariable Long id, @Valid @RequestBody StatutCoursDto request) {
        return ResponseEntity.ok(mapper.toDto(service.transition(id, request.getStatut())));
    }

    @PostMapping
    public ResponseEntity<CoursDto> create(
            @Valid @RequestBody CreateCoursDto request,
            @RequestHeader(name = "idempotencyKey") String idempotencyKey) {

        Cours cours = service.create(request, idempotencyKey);

        return ResponseEntity.created(URI.create("/api/cours/" + cours.getId()))
                .body(mapper.toDto(cours));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<CoursDtoFull> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDetail(id));
    }
}
