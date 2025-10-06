package com.lucas.server;

import com.lucas.server.common.MqttPublisher;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
public class TestConfiguration {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg17"))
            .withInitScript("init.sql");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return postgresContainer;
    }

    @Bean
    @Primary
    MqttPublisher mqttPublisher() {
        MqttPublisher res = mock(MqttPublisher.class);
        doNothing().when(res).publish(anyString(), anyString());
        return res;
    }
}
