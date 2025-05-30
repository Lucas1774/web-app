package com.lucas.server.components.tradingbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;


@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    public record DeploymentProperties(
            String apiKey,
            String url,
            String model,
            double temperature,
            int maxTokens
    ) {
    }

    private Map<String, DeploymentProperties> deployments = new LinkedHashMap<>();
}
