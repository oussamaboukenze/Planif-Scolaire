package com.planifscolaire.presences.controller;

import com.planifscolaire.presences.dto.AbsenceDto;
import com.planifscolaire.presences.dto.AppelDto;
import com.planifscolaire.presences.dto.CreateAbsenceDto;
import com.planifscolaire.presences.entite.Absence;
import com.planifscolaire.presences.mapper.AbsenceMapper;
import com.planifscolaire.presences.mapper.AppelMapper;
import com.planifscolaire.presences.service.PresenceService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/appels")
public class PresenceController {
    private final PresenceService service;
    private final AppelMapper appelMapper;
    private final AbsenceMapper absenceMapper;

    public PresenceController(
            PresenceService service, AppelMapper appelMapper, AbsenceMapper absenceMapper) {
        this.service = service;
        this.appelMapper = appelMapper;
        this.absenceMapper = absenceMapper;
    }

    @GetMapping
    public ResponseEntity<List<AppelDto>> getAll() {
        return ResponseEntity.ok(service.getAll().stream().map(appelMapper::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppelDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appelMapper.toDto(service.get(id)));
    }

    @GetMapping("/cours/{coursId}")
    public ResponseEntity<AppelDto> getByCoursId(@PathVariable Long coursId) {
        return ResponseEntity.ok(appelMapper.toDto(service.getByCoursId(coursId)));
    }

    @GetMapping("/{id}/absences")
    public ResponseEntity<List<AbsenceDto>> getAbsences(@PathVariable Long id) {
        return ResponseEntity.ok(
                service.getAbsences(id).stream().map(absenceMapper::toDto).toList());
    }

    @PostMapping("/{id}/absences")
    public ResponseEntity<AbsenceDto> record(
            @PathVariable Long id, @Valid @RequestBody CreateAbsenceDto request) {
        Absence absence = service.record(id, request);
        return ResponseEntity.created(
                        URI.create("/api/appels/" + id + "/absences/" + absence.getId()))
                .body(absenceMapper.toDto(absence));
    }
}
