package com.example.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

@SpringBootTest
class ConfigServerApplicationTests {

    @Autowired
    private EnvironmentRepository repository;

    @Test
    void servesLocalConfigurationForEveryClient() {
        for (String service : new String[] {
                "service-referentiel", "service-planning", "service-presences", "gateway-service"}) {
            var environment = repository.findOne(service, "default", null);
            assertThat(environment.getPropertySources()).as(service).isNotEmpty();
            assertThat(environment.getPropertySources().get(0).getSource().containsKey("server.port")).isTrue();
        }
    }
}
