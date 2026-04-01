package com.lucas.server.components.tradingbot.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.config.AIProperties;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.ratelimiter.CompletionSlidingWindowRateLimiter;
import lombok.Getter;

import java.util.Map;
import java.util.function.UnaryOperator;

import static com.lucas.server.common.Constants.*;

public class AIClient {

    @Getter
    private final AIProperties.DeploymentProperties config;
    @Getter
    private final CompletionSlidingWindowRateLimiter rateLimiter;
    @Getter
    private final CompletionSlidingWindowRateLimiter concurrentRequestsRateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpRequestClient httpClient;
    private final UnaryOperator<String> responseSanitizer;

    public AIClient(AIProperties.DeploymentProperties config,
                    CompletionSlidingWindowRateLimiter rateLimiter,
                    CompletionSlidingWindowRateLimiter concurrentRequestsRateLimiter,
                    ObjectMapper objectMapper,
                    HttpRequestClient httpClient,
                    UnaryOperator<String> responseSanitizer) {
        this.config = config;
        this.rateLimiter = rateLimiter;
        this.concurrentRequestsRateLimiter = concurrentRequestsRateLimiter;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.responseSanitizer = responseSanitizer;
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

        return responseSanitizer.apply(httpClient.fetch(config.url(), config.apiKey(), body, true)
                .get("choices").get(0).get("message").get(CONTENT).asText());
    }
}
