package com.lucas.server.components.tradingbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private Set<DeploymentProperties> deployments;

    public record DeploymentProperties(String name,
                                       String apiKey,
                                       String url,
                                       String model,
                                       double temperature,
                                       int maxTokens,
                                       int requestsPerMinute,
                                       int concurrentRequests,
                                       int chunkSize,
                                       boolean fixMe,
                                       List<String> fallbackModels) {
    }
}
