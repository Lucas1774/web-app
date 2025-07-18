package com.lucas.server.components.tradingbot.config;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.policy.RetryPolicy;
import com.lucas.server.components.tradingbot.common.AIClient;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class AIClientAutoconfig {

    @Bean
    public Map<String, AIClient> clients(HttpClient httpClient, RetryPolicy retryPolicy, AIProperties aiProps) {
        return aiProps.getDeployments().stream()
                .collect(Collectors.toMap(
                        AIProperties.DeploymentProperties::name,
                        config -> {
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
                                    .limitRefreshPeriod(Duration.ofMinutes(1).dividedBy(config.requestsPerMinute()))
                                    .limitForPeriod(1)
                                    .timeoutDuration(Duration.ofMinutes(1))
                                    .build());
                            return new AIClient(client, optionsProvider, config.name(), config.chunkSize(), config.fixMe(), rateLimiter);
                        }
                ));
    }
}
