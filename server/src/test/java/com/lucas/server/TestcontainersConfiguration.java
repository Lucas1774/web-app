package com.lucas.server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withInitScript("init.sql");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return postgresContainer;
    }
}
