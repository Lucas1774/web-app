package com.lucas.server.components.tradingbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.components.tradingbot.common.AIClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class AIClientAutoconfig {

    @Bean
    public Map<String, AIClient> clients(HttpRequestClient httpClient, AIProperties aiProps, ObjectMapper objectMapper) {
        return aiProps.getDeployments().stream()
                .collect(Collectors.toMap(
                        AIProperties.DeploymentProperties::name,
                        config -> new AIClient(
                                config,
                                new SlidingWindowRateLimiter(config.requestsPerMinute(), Duration.ofMinutes(1)),
                                objectMapper,
                                httpClient
                        )
                ));
    }
}
