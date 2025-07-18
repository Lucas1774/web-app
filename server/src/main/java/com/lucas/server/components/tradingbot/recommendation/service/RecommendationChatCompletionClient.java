package com.lucas.server.components.tradingbot.recommendation.service;

import com.azure.ai.inference.models.ChatRequestAssistantMessage;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationChatCompletionResponseMapper;
import com.lucas.server.components.tradingbot.recommendation.prompt.PromptRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationChatCompletionClient {

    private final ChatRequestMessage systemMessage;
    private final ObjectNode context;
    private final ChatRequestMessage fewShotMessage;
    private final ChatRequestMessage fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final ObjectMapper objectMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final Map<String, RateLimiter> rateLimiters;
    private static final Map<String, Function<String, ChatRequestMessage>> messageFactory = Map.of(
            "user", ChatRequestUserMessage::new,
            "assistant", ChatRequestAssistantMessage::new,
            "system", ChatRequestSystemMessage::new
    );
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper, ObjectMapper objectMapper,
                                              RecommendationChatCompletionResponseMapper mapper, Map<String, RateLimiter> rateLimiters)
            throws JsonProcessingException {
        fixMeMessage = generateMessageFromNode(promptRepository.getFixMeMessage());
        systemMessage = generateMessageFromNode(promptRepository.getSystem());
        context = promptRepository.getContext();
        fewShotMessage = generateMessageFromNode(promptRepository.getFewShot());
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.rateLimiters = rateLimiters;
    }

    public List<Recommendation> getRecommendations(List<DataManager.SymbolPayload> payload, AIClient client) throws IOException {
        List<AssetReportRaw> reports = payload.stream()
                .map(assertReportDataProvider::provide)
                .toList();
        ChatRequestUserMessage rawReportMessage = (ChatRequestUserMessage) generateMessageFromNode(objectMapper.readValue(
                assetReportToMustacheMapper.map(reports), ObjectNode.class
        ));
        ChatRequestUserMessage reportMessage;
        if (!client.isFixMe()) {
            reportMessage = rawReportMessage;
        } else {
            reportMessage = new ChatRequestUserMessage(((ChatRequestUserMessage) fixMeMessage).getContent().toObject(String.class)
                    .replace("{placeholder}", rawReportMessage.getContent().toObject(String.class)));
        }
        List<Symbol> symbols = payload.stream().map(DataManager.SymbolPayload::symbol).toList();
        ChatRequestMessage contextMessage = generateMessageFromNode(context.deepCopy().put("content", context.get("content").asText().replace("{date}",
                ZonedDateTime.now(NY_ZONE).format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH)))));

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, symbols);
        try {
            client.getRateLimiter().acquirePermission();
            rateLimiters.get(AI_PER_MINUTE_RATE_LIMITER).acquirePermission();
            rateLimiters.get(AI_PER_SECOND_RATE_LIMITER).acquirePermission();
            logger.info(PROMPTING_MODEL_INFO, client.getModelName());
            List<ChatRequestMessage> prompt = List.of(systemMessage, contextMessage, fewShotMessage, reportMessage);
            return mapper.mapAll(payload,
                    objectMapper.readTree(client.complete(prompt)
                            .getChoice()
                            .getMessage()
                            .getContent()
                            .replace("```", "")
                            .replace("json", "")),
                    prompt.stream().map(this::getAsString).collect(Collectors.joining("\n\n\n")), client.getModelName());
        } catch (Exception e) {
            logger.warn(CLIENT_FAILED_BACKUP_WARN, client.getModelName(), PROMPT, e.getMessage());
            return new ArrayList<>();
        }
    }

    private ChatRequestMessage generateMessageFromNode(ObjectNode data) throws JsonProcessingException {
        try {
            return messageFactory.get(data.get(ROLE).asText()).apply(data.get(CONTENT).asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, PROMPT), e);
        }
    }

    private String getAsString(ChatRequestMessage message) {
        return switch (message) {
            case ChatRequestUserMessage m -> m.getContent().toObject(String.class);
            case ChatRequestAssistantMessage m -> m.getContent();
            case ChatRequestSystemMessage m -> m.getContent();
            default -> throw new IllegalArgumentException();
        };
    }
}
