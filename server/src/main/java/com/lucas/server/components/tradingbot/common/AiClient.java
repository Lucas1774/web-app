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

import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, Object> bodyMap = new HashMap<>();

        if (config.fallbackModels() != null && !config.fallbackModels().isEmpty()) {
            ArrayList<String> models = new ArrayList<>();
            models.add(config.model());
            models.addAll(config.fallbackModels());
            bodyMap.put("models", models);
        } else {
            bodyMap.put("model", config.model());
        }

        bodyMap.put("messages",
                prompt.stream()
                        .map(m -> Map.of(ROLE, m.get(ROLE).asText(), CONTENT, sanitizeHtml(m.get(CONTENT).asText())))
                        .toList());
        bodyMap.put("max_tokens", config.maxTokens());
        bodyMap.put("temperature", config.temperature());


        JsonNode body = objectMapper.valueToTree(bodyMap);

        return responseSanitizer.apply(httpClient.fetchFromJson(config.url(), config.apiKey(), body, true)
                .get("choices")
                .get(0)
                .get("message")
                .get(CONTENT)
                .asText());
    }
}
