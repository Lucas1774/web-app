package com.lucas.server.components.tradingbot.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.config.SlidingWindowRateLimiter;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@Component
public class RecommendationChatCompletionClient {

    private final Object yahooApiLock = new Object();
    private final JsonNode systemMessage;
    private final ObjectNode context;
    private final JsonNode fewShotMessage;
    private final JsonNode fixMeMessage;
    private final AssetReportDataProvider assertReportDataProvider;
    private final AssetReportToMustacheMapper assetReportToMustacheMapper;
    private final ObjectMapper objectMapper;
    private final RecommendationChatCompletionResponseMapper mapper;
    private final Map<String, RateLimiter> rateLimiters;
    private static final Logger logger = LoggerFactory.getLogger(RecommendationChatCompletionClient.class);

    public RecommendationChatCompletionClient(PromptRepository promptRepository, AssetReportDataProvider assertReportDataProvider,
                                              AssetReportToMustacheMapper assetReportToMustacheMapper, ObjectMapper objectMapper,
                                              RecommendationChatCompletionResponseMapper mapper, Map<String, RateLimiter> rateLimiters) {
        fixMeMessage = promptRepository.getFixMeMessage();
        systemMessage = promptRepository.getSystem();
        context = promptRepository.getContext();
        fewShotMessage = promptRepository.getFewShot();
        this.assertReportDataProvider = assertReportDataProvider;
        this.assetReportToMustacheMapper = assetReportToMustacheMapper;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.rateLimiters = rateLimiters;
    }

    @FunctionalInterface
    public interface NewsFetcher {
        List<News> apply(List<Symbol> symbols) throws ClientException, JsonProcessingException;
    }

    @FunctionalInterface
    public interface MarketDataFetcher {
        MarketData apply(Symbol symbol) throws ClientException, JsonProcessingException;
    }

    public List<Recommendation> getRecommendations(List<DataManager.SymbolPayload> payload, AIClient client,
                                                   boolean onTheFlyNews, boolean fetchPreMarket, NewsFetcher newsFetcher,
                                                   BiFunction<Long, Integer, List<News>> backupNewsFetcher,
                                                   MarketDataFetcher marketDataFetcher) throws IOException {
        synchronized (yahooApiLock) {
            if (onTheFlyNews) {
                List<Symbol> symbols = payload.stream()
                        .map(DataManager.SymbolPayload::getSymbol)
                        .toList();
                List<News> news;
                try {
                    news = newsFetcher.apply(symbols);
                } catch (ClientException | JsonProcessingException e) {
                    logger.warn(RETRIEVAL_FAILED_WARN, NEWS, symbols, e);
                    news = payload.stream()
                            .flatMap(p -> backupNewsFetcher.apply(p.getSymbol().getId(), NEWS_COUNT).stream())
                            .distinct()
                            .toList();
                }
                List<News> finalNews = news;
                payload.forEach(p -> p.setNews(finalNews.stream()
                        .filter(n -> n.getSymbols().contains(p.getSymbol()))
                        .sorted(Comparator.comparing(News::getDate).reversed())
                        .limit(NEWS_COUNT)
                        .toList()));
            }

            if (fetchPreMarket) {
                payload.forEach(p -> {
                    try {
                        p.setPremarket(marketDataFetcher.apply(p.getSymbol()));
                    } catch (ClientException | JsonProcessingException e) {
                        logger.warn(RETRIEVAL_FAILED_WARN, PREMARKET, p.getSymbol(), e);
                    }
                });
            }
        }

        List<AssetReportRaw> reports = payload.stream()
                .map(assertReportDataProvider::provide)
                .toList();
        JsonNode rawReportMessage = objectMapper.readValue(assetReportToMustacheMapper.map(reports), ObjectNode.class);
        JsonNode reportMessage;
        if (!client.getConfig().fixMe()) {
            reportMessage = rawReportMessage;
        } else {
            reportMessage = objectMapper.readValue(fixMeMessage.get(CONTENT).asText()
                    .replace("{placeholder}", rawReportMessage.get(CONTENT).asText()), ObjectNode.class);
        }
        List<Symbol> symbols = payload.stream().map(DataManager.SymbolPayload::getSymbol).toList();
        JsonNode contextMessage = context.deepCopy().put(CONTENT, context.get(CONTENT).asText().replace("{date}",
                ZonedDateTime.now(NY_ZONE).format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH))));

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, symbols);
        try {
            client.getRateLimiter().acquirePermission();
            rateLimiters.get(AI_PER_MINUTE_RATE_LIMITER).acquirePermission();
            rateLimiters.get(AI_PER_SECOND_RATE_LIMITER).acquirePermission();
            logger.info(PROMPTING_MODEL_INFO, client.getConfig().name());
            List<JsonNode> prompt = List.of(systemMessage, contextMessage, fewShotMessage, reportMessage);
            return mapper.mapAll(payload,
                    objectMapper.readTree(client.complete(prompt)),
                    prompt.stream().map(p -> p.get(CONTENT).asText()).collect(Collectors.joining("\n\n\n")), client.getConfig().name());
        } catch (Exception e) {
            logger.warn(CLIENT_FAILED_BACKUP_WARN, client.getConfig().name(), PROMPT, e);
            return new ArrayList<>();
        }
    }
}
