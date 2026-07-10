package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.config.AiProperties;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.ratelimiter.DefaultSlidingWindowRateLimiter;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.lucas.server.common.Constants.AiProvider.GOOGLE;
import static com.lucas.server.common.Constants.AiProvider.OPENROUTER;
import static com.lucas.server.common.Constants.CONTENT;
import static com.lucas.server.common.Constants.PARTS;
import static com.lucas.server.common.Constants.ROLE;
import static com.lucas.server.common.Constants.sanitizeHtml;

@RequiredArgsConstructor
public class AiClient {

    @Getter
    private final AiProperties.DeploymentProperties config;
    @Getter
    private final SlidingWindowRateLimiter moreRestrictiveRateLimiter;
    @Getter
    private final SlidingWindowRateLimiter lessRestrictiveRateLimiter;
    @Getter
    @Nullable
    private final DefaultSlidingWindowRateLimiter apiKeyRateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpRequestClient httpClient;
    private final UnaryOperator<String> responseSanitizer;

    public String complete(OrderedIndexedSet<JsonNode> prompt) throws ClientException {
        Map<String, Object> bodyMap = new HashMap<>();

        if (GOOGLE == config.provider()) {
            List<JsonNode> systemMessages =
                    prompt.stream().filter(m -> "system".equals(m.get(ROLE).asString())).toList();
            if (!systemMessages.isEmpty()) {
                bodyMap.put("systemInstruction",
                        Map.of(PARTS,
                                systemMessages.stream()
                                        .map(m -> Map.of("text", sanitizeHtml(m.get(CONTENT).asString())))
                                        .toList()));
            }

            bodyMap.put("contents",
                    prompt.stream()
                            .filter(m -> !"system".equals(m.get(ROLE).asString()))
                            .map(m -> Map.of("role",
                                    "assistant".equals(m.get(ROLE).asString()) ? "model" : m.get(ROLE).asString(),
                                    PARTS,
                                    List.of(Map.of("text", sanitizeHtml(m.get(CONTENT).asString())))))
                            .toList());

            Map<String, Object> generationConfig = new HashMap<>(Map.of("maxOutputTokens",
                    config.maxTokens(),
                    "thinkingConfig",
                    Map.of("thinkingLevel", config.thinkingLevel())));
            if (null != config.temperature()) {
                generationConfig.put("temperature", config.temperature());
            }
            bodyMap.put("generationConfig", generationConfig);

            return responseSanitizer.apply(httpClient.post(config.url(), null, objectMapper.valueToTree(bodyMap), true)
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get(PARTS)
                    .get(0)
                    .get("text")
                    .asString());
        } else {
            if (OPENROUTER == config.provider() && null != config.fallbackModels() && !config.fallbackModels()
                    .isEmpty()) {
                ArrayList<String> models = new ArrayList<>();
                models.add(config.model());
                models.addAll(config.fallbackModels());
                bodyMap.put("models", models);
            } else {
                bodyMap.put("model", config.model());
            }

            bodyMap.put("messages",
                    prompt.stream()
                            .map(m -> Map.of(ROLE,
                                    m.get(ROLE).asString(),
                                    CONTENT,
                                    sanitizeHtml(m.get(CONTENT).asString())))
                            .toList());

            bodyMap.put("max_tokens", config.maxTokens());

            bodyMap.put("temperature", config.temperature());

            return responseSanitizer.apply(httpClient.post(config.url(),
                    config.apiKey(),
                    objectMapper.valueToTree(bodyMap),
                    true).get("choices").get(0).get("message").get(CONTENT).asString());
        }
    }
}
