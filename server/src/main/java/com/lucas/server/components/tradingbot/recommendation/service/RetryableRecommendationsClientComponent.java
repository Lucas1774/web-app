package com.lucas.server.components.tradingbot.recommendation.service;

import com.azure.ai.inference.models.ChatRequestMessage;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.AIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class RetryableRecommendationsClientComponent {

    private final Map<String, AIClient> chatClients;
    private static final Logger logger = LoggerFactory.getLogger(RetryableRecommendationsClientComponent.class);

    public RetryableRecommendationsClientComponent(Map<String, AIClient> chatClients) {
        this.chatClients = chatClients;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS, backoff = @Backoff(delay = CHAT_COMPLETIONS_BACKOFF_MILLIS))
    String callWithBackupStrategy(List<ChatRequestMessage> prompt, List<Clients> clients) throws ClientException {
        for (int i = 0; i < clients.size() - 1; i++) {
            AIClient current = chatClients.get(clients.get(i).toString());
            logger.info(PROMPTING_MODEL_INFO, current.getModelName());
            try {
                return current.complete(prompt)
                        .getChoice()
                        .getMessage()
                        .getContent();
            } catch (Exception e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, current.getModelName(), PROMPT, e.getMessage());
            }
        }

        AIClient last = chatClients.get(clients.getLast().toString());
        logger.info(PROMPTING_MODEL_INFO, last.getModelName());
        try {
            return last.complete(prompt)
                    .getChoice()
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }
}
