package com.lucas.server.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Random;

@Configuration
public class Config {

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.connectTimeout(Duration.ofMinutes(10))
                .readTimeout(Duration.ofMinutes(10))
                .build();
    }
}
