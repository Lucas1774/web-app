package com.lucas.server.components.tradingbot.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.config.AIProperties;
import com.lucas.utils.SlidingWindowRateLimiter;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.Getter;

import java.util.Map;

import static com.lucas.server.common.Constants.*;
import static com.lucas.utils.Utils.EMPTY_STRING;

public class AIClient {

    @Getter
    private final AIProperties.DeploymentProperties config;
    @Getter
    private final SlidingWindowRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpRequestClient httpClient;

    public AIClient(AIProperties.DeploymentProperties config, SlidingWindowRateLimiter rateLimiter, ObjectMapper objectMapper, HttpRequestClient httpClient) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public String complete(OrderedIndexedSet<JsonNode> prompt) throws ClientException {
        JsonNode body = objectMapper.valueToTree(
                Map.of(
                        "model", config.model(),
                        "messages", prompt.stream()
                                .map(m -> Map.of(
                                                ROLE, m.get(ROLE).asText(),
                                                CONTENT, sanitizeHtml(m.get(CONTENT).asText())
                                        )
                                )
                                .toList(),
                        "max_tokens", config.maxTokens(),
                        "temperature", config.temperature()
                )

        );

        return httpClient.fetch(config.url(), config.apiKey(), body, true).get("choices").get(0).get("message").get(CONTENT).asText()
                .replace("```", EMPTY_STRING)
                .replace("json", EMPTY_STRING);
    }
}
