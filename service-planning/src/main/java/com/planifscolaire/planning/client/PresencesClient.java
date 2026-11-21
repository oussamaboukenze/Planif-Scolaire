package com.planifscolaire.planning.client;

import com.planifscolaire.planning.dto.AbsenceDto;
import com.planifscolaire.planning.dto.AppelDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "service-presences", url = "${app.presences-url:}")
public interface PresencesClient {
    @GetMapping("/api/appels/cours/{coursId}")
    AppelDto appelDuCours(@PathVariable("coursId") Long coursId);

    @GetMapping("/api/appels/{id}/absences")
    List<AbsenceDto> absences(@PathVariable("id") Long appelId);
}
