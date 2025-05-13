package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper;
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

@Component
public class RecommendationChatCompletionClient {

    private final Message systemMessage;
    private final Message promptMessage;
    private final Message fewShotMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiChatModel client;
    private static final Map<String, Function<String, Message>> messageFactory = Map.of(
            "user", UserMessage::new,
            "assistant", AssistantMessage::new,
            "system", SystemMessage::new
    );
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper,
                                              ObjectMapper objectMapper, AzureOpenAiChatModel client) throws JsonProcessingException {
        this.systemMessage = generateMessageFromNode(promptRepository.getSystem());
        this.promptMessage = generateMessageFromNode(promptRepository.getContext());
        this.fewShotMessage = generateMessageFromNode(promptRepository.getFewShot());
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public String getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData) throws ClientException, IllegalStateException, IOException {
        List<AssetReportToMustacheMapper.AssetReportRaw> reports = new ArrayList<>();
        for (Map.Entry<Symbol, List<MarketData>> mdHistory : marketData.entrySet()) {
            reports.add(this.assertReportDataProvider.provide(mdHistory.getKey(), mdHistory.getValue(), newsData.get(mdHistory.getKey())));
        }
        Prompt prompt = new Prompt(List.of(this.systemMessage, this.promptMessage, this.fewShotMessage,
                generateMessageFromNode(this.objectMapper.readValue(this.assetReportToMustacheMapper.map(reports), ObjectNode.class)))
        );
        logger.info(Constants.GENERATING_RECOMMENDATIONS_INFO, prompt.getContents());

        try {
            return client.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    private Message generateMessageFromNode(ObjectNode data) throws JsonProcessingException {
        try {
            return messageFactory.get(data.get(Constants.ROLE).asText()).apply(data.get(Constants.CONTENT).asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "prompt"), e);
        }
    }
}
