package com.lucas.server.components.tradingbot.config;

import org.junit.jupiter.api.Test;

import static com.lucas.server.common.Constants.AiProvider.GOOGLE;
import static com.lucas.server.common.Constants.AiProvider.OPENROUTER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiPropertiesTest {

    @Test
    void defaultsGoogleConcurrentRequestsToOneWithoutChangingRequestsPerMinute() {
        AiProperties.DeploymentProperties config = new AiProperties.DeploymentProperties("google",
                "key",
                null,
                "gemini-model",
                null,
                null,
                null,
                5,
                null,
                null,
                null,
                GOOGLE,
                null);

        assertEquals(5, config.requestsPerMinute());
        assertEquals(1, config.concurrentRequests());
    }

    @Test
    void preservesExplicitGoogleConcurrentRequests() {
        AiProperties.DeploymentProperties config = new AiProperties.DeploymentProperties("google",
                "key",
                null,
                "gemini-model",
                null,
                null,
                null,
                5,
                2,
                null,
                null,
                GOOGLE,
                null);

        assertEquals(2, config.concurrentRequests());
    }

    @Test
    void fixesOpenRouterPacingAtOneRequestPerSecond() {
        AiProperties.DeploymentProperties config = new AiProperties.DeploymentProperties("openrouter",
                "key",
                null,
                "openrouter-model",
                null,
                null,
                null,
                5,
                5,
                null,
                null,
                OPENROUTER,
                null);

        assertEquals(20, config.requestsPerMinute());
        assertEquals(1, config.concurrentRequests());
    }
}
