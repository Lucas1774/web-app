package com.lucas.server.components.tradingbot.recommendation.service;

import com.azure.ai.inference.models.ChatRequestAssistantMessage;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioBase;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.RecommendationChatCompletionResponseMapper;
import com.lucas.server.components.tradingbot.recommendation.prompt.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationChatCompletionClient {

    private final ChatRequestMessage systemMessage;
    private final ChatRequestMessage promptMessage;
    private final ChatRequestMessage fewShotMessage;
    private final ChatRequestMessage fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final ObjectMapper objectMapper;
    private final RetryableRecommendationsClientComponent retryableClient;
    private static final Map<String, Function<String, ChatRequestMessage>> messageFactory = Map.of(
            "user", ChatRequestUserMessage::new,
            "assistant", ChatRequestAssistantMessage::new,
            "system", ChatRequestSystemMessage::new
    );
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper, RecommendationChatCompletionResponseMapper mapper,
                                              ObjectMapper objectMapper, RetryableRecommendationsClientComponent retryableClient)
            throws JsonProcessingException {
        fixMeMessage = generateMessageFromNode(promptRepository.getFixMeMessage());
        systemMessage = generateMessageFromNode(promptRepository.getSystem());
        promptMessage = generateMessageFromNode(promptRepository.getContext());
        fewShotMessage = generateMessageFromNode(promptRepository.getFewShot());
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.retryableClient = retryableClient;
    }

    public List<Recommendation> getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData,
                                                   Map<Symbol, ? extends PortfolioBase> portfolioData, boolean withFixmeRequest, List<Clients> clients) throws ClientException, IOException {
        List<AssetReportRaw> reports = new ArrayList<>();
        for (Map.Entry<Symbol, List<MarketData>> mdHistory : marketData.entrySet()) {
            reports.add(assertReportDataProvider.provide(mdHistory.getKey(), mdHistory.getValue(),
                    newsData.get(mdHistory.getKey()), portfolioData.get(mdHistory.getKey())));
        }

        ChatRequestMessage reportMessage = generateMessageFromNode(objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class));
        ChatRequestMessage fixedMessage;
        if (Boolean.TRUE.equals(withFixmeRequest)) {
            logger.info(RETRIEVING_DATA_INFO, PRE_REQUEST, marketData.keySet());
            fixedMessage = new ChatRequestUserMessage(retryableClient.callWithBackupStrategy(List.of(fixMeMessage, reportMessage), clients));
            backOff(CHAT_COMPLETIONS_BACKOFF_MILLIS);
        } else {
            fixedMessage = reportMessage;
        }

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, marketData.keySet());

        return mapper.mapAll(marketData, objectMapper.readTree(retryableClient
                        .callWithBackupStrategy(List.of(systemMessage, promptMessage, fewShotMessage, fixedMessage), clients)
                        .replace("```", "").replace("json", "")),
                ((ChatRequestUserMessage) fixedMessage).getContent().toString());
    }

    private ChatRequestMessage generateMessageFromNode(ObjectNode data) throws JsonProcessingException {
        try {
            return messageFactory.get(data.get(ROLE).asText()).apply(data.get(CONTENT).asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, PROMPT), e);
        }
    }
}
