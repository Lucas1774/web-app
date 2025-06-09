package com.lucas.server.components.tradingbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    public record DeploymentProperties(
            String name,
            String apiKey,
            String url,
            String model,
            double temperature,
            int maxTokens,
            int requestsPerMinute,
            int chunkSize,
            boolean fixMe
    ) {
    }

    private List<DeploymentProperties> deployments;
}
