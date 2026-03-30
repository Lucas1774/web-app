package com.lucas.server.components.tradingbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.utils.CompletionSlidingWindowRateLimiter;
import com.lucas.utils.SlidingWindowRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Configuration
public class HttpClientConfig {

    private static UnaryOperator<String> sanitizer(boolean stripThinking) {
        return raw -> {
            String result = raw;
            if (stripThinking) {
                int end = result.indexOf("</think>");
                if (0 <= end) {
                    result = result.substring(end + "</think>".length());
                }
            }

            return result.replace("```json", EMPTY_STRING)
                    .replace("```", EMPTY_STRING)
                    .trim();
        };
    }

    @Bean
    public Map<String, SlidingWindowRateLimiter> rateLimiter(AIProperties aiProps) {
        Map<String, SlidingWindowRateLimiter> res = new HashMap<>();
        res.put(TWELVEDATA_RATE_LIMITER, new SlidingWindowRateLimiter(8, Duration.ofMinutes(1)));
        res.put(YAHOO_FINANCE_RATE_LIMITER, new SlidingWindowRateLimiter(1, Duration.ofSeconds(1).dividedBy(4)));
        getFinnhubRateLimiterNames().forEach(name -> res.put(name,
                new SlidingWindowRateLimiter(60, Duration.ofMinutes(1), Duration.ofMinutes(1))));
        aiProps.getDeployments()
                .forEach(config -> res.put(config.apiKey(), new SlidingWindowRateLimiter(24, Duration.ofMinutes(1))));

        return res;
    }

    @Bean
    public Map<String, AIClient> clients(HttpRequestClient httpClient, AIProperties aiProps, ObjectMapper objectMapper) {
        Map<String, AIClient> res = aiProps.getDeployments().stream()
                .filter(d -> !d.name().contains("specialist"))
                .collect(Collectors.toMap(
                        AIProperties.DeploymentProperties::name,
                        config -> new AIClient(
                                config,
                                new CompletionSlidingWindowRateLimiter(config.requestsPerMinute(), Duration.ofMinutes(1)),
                                new CompletionSlidingWindowRateLimiter(config.concurrentRequests(), Duration.ofSeconds(1)),
                                objectMapper,
                                httpClient,
                                sanitizer(getModelsWithThinkingBlock().contains(config.name()))
                        )
                ));
        res.putAll(aiProps.getDeployments().stream()
                .filter(d -> d.name().contains("-specialist"))
                .collect(Collectors.toMap(
                        AIProperties.DeploymentProperties::name,
                        config -> {
                            String baseName = config.name().replace("-specialist", EMPTY_STRING);
                            return new AIClient(
                                    config,
                                    res.get(baseName).getRateLimiter(),
                                    res.get(baseName).getConcurrentRequestsRateLimiter(),
                                    objectMapper,
                                    httpClient,
                                    sanitizer(getModelsWithThinkingBlock().contains(baseName))
                            );
                        }
                )));
        return res;
    }
}
