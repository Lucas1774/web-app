package com.lucas.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SerializationConfig {

    private final ObjectMapper mapper;

    public SerializationConfig(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void customize() {
        this.mapper.registerModule(new JavaTimeModule());
    }
}
