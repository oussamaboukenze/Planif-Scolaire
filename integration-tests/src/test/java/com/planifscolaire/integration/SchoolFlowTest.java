package com.planifscolaire.integration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.planifscolaire.planning.PlanningApplication;
import com.planifscolaire.presences.PresencesApplication;
import com.planifscolaire.servicereferentiel.ServiceReferentielApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@SpringJUnitConfig(SchoolFlowTest.Config.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {"course-scheduled", "course-cancelled", "absence-recorded"})
@DirtiesContext
class SchoolFlowTest {
    @Configuration
    static class Config {}

    @Autowired EmbeddedKafkaBroker broker;
    private final HttpClient http =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
    private final JsonMapper json = JsonMapper.builder().build();

    ConfigurableApplicationContext start(Class<?> application, String module, String... extra) {
        List<String> args =
                new ArrayList<>(
                        List.of(
                                "--spring.config.location=file:../config-repo/" + module + ".properties",
                                "--spring.application.name=" + module,
                                "--spring.cloud.config.enabled=false",
                                "--server.port=0",
                                "--eureka.client.enabled=false",
                                "--spring.datasource.url=jdbc:h2:mem:"
                                        + module
                                        + "-flow;DB_CLOSE_DELAY=-1",
                                "--spring.jpa.hibernate.ddl-auto=create-drop",
                                "--spring.kafka.bootstrap-servers=" + broker.getBrokersAsString(),
                                "--app.export.enabled=false",
                                "--spring.main.banner-mode=off",
                                "--logging.level.root=WARN",
                                "--app.outbox.delay=100"));
        args.addAll(List.of(extra));
        return new SpringApplicationBuilder(application).run(args.toArray(String[]::new));
    }

    String url(ConfigurableApplicationContext context) {
        return "http://localhost:"
                + ((WebServerApplicationContext) context).getWebServer().getPort();
    }

    JsonNode request(String method, String url, String body, int expected) throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json");
        var response =
                http.send(
                        builder.method(
                                        method,
                                        body == null
                                                ? HttpRequest.BodyPublishers.noBody()
                                                : HttpRequest.BodyPublishers.ofString(body))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("%s %s: %s", method, url, response.body())
                .isEqualTo(expected);
        return json.readTree(response.body());
    }

    @Test
    void completeHttpFeignKafkaWorkflow() throws Exception {
        try (var reference = start(ServiceReferentielApplication.class, "service-referentiel");
                var planning =
                        start(
                                PlanningApplication.class,
                                "service-planning",
                                "--app.referentiel-url=" + url(reference));
                var presences =
                        start(
                                PresencesApplication.class,
                                "service-presences",
                                "--app.planning-url=" + url(planning))) {
            String r = url(reference), p = url(planning), a = url(presences);
            for (String endpoint : List.of(r + "/api/classes/999999", r + "/api/enseignants/999999",
                    p + "/api/cours/999999", a + "/api/appels/999999")) {
                assertThat(request("GET", endpoint, null, 404).path("status").asInt()).isEqualTo(404);
            }
            assertThat(request("POST", r + "/api/enseignants",
                    "{\"nom\":\"\",\"matiere\":\"\",\"chargeMaxHeures\":0}", 400)
                    .path("detail").asText()).contains("nom", "matiere", "chargeMaxHeures");
            assertThat(request("POST", p + "/api/cours", "{}", 400)
                    .path("detail").asText()).contains("classeId", "enseignantId", "dateHeure");
            assertThat(request("POST", a + "/api/appels/999999/absences", "{}", 400)
                    .path("detail").asText()).contains("etudiantNom", "motif");
            request("POST", a + "/api/appels/999999/absences",
                    "{\"etudiantNom\":\"Sara\",\"motif\":\"Maladie\",\"justifiee\":true}", 404);
            request(
                    "POST",
                    r + "/api/classes",
                    "{\"nom\":\"\",\"niveau\":\"6e\",\"effectifMax\":0}",
                    400);
            long classe =
                    request(
                                    "POST",
                                    r + "/api/classes",
                                    "{\"nom\":\"6A\",\"niveau\":\"6e\",\"effectifMax\":30}",
                                    201)
                            .path("id")
                            .asLong();
            long prof =
                    request(
                                    "POST",
                                    r + "/api/enseignants",
                                    "{\"nom\":\"Ali\",\"matiere\":\"Maths\",\"chargeMaxHeures\":20}",
                                    201)
                            .path("id")
                            .asLong();
            String body =
                    "{\"classeId\":"
                            + classe
                            + ",\"enseignantId\":"
                            + prof
                            + ",\"matiere\":\"Maths\",\"dateHeure\":\"2026-11-02T09:00:00\",\"salle\":\"A1\"}";
            request(
                    "POST",
                    p + "/api/cours",
                    body.replace("\"classeId\":" + classe, "\"classeId\":999999"),
                    422);
            long cours = request("POST", p + "/api/cours", body, 201).path("id").asLong();
            request("POST", p + "/api/cours", body, 409);
            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(
                            () -> request("GET", a + "/api/appels/cours/" + cours, null, 200));
            long appel =
                    request("GET", a + "/api/appels/cours/" + cours, null, 200).path("id").asLong();
            String absence =
                    "{\"etudiantNom\":\"Sara Ali\",\"motif\":\"Maladie\",\"justifiee\":true}";
            request("POST", a + "/api/appels/" + appel + "/absences", absence, 409);
            request(
                    "PATCH",
                    p + "/api/cours/" + cours + "/statut",
                    "{\"statut\":\"REALISE\"}",
                    200);
            request("POST", a + "/api/appels/" + appel + "/absences", absence, 201);
            request("POST", a + "/api/appels/" + appel + "/absences", absence, 409);
            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(
                            () ->
                                    assertThat(
                                                    request(
                                                                    "GET",
                                                                    p + "/api/cours/" + cours,
                                                                    null,
                                                                    200)
                                                            .path("nbAbsencesSignalees")
                                                            .asInt())
                                            .isEqualTo(1));
            long cancelled =
                    request("POST", p + "/api/cours", body.replace("09:00:00", "10:00:00"), 201)
                            .path("id")
                            .asLong();
            request(
                    "PATCH",
                    p + "/api/cours/" + cancelled + "/statut",
                    "{\"statut\":\"ANNULE\"}",
                    200);
            await().atMost(Duration.ofSeconds(30))
                    .untilAsserted(
                            () ->
                                    assertThat(
                                                    request(
                                                                    "GET",
                                                                    a
                                                                            + "/api/appels/cours/"
                                                                            + cancelled,
                                                                    null,
                                                                    200)
                                                            .path("statut")
                                                            .asText())
                                            .isEqualTo("CLOTURE"));
            request(
                    "PATCH",
                    p + "/api/cours/" + cancelled + "/statut",
                    "{\"statut\":\"REALISE\"}",
                    409);
        }
    }
}
