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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                            return new AIClient(client, optionsProvider, config.model());
                        },
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}