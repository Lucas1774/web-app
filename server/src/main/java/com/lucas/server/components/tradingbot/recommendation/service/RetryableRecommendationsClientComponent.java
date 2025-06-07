package com.lucas.server.components.tradingbot.recommendation.service;

import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationChatCompletionResponseMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@Component
public class RetryableRecommendationsClientComponent {

    private final Map<String, AIClient> chatClients;
    private final ObjectMapper objectMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final Map<String, RateLimiter> rateLimiters;
    private static final Logger logger = LoggerFactory.getLogger(RetryableRecommendationsClientComponent.class);

    @FunctionalInterface
    private interface ClientAction<T> {
        T apply(AIClient client) throws ClientException, IOException;
    }

    public RetryableRecommendationsClientComponent(Map<String, AIClient> chatClients, ObjectMapper objectMapper,
                                                   RecommendationChatCompletionResponseMapper mapper, Map<String, RateLimiter> rateLimiters) {
        this.chatClients = chatClients;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.rateLimiters = rateLimiters;
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = REQUEST_MAX_ATTEMPTS, backoff = @Backoff(delay = CHAT_COMPLETIONS_BACKOFF_MILLIS))
    String callWithBackupStrategy(List<ChatRequestMessage> prompt, List<Clients> clients) throws ClientException {
        return tryAllClients(clients, aiClient -> aiClient.complete(prompt)
                .getChoice()
                .getMessage()
                .getContent()
                .replace("```", "")
                .replace("json", ""));
    }

    @Retryable(retryFor = {ClientException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS, backoff = @Backoff(delay = CHAT_COMPLETIONS_BACKOFF_MILLIS))
    List<Recommendation> callWithBackupStrategyAndMap(Map<Symbol, List<MarketData>> marketData, List<ChatRequestMessage> prompt, List<Clients> clients,
                                                      ChatRequestUserMessage fixedMessage) throws ClientException {
        return tryAllClients(clients, aiClient -> mapper.mapAll(marketData,
                objectMapper.readTree(aiClient.complete(prompt)
                        .getChoice()
                        .getMessage()
                        .getContent()
                        .replace("```", "")
                        .replace("json", "")),
                fixedMessage.getContent().toString(), aiClient.getModelName()));
    }

    private <T> T tryAllClients(List<Clients> clients, ClientAction<T> action) throws ClientException {
        for (int i = 0; i < clients.size() - 1; i++) {
            AIClient current = chatClients.get(clients.get(i).toString());
            try {
                current.getRateLimiter().acquirePermission();
                rateLimiters.get(PER_MINUTE_RATE_LIMITER).acquirePermission();
                rateLimiters.get(PER_SECOND_RATE_LIMITER).acquirePermission();
                logger.info(PROMPTING_MODEL_INFO, current.getModelName());
                return action.apply(current);
            } catch (Exception e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, current.getModelName(), PROMPT, e.getMessage());
            }
        }

        AIClient last = chatClients.get(clients.getLast().toString());
        try {
            last.getRateLimiter().acquirePermission();
            rateLimiters.get(PER_MINUTE_RATE_LIMITER).acquirePermission();
            rateLimiters.get(PER_SECOND_RATE_LIMITER).acquirePermission();
            logger.info(PROMPTING_MODEL_INFO, last.getModelName());
            return action.apply(last);
        } catch (Exception e) {
            logger.warn(CLIENT_FAILED_BACKUP_WARN, last.getModelName(), PROMPT, e.getMessage());
            throw new ClientException(e);
        }
    }
}
