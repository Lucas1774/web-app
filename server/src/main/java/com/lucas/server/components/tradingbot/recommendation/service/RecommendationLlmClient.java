package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
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
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationLlmClient implements RecommendationClient {
    private final ObjectNode fixMeMessage;
    private final ObjectNode systemMessage;
    private final ObjectNode promptMessage;
    private final ObjectNode fewShotMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiChatModel client;
    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private static final Logger logger = LoggerFactory.getLogger(RecommendationLlmClient.class);

    public RecommendationLlmClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                   AssetReportToMustacheMapper assetReportToMustacheMapper, RecommendationChatCompletionResponseMapper mapper,
                                   ObjectMapper objectMapper, AzureOpenAiChatModel client, HttpRequestClient httpRequestClient,
                                   @Value("${language.endpoint}") String endpoint, @Value("${language.api-key}") String apiKey,
                                   @Value("${spring.ai.azure.openai.chat.options.llm-deployment-name}") String model) {
        fixMeMessage = promptRepository.getFixMeMessage();
        systemMessage = promptRepository.getSystem();
        promptMessage = promptRepository.getContext();
        fewShotMessage = promptRepository.getFewShot();
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.client = client;
        this.httpRequestClient = httpRequestClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<Recommendation> getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData,
                                                   Map<Symbol, ? extends PortfolioBase> portfolioData, boolean withFixmeRequest)
            throws ClientException, IOException {
        List<AssetReportRaw> reports = new ArrayList<>();
        for (Map.Entry<Symbol, List<MarketData>> mdHistory : marketData.entrySet()) {
            reports.add(assertReportDataProvider.provide(mdHistory.getKey(), mdHistory.getValue(),
                    newsData.get(mdHistory.getKey()), portfolioData.get(mdHistory.getKey())));
        }

        ObjectNode reportMessage = objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class);
        ObjectNode fixedMessage;
        if (Boolean.TRUE.equals(withFixmeRequest)) {
            logger.info(GENERATING_PRE_REQUEST_INFO, marketData.keySet());
            fixedMessage = ((ObjectNode) httpRequestClient.fetch(endpoint, apiKey, buildRequestPayload(List.of(fixMeMessage, reportMessage)))
                    .get("choices").get(0).get("message")).put("role", "user");
            fixedMessage.remove("tool_calls");
            fixedMessage.remove("refusal");
            backOff(LLM_BACKOFF_MILLIS);
        } else {
            fixedMessage = reportMessage;
        }

        logger.info(GENERATING_RECOMMENDATIONS_INFO, marketData.keySet());
        ObjectNode prompt = buildRequestPayload(List.of(systemMessage, promptMessage, fewShotMessage, fixedMessage));

        return mapper.mapAll(marketData, objectMapper.readTree(httpRequestClient.fetch(endpoint, apiKey, prompt)
                .get("choices").get(0).get("message").get("content").asText()), fixedMessage.get("content").asText());
    }

    private ObjectNode buildRequestPayload(List<ObjectNode> messageNodes) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode messagesArray = objectMapper.createArrayNode();
        for (ObjectNode msgNode : messageNodes) {
            messagesArray.add(msgNode);
        }
        root.set("messages", messagesArray);
        AzureOpenAiChatOptions options = client.getDefaultOptions();
        root.put("max_completion_tokens", Optional.ofNullable(options.getMaxTokens()).orElse(8000));
        root.put("temperature", Optional.ofNullable(options.getTemperature()).orElse(0.2));
        root.put("top_p", Optional.ofNullable(options.getTopP()).orElse(1.0));
        root.put("frequency_penalty", Optional.ofNullable(options.getFrequencyPenalty()).orElse(0.0));
        root.put("presence_penalty", Optional.ofNullable(options.getPresencePenalty()).orElse(0.0));
        root.put("model", model);

        return root;
    }
}
