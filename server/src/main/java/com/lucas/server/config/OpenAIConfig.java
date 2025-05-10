package com.lucas.server.config;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatProperties;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BIG hacks to inject a non-bugged retry policy into the openAIClients
 */
@Configuration
public class OpenAIConfig {

    @Bean
    public RetryPolicy retryPolicy() {
        return new RetryPolicy(new RetryOptions(new ExponentialBackoffOptions().setMaxRetries(0)));
    }

    @Bean
    public AzureOpenAiEmbeddingModel azureOpenAiEmbeddingModel(RetryPolicy retryPolicy, OpenAIClientBuilder openAIClientBuilder,
                                                               AzureOpenAiEmbeddingProperties embeddingProperties, ObjectProvider<ObservationRegistry> observationRegistry,
                                                               ObjectProvider<EmbeddingModelObservationConvention> observationConvention) {
        var embeddingModel = new AzureOpenAiEmbeddingModel(openAIClientBuilder.retryPolicy(retryPolicy).buildClient(),
                embeddingProperties.getMetadataMode(), embeddingProperties.getOptions(),
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));

        observationConvention.ifAvailable(embeddingModel::setObservationConvention);

        return embeddingModel;
    }

    @Bean
    @ConditionalOnMissingBean
    public AzureOpenAiChatModel azureOpenAiChatModel(RetryPolicy retryPolicy, OpenAIClientBuilder openAIClientBuilder,
                                                     AzureOpenAiChatProperties chatProperties, ToolCallingManager toolCallingManager,
                                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                                     ObjectProvider<ChatModelObservationConvention> observationConvention,
                                                     ObjectProvider<ToolExecutionEligibilityPredicate> azureOpenAiToolExecutionEligibilityPredicate) {
        var chatModel = AzureOpenAiChatModel.builder()
                .openAIClientBuilder(openAIClientBuilder.retryPolicy(retryPolicy))
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(azureOpenAiToolExecutionEligibilityPredicate
                        .getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }
}
