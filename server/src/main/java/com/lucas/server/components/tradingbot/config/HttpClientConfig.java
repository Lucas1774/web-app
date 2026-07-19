package com.lucas.server.components.tradingbot.config;

import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.utils.ratelimiter.CompletionSlidingWindowRateLimiter;
import com.lucas.utils.ratelimiter.DefaultSlidingWindowRateLimiter;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.AiProvider.GITHUB;
import static com.lucas.server.common.Constants.AiProvider.GOOGLE;
import static com.lucas.server.common.Constants.AiProvider.OPENROUTER;
import static com.lucas.server.common.Constants.TWELVEDATA_RATE_LIMITER;
import static com.lucas.server.common.Constants.YAHOO_FINANCE_RATE_LIMITER;
import static com.lucas.server.common.Constants.getFinnhubRateLimiterNames;
import static com.lucas.server.common.Constants.getModelsWithThinkingBlock;
import static com.lucas.utils.Utils.EMPTY_STRING;

@Configuration
public class HttpClientConfig {

    private static final Map<Constants.AiProvider, Function<AiProperties.DeploymentProperties, SlidingWindowRateLimiter>>
            providerToPerMinuteRateLimiter = Map.of(OPENROUTER,
            config -> new DefaultSlidingWindowRateLimiter(config.requestsPerMinute(), Duration.ofMinutes(1)),
            GOOGLE,
            config -> new DefaultSlidingWindowRateLimiter(config.requestsPerMinute(), Duration.ofMinutes(1)),
            GITHUB,
            config -> new CompletionSlidingWindowRateLimiter(config.requestsPerMinute(), Duration.ofMinutes(1)));
    private static final Map<Constants.AiProvider, Function<AiProperties.DeploymentProperties, SlidingWindowRateLimiter>>
            providerToConcurrentRateLimiter = Map.of(OPENROUTER,
            config -> new DefaultSlidingWindowRateLimiter(config.concurrentRequests(), Duration.ofSeconds(1)),
            GOOGLE,
            config -> new DefaultSlidingWindowRateLimiter(config.concurrentRequests(), Duration.ofSeconds(1)),
            GITHUB,
            config -> new CompletionSlidingWindowRateLimiter(config.concurrentRequests(), Duration.ofSeconds(1)));
    private static final String SPECIALIST = "-specialist";

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    @Bean
    public Map<String, DefaultSlidingWindowRateLimiter> rateLimiters() {
        Map<String, DefaultSlidingWindowRateLimiter> res = new HashMap<>();
        res.put(TWELVEDATA_RATE_LIMITER, new DefaultSlidingWindowRateLimiter(8, Duration.ofMinutes(1)));
        res.put(YAHOO_FINANCE_RATE_LIMITER, new DefaultSlidingWindowRateLimiter(1, Duration.ofSeconds(1).dividedBy(4)));
        // spread requests are needed for Finnhub, even if its documentation suggests otherwise (60 / minute).
        getFinnhubRateLimiterNames().forEach(name -> res.put(name,
                new DefaultSlidingWindowRateLimiter(1, Duration.ofSeconds(1))));

        return res;
    }

    @Bean
    public Map<String, AiClient> clients(HttpRequestClient httpClient,
                                         AiProperties aiProps,
                                         ObjectMapper objectMapper) {
        Map<String, DefaultSlidingWindowRateLimiter> rateLimiters = aiProps.getDeployments()
                .stream()
                .filter(d -> GITHUB.equals(d.provider()))
                .map(AiProperties.DeploymentProperties::apiKey)
                .collect(Collectors.toUnmodifiableMap(Function.identity(),
                        _ -> new DefaultSlidingWindowRateLimiter(24, Duration.ofMinutes(1)),
                        (a, _) -> a));
        Map<String, AiClient> res = aiProps.getDeployments()
                .stream()
                .filter(d -> !d.name().contains(SPECIALIST))
                .collect(Collectors.toMap(AiProperties.DeploymentProperties::name,
                        config -> new AiClient(config,
                                providerToPerMinuteRateLimiter.get(config.provider()).apply(config),
                                providerToConcurrentRateLimiter.get(config.provider()).apply(config),
                                rateLimiters.get(config.apiKey()),
                                objectMapper,
                                httpClient,
                                sanitizer(getModelsWithThinkingBlock().contains(config.name())))));
        res.putAll(aiProps.getDeployments()
                .stream()
                .filter(d -> d.name().contains(SPECIALIST))
                .collect(Collectors.toUnmodifiableMap(AiProperties.DeploymentProperties::name, config -> {
                    String baseName = config.name().replace(SPECIALIST, EMPTY_STRING);
                    AiClient baseClient = res.get(baseName);
                    return new AiClient(config,
                            baseClient.getMoreRestrictiveRateLimiter(),
                            baseClient.getLessRestrictiveRateLimiter(),
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
