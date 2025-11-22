package com.lucas.server.components.tradingbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.utils.SlidingWindowRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Configuration
public class HttpClientConfig {

    @Bean
    public Map<String, SlidingWindowRateLimiter> rateLimiter(AIProperties aiProps) {
        Map<String, SlidingWindowRateLimiter> res = new HashMap<>();
        res.put(AI_PER_SECOND_RATE_LIMITER, new SlidingWindowRateLimiter(1, Duration.ofSeconds(1)));
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
                                new SlidingWindowRateLimiter(1, Duration.ofMinutes(1).dividedBy(config.requestsPerMinute())),
                                objectMapper,
                                httpClient
                        )
                ));
        res.putAll(aiProps.getDeployments().stream()
                .filter(d -> d.name().contains("-specialist"))
                .collect(Collectors.toMap(
                        AIProperties.DeploymentProperties::name,
                        config -> new AIClient(
                                config,
                                res.get(config.name().replace("-specialist", EMPTY_STRING)).getRateLimiter(),
                                objectMapper,
                                httpClient
                        )
                )));
        return res;
    }
}
