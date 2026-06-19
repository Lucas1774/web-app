package com.lucas.server.components.tradingbot.config;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.utils.ratelimiter.CompletionSlidingWindowRateLimiter;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.TWELVEDATA_RATE_LIMITER;
import static com.lucas.server.common.Constants.YAHOO_FINANCE_RATE_LIMITER;
import static com.lucas.server.common.Constants.getFinnhubRateLimiterNames;
import static com.lucas.server.common.Constants.getModelsWithThinkingBlock;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Configuration
public class HttpClientConfig {

    @Bean
    public Map<String, SlidingWindowRateLimiter> rateLimiters() {
        Map<String, SlidingWindowRateLimiter> res = new HashMap<>();
        res.put(TWELVEDATA_RATE_LIMITER, new SlidingWindowRateLimiter(8, Duration.ofMinutes(1)));
        res.put(YAHOO_FINANCE_RATE_LIMITER, new SlidingWindowRateLimiter(1, Duration.ofSeconds(1).dividedBy(4)));
        // spread requests are needed for Finnhub, even if its documentation suggests otherwise (60 / minute).
        getFinnhubRateLimiterNames().forEach(name -> res.put(name,
                new SlidingWindowRateLimiter(1, Duration.ofSeconds(1))));

        return res;
    }

    @Bean
    public Map<String, AiClient> clients(HttpRequestClient httpClient,
                                         AiProperties aiProps,
                                         ObjectMapper objectMapper) {
        Map<String, SlidingWindowRateLimiter> rateLimiters = aiProps.getDeployments()
                .stream()
                .map(AiProperties.DeploymentProperties::apiKey)
                .collect(Collectors.toUnmodifiableMap(Function.identity(),
                        _ -> new SlidingWindowRateLimiter(24, Duration.ofMinutes(1)),
                        (a, _) -> a));
        Map<String, AiClient> res = aiProps.getDeployments()
                .stream()
                .filter(d -> !d.name().contains("specialist"))
                .collect(Collectors.toMap(AiProperties.DeploymentProperties::name,
                        config -> new AiClient(config,
                                new CompletionSlidingWindowRateLimiter(config.requestsPerMinute(),
                                        Duration.ofMinutes(1)),
                                new CompletionSlidingWindowRateLimiter(config.concurrentRequests(),
                                        Duration.ofSeconds(1)),
                                rateLimiters.get(config.apiKey()),
                                objectMapper,
                                httpClient,
                                sanitizer(getModelsWithThinkingBlock().contains(config.name())))));
        res.putAll(aiProps.getDeployments()
                .stream()
                .filter(d -> d.name().contains("-specialist"))
                .collect(Collectors.toUnmodifiableMap(AiProperties.DeploymentProperties::name, config -> {
                    String baseName = config.name().replace("-specialist", EMPTY_STRING);
                    AiClient baseClient = res.get(baseName);
                    return new AiClient(config,
                            baseClient.getRateLimiter(),
                            baseClient.getConcurrentRequestsRateLimiter(),
                            baseClient.getApiKeyRateLimiter(),
                            objectMapper,
                            httpClient,
                            sanitizer(getModelsWithThinkingBlock().contains(baseName)));
                })));

        return Map.copyOf(res);
    }

    private static UnaryOperator<String> sanitizer(boolean stripThinking) {
        return raw -> {
            String result = raw;
            if (stripThinking) {
                int end = result.indexOf("</think>");
                if (0 <= end) {
                    result = result.substring(end + "</think>".length());
                }
            }

            return result.replace("```json", EMPTY_STRING).replace("```", EMPTY_STRING).trim();
        };
    }
}
