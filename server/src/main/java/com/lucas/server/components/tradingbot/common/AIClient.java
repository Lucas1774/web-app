package com.lucas.server.components.tradingbot.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.config.AIProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.Getter;

import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.CONTENT;
import static com.lucas.server.common.Constants.ROLE;

public class AIClient {

    @Getter
    private final AIProperties.DeploymentProperties config;
    @Getter
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpRequestClient httpClient;

    public AIClient(AIProperties.DeploymentProperties config, RateLimiter rateLimiter, ObjectMapper objectMapper, HttpRequestClient httpClient) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String complete(List<JsonNode> prompt) throws ClientException {
        JsonNode body = objectMapper.valueToTree(
                Map.of(
                        "model", config.model(),
                        "messages", prompt.stream()
                                .map(m -> Map.of(
                                                "role", m.get(ROLE).asText(),
                                                CONTENT, m.get(CONTENT).asText()
                                        )
                                )
                                .toList(),
                        "max_tokens", config.maxTokens(),
                        "temperature", config.temperature()
                )

        );

        return httpClient.fetch(config.url(), config.apiKey(), body).get("choices").get(0).get("message").get(CONTENT).asText()
                .replace("```", "")
                .replace("json", "");
    }
}
