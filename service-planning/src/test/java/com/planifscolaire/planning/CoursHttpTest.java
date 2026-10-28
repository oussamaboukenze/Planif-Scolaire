package com.planifscolaire.planning;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.planifscolaire.planning.controller.CoursController;
import com.planifscolaire.planning.exception.GlobalExceptionHandler;
import com.planifscolaire.planning.mapper.CoursMapper;
import com.planifscolaire.planning.service.CoursService;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Map;

class CoursHttpTest {
    private CoursService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(CoursService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new CoursController(service, Mappers.getMapper(CoursMapper.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @CsvSource({"404,404", "503,503", "500,502", "403,502"})
    void translatesRemoteHttpFailures(int remote, int expected) throws Exception {
        Request request = Request.create(Request.HttpMethod.GET, "http://referentiel/api/classes/1",
                Map.of(), (byte[]) null, java.nio.charset.StandardCharsets.UTF_8, null);
        when(service.getDetail(1L)).thenThrow(FeignException.errorStatus("classe",
                Response.builder().status(remote).reason("remote").request(request).build()));
        mvc.perform(get("/api/cours/1/detail"))
                .andExpect(status().is(expected))
                .andExpect(jsonPath("status").value(expected));
    }

    @Test
    void translatesCommunicationFailure() throws Exception {
        FeignException failure = mock(FeignException.class);
        when(failure.status()).thenReturn(-1);
        when(service.getDetail(1L)).thenThrow(failure);
        mvc.perform(get("/api/cours/1/detail"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("detail").value("Échec de communication avec un service"));
    }

    @Test
    void creationRequiresKey() throws Exception {
        mvc.perform(post("/api/cours").contentType("application/json").content(validBody()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void duplicateKeyIsBadRequest() throws Exception {
        when(service.create(any(), eq("used-key")))
                .thenThrow(new IllegalArgumentException("Opération déjà traitée"));
        mvc.perform(post("/api/cours").header("idempotencyKey", "used-key")
                .contentType("application/json").content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("detail").value("Opération déjà traitée"));
    }

    private String validBody() {
        return """
                {"classeId":1,"enseignantId":1,"matiere":"Maths",
                 "dateHeure":"2026-11-02T09:00:00","salle":"A1"}
                """;
    }
}
