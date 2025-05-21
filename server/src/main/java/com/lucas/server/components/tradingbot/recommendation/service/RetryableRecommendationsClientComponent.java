package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.lucas.server.common.Constants.PROMPT;

@Component
public class RetryableRecommendationsClientComponent {

    private final AzureOpenAiChatModel client;
    private final String secondaryModel;
    private static final Logger logger = LoggerFactory.getLogger(RetryableRecommendationsClientComponent.class);

    public RetryableRecommendationsClientComponent(AzureOpenAiChatModel client,
                                                   @Value("${spring.ai.azure.openai.chat.options.secondary-deployment-name}") String secondaryModel) {
        this.client = client;
        this.secondaryModel = secondaryModel;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = Constants.REQUEST_MAX_ATTEMPTS, backoff = @Backoff(delay = 60000))
    String callWithBackupStrategy(Prompt prompt) throws ClientException {
        try {
            return client.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            logger.warn(Constants.MAIN_CLIENT_FAILED_BACKUP_WARN, client.getDefaultOptions().getDeploymentName(), PROMPT, secondaryModel, e);
            ((AzureOpenAiChatOptions) Objects.requireNonNull(prompt.getOptions())).setDeploymentName(secondaryModel);
            try {
                return client.call(prompt).getResult().getOutput().getText();
            } catch (Exception ex) {
                throw new ClientException(ex);
            }
        }
    }
}
