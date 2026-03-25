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

    private static final String THINK_CLOSE_TAG = "</think>";
    private static final UnaryOperator<String> STRIP_THINKING_BLOCK =
            raw -> {
                int end = raw.indexOf(THINK_CLOSE_TAG);
                if (0 <= end) {
                    return raw.substring(end + THINK_CLOSE_TAG.length()).trim();
                }
                return raw;
            };

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
                                getModelsWithThinkingBlock().contains(config.name()) ? STRIP_THINKING_BLOCK : UnaryOperator.identity()
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
                                    getModelsWithThinkingBlock().contains(baseName) ? STRIP_THINKING_BLOCK : UnaryOperator.identity()
                            );
                        }
                )));
        return res;
    }
}
