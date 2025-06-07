package com.lucas.server.components.tradingbot.config;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.lucas.server.components.tradingbot.common.AIClient;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.PER_MINUTE_RATE_LIMITER;
import static com.lucas.server.common.Constants.PER_SECOND_RATE_LIMITER;

@Configuration
public class AIClientAutoconfig {

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy(new RetryOptions(new ExponentialBackoffOptions().setMaxRetries(0)));
    }

    @Bean
    public HttpClient httpClient() {
        return new OkHttpAsyncHttpClientBuilder()
                .responseTimeout(Duration.ofMinutes(10))
                .build();
    }

    @Bean
    public Map<String, AIClient> clients(HttpClient httpClient, RetryPolicy retryPolicy, AIProperties aiProps) {
        return aiProps.getDeployments().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            AIProperties.DeploymentProperties config = entry.getValue();
                            ChatCompletionsClient client = new ChatCompletionsClientBuilder()
                                    .httpClient(httpClient)
                                    .retryPolicy(retryPolicy)
                                    .endpoint(config.url())
                                    .credential(new AzureKeyCredential(config.apiKey()))
                                    .buildClient();
                            Function<List<ChatRequestMessage>, ChatCompletionsOptions> optionsProvider =
                                    prompt -> new ChatCompletionsOptions(prompt)
                                            .setModel(config.model())
                                            .setTemperature(config.temperature())
                                            .setMaxTokens(config.maxTokens());
                            RateLimiter rateLimiter = RateLimiter.of(config.model(), RateLimiterConfig.custom()
                                    .limitRefreshPeriod(Duration.ofMinutes(1))
                                    .limitForPeriod(config.requestsPerMinute())
                                    .timeoutDuration(Duration.ofMinutes(1))
                                    .build());
                            return new AIClient(client, optionsProvider, config.model(), rateLimiter);
                        },
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Bean
    public Map<String, RateLimiter> rateLimiter() {
        Map<String, RateLimiter> res = new HashMap<>();
        res.put(PER_MINUTE_RATE_LIMITER,
                RateLimiter.of(PER_MINUTE_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .limitForPeriod(24)
                        .timeoutDuration(Duration.ofMinutes(5))
                        .build()));
        res.put(PER_SECOND_RATE_LIMITER,
                RateLimiter.of(PER_SECOND_RATE_LIMITER, RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(2))
                        .limitForPeriod(1)
                        .timeoutDuration(Duration.ofMinutes(1))
                        .build()));
        return res;
    }
}
