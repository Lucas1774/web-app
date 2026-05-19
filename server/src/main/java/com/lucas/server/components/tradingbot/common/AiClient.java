package com.lucas.server.components.tradingbot.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.config.AiProperties;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.ratelimiter.CompletionSlidingWindowRateLimiter;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.UnaryOperator;

import static com.lucas.server.common.Constants.CONTENT;
import static com.lucas.server.common.Constants.ROLE;
import static com.lucas.server.common.Constants.sanitizeHtml;

@RequiredArgsConstructor
public class AiClient {

    @Getter
    private final AiProperties.DeploymentProperties config;
    @Getter
    private final CompletionSlidingWindowRateLimiter rateLimiter;
    @Getter
    private final CompletionSlidingWindowRateLimiter concurrentRequestsRateLimiter;
    @Getter
    private final SlidingWindowRateLimiter apiKeyRateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpRequestClient httpClient;
    private final UnaryOperator<String> responseSanitizer;

    public String complete(OrderedIndexedSet<JsonNode> prompt) throws ClientException {
        JsonNode body = objectMapper.valueToTree(Map.of("model",
                config.model(),
                "messages",
                prompt.stream()
                        .map(m -> Map.of(ROLE, m.get(ROLE).asText(), CONTENT, sanitizeHtml(m.get(CONTENT).asText())))
                        .toList(),
                "max_tokens",
                config.maxTokens(),
                "temperature",
                config.temperature())

        );

        return responseSanitizer.apply(httpClient.fetch(config.url(), config.apiKey(), body, true)
                .get("choices")
                .get(0)
                .get("message")
                .get(CONTENT)
                .asText());
    }
}
