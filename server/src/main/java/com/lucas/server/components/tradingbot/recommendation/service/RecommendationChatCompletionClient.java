package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.Constants;
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
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.lucas.server.common.Constants.PROMPT;
import static com.lucas.server.common.Constants.backOff;

@Component
public class RecommendationChatCompletionClient {

    private final Message systemMessage;
    private final Message promptMessage;
    private final Message fewShotMessage;
    private final Message fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final ObjectMapper objectMapper;
    private final AzureOpenAiChatModel client;
    private final String secondaryModel;
    private final boolean sendFixMeRequest;
    private static final Map<String, Function<String, Message>> messageFactory = Map.of(
            "user", UserMessage::new,
            "assistant", AssistantMessage::new,
            "system", SystemMessage::new
    );
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper, RecommendationChatCompletionResponseMapper mapper,
                                              ObjectMapper objectMapper, AzureOpenAiChatModel client,
                                              @Value("${spring.ai.azure.openai.chat.options.secondary-deployment-name}") String secondaryModel,
                                              @Value("${spring.ai.azure.openai.chat.options.fix-me-request}") boolean sendFixMeRequest) throws JsonProcessingException {
        this.fixMeMessage = generateMessageFromNode(promptRepository.getFixMeMessage());
        this.systemMessage = generateMessageFromNode(promptRepository.getSystem());
        this.promptMessage = generateMessageFromNode(promptRepository.getContext());
        this.fewShotMessage = generateMessageFromNode(promptRepository.getFewShot());
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.client = client;
        this.secondaryModel = secondaryModel;
        this.sendFixMeRequest = sendFixMeRequest;
    }

    public List<Recommendation> getRecommendations(Map<Symbol, List<MarketData>> marketData, Map<Symbol, List<News>> newsData,
                                                   Map<Symbol, ? extends PortfolioBase> portfolioData, Boolean withFixmeRequest) throws ClientException, IOException {
        List<AssetReportRaw> reports = new ArrayList<>();
        for (Map.Entry<Symbol, List<MarketData>> mdHistory : marketData.entrySet()) {
            reports.add(this.assertReportDataProvider.provide(mdHistory.getKey(), mdHistory.getValue(),
                    newsData.get(mdHistory.getKey()), portfolioData.get(mdHistory.getKey())));
        }

        Message reportMessage = generateMessageFromNode(this.objectMapper.readValue(this.assetReportToMustacheMapper.map(reports), ObjectNode.class));
        Message fixedMessage;
        if ((null != withFixmeRequest) ? withFixmeRequest : sendFixMeRequest) {
            logger.info(Constants.
                    GENERATING_PRE_REQUEST_INFO, marketData.keySet());
            fixedMessage = new UserMessage(this.callWithBackupStrategy(new Prompt(List.of(fixMeMessage, reportMessage), client.getDefaultOptions())));
            backOff(60000);
        } else {
            fixedMessage = reportMessage;
        }

        logger.info(Constants.GENERATING_RECOMMENDATIONS_INFO, marketData.keySet());
        Prompt prompt = new Prompt(List.of(this.systemMessage, this.promptMessage, this.fewShotMessage, fixedMessage), client.getDefaultOptions());

        return this.mapper.mapAll(marketData.keySet().stream().toList(), objectMapper.readTree(this.callWithBackupStrategy(prompt)), fixedMessage);
    }

    @Retryable(retryFor = ClientException.class, maxAttempts = Constants.REQUEST_MAX_ATTEMPTS)
    private String callWithBackupStrategy(Prompt prompt) throws ClientException {
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

    private Message generateMessageFromNode(ObjectNode data) throws JsonProcessingException {
        try {
            return messageFactory.get(data.get(Constants.ROLE).asText()).apply(data.get(Constants.CONTENT).asText());
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(Constants.JSON_MAPPING_ERROR, PROMPT), e);
        }
    }
}
