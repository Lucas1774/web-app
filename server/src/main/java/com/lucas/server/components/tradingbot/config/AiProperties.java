package com.lucas.server.components.tradingbot.config;

import com.lucas.server.common.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.lucas.server.common.Constants.AiProvider.GITHUB;
import static com.lucas.server.common.Constants.AiProvider.GOOGLE;
import static com.lucas.server.common.Constants.AiProvider.OPENROUTER;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
@Slf4j
public class AiProperties {

    private Set<DeploymentProperties> deployments;

    public record DeploymentProperties(String name,
                                       String apiKey,
                                       String url,
                                       String model,
                                       String thinkingLevel,
                                       Double temperature,
                                       Integer maxTokens,
                                       Integer requestsPerMinute,
                                       Integer concurrentRequests,
                                       Integer chunkSize,
                                       Boolean fixMe,
                                       Constants.AiProvider provider,
                                       List<String> fallbackModels) {

        @SuppressWarnings("LoggingSimilarMessage")
        public DeploymentProperties {
            if (null == provider) {
                provider = GITHUB;
            }
            if (GITHUB.equals(provider)) {
                if (null == temperature) {
                    temperature = 0.0;
                }
                url = "https://models.github.ai/inference/chat/completions";
            }
            if (OPENROUTER.equals(provider)) {
                if (null == temperature) {
                    temperature = 0.0;
                }
                if (null != requestsPerMinute) {
                    log.warn("Overriding requestsPerMinute to 20");
                }
                requestsPerMinute = 20;
                if (null != concurrentRequests) {
                    log.warn("Overriding concurrentRequests to match requestsPerMinute");
                }
                concurrentRequests = requestsPerMinute;
                url = "https://openrouter.ai/api/v1/chat/completions";
            }
            if (GOOGLE.equals(provider)) {
                if (null == thinkingLevel) {
                    thinkingLevel = "high";
                }
                if (null != concurrentRequests) {
                    log.warn("Overriding concurrentRequests to match requestsPerMinute");
                }
                concurrentRequests = requestsPerMinute;
                url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key="
                      + apiKey;
            }
            if (null == fixMe) {
                fixMe = false;
            }
            if (null == chunkSize) {
                chunkSize = 5;
            }
            if (null == maxTokens) {
                maxTokens = 8000;
            }

            Objects.requireNonNull(requestsPerMinute);
            Objects.requireNonNull(concurrentRequests);
        }
    }
}
