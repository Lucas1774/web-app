package com.lucas.server.config;

import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAIClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy(new RetryOptions(new ExponentialBackoffOptions().setMaxRetries(0)));
    }

    @Bean
    public AzureOpenAIClientBuilderCustomizer customizer(RetryPolicy retryPolicy) {
        return builder -> builder.httpClient(new NettyAsyncHttpClientBuilder()
                .responseTimeout(Duration.ofMinutes(10))
                .build()).retryPolicy(retryPolicy);
    }
}
