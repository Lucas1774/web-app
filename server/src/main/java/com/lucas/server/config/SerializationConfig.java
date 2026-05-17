package com.lucas.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SerializationConfig {

    private final ObjectMapper mapper;

    @PostConstruct
    public void customize() {
        mapper.registerModule(new JavaTimeModule());
    }
}
