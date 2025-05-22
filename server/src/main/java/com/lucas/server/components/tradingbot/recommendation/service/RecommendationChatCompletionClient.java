package com.lucas.server.components.tradingbot.recommendation.service;

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
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationChatCompletionClient implements RecommendationClient {

    private final Message systemMessage;
    private final Message promptMessage;
    private final Message fewShotMessage;
    private final Message fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiChatModel client;
    private final RetryableRecommendationsClientComponent retryableClient;
    private static final Map<String, Function<String, Message>> messageFactory = Map.of(
            "user", UserMessage::new,
            "assistant", AssistantMessage::new,
            "system", SystemMessage::new
    );
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper, RecommendationChatCompletionResponseMapper mapper,
                                              ObjectMapper objectMapper, AzureOpenAiChatModel client, RetryableRecommendationsClientComponent retryableClient)
            throws JsonProcessingException {
        fixMeMessage = generateMessageFromNode(promptRepository.getFixMeMessage());
        systemMessage = generateMessageFromNode(promptRepository.getSystem());
        promptMessage = generateMessageFromNode(promptRepository.getContext());
        fewShotMessage = generateMessageFromNode(promptRepository.getFewShot());
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.client = client;
        this.retryableClient = retryableClient;
    }

    public List<Recommendation> getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData,
                                                   Map<Symbol, ? extends PortfolioBase> portfolioData, Boolean withFixmeRequest) throws ClientException, IOException {
        List<AssetReportRaw> reports = new ArrayList<>();
        for (Map.Entry<Symbol, List<MarketData>> mdHistory : marketData.entrySet()) {
            reports.add(assertReportDataProvider.provide(mdHistory.getKey(), mdHistory.getValue(),
                    newsData.get(mdHistory.getKey()), portfolioData.get(mdHistory.getKey())));
        }

        Message reportMessage = generateMessageFromNode(objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class));
        Message fixedMessage;
        if (Boolean.TRUE.equals(withFixmeRequest)) {
            logger.info(GENERATING_PRE_REQUEST_INFO, marketData.keySet());
            fixedMessage = new UserMessage(retryableClient.callWithBackupStrategy(new Prompt(List.of(fixMeMessage, reportMessage), client.getDefaultOptions())));
            backOff(CHAT_COMPLETIONS_BACKOFF_MILLIS);
        } else {
            fixedMessage = reportMessage;
        }

        logger.info(GENERATING_RECOMMENDATIONS_INFO, marketData.keySet());
        Prompt prompt = new Prompt(List.of(systemMessage, promptMessage, fewShotMessage, fixedMessage), client.getDefaultOptions());

        return mapper.mapAll(marketData.keySet().stream().toList(), objectMapper.readTree(retryableClient.callWithBackupStrategy(prompt)), fixedMessage.getText());
    }

    private Message generateMessageFromNode(ObjectNode data) throws JsonProcessingException {
        try {
            return messageFactory.get(data.get(ROLE).asText()).apply(data.get(CONTENT).asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(JSON_MAPPING_ERROR, PROMPT), e);
        }
    }
}
