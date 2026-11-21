package com.planifscolaire.presences.client;

import com.planifscolaire.presences.dto.UserDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "service-utilisateurs", url = "${app.utilisateurs-url:}")
public interface UtilisateursClient {
    @GetMapping("/api/users/{id}")
    UserDto user(@PathVariable("id") Long id);
}
