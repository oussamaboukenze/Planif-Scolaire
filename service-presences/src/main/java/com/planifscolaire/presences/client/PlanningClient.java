package com.planifscolaire.presences.client;

import com.planifscolaire.presences.dto.CoursDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "service-planning", url = "${app.planning-url:}")
public interface PlanningClient {
    @GetMapping("/api/cours/{id}")
    CoursDto cours(@PathVariable("id") Long id);
}
