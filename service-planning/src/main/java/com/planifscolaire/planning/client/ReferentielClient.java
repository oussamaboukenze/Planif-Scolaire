package com.planifscolaire.planning.client;

import com.planifscolaire.planning.dto.ClasseDto;
import com.planifscolaire.planning.dto.EnseignantDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "service-referentiel")
public interface ReferentielClient {
    @GetMapping("/api/classes/{id}")
    ClasseDto classe(@PathVariable("id") Long id);

    @GetMapping("/api/enseignants/{id}")
    EnseignantDto enseignant(@PathVariable("id") Long id);
}
