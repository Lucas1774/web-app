package com.lucas.server.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    OpenAIClient openAIClient(OpenAIClientBuilder builder) {
        return builder.buildClient();
    }
}
