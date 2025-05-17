package com.lucas.server.components.tradingbot.common.jpa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.portfolio.jpa.*;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@Service
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final ObjectMapper mapper;
    private final RecommendationChatCompletionClient recommendationsClient;
    private final FinnhubNewsClient newsClient;
    private final Map<TwelveDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    private static final Map<PortfolioType, Supplier<? extends PortfolioBase>> portfolioTypeToNewPortfolio = Map.of(
            PortfolioType.REAL, Portfolio::new,
            PortfolioType.MOCK, PortfolioMock::new
    );

    @FunctionalInterface
    private interface TypeToMarketDataFunction {
        List<MarketData> apply(List<Symbol> symbols) throws ClientException, JsonProcessingException;
    }

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, NewsJpaService newsService,
                       PortfolioJpaService portfolioService, PortfolioMockJpaService portfolioMockService, ObjectMapper mapper,
                       RecommendationChatCompletionClient recommendationsClient, FinnhubNewsClient newsClient,
                       TwelveDataMarketDataClient twelveDataMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.mapper = mapper;
        this.recommendationsClient = recommendationsClient;
        this.newsClient = newsClient;
        this.typeToRunner = new EnumMap<>(Map.of(
                TwelveDataType.LAST, symbols -> retrieveMarketDataWithBackupStrategy(symbols, twelveDataMarketDataClient, finnhubMarketDataClient),
                TwelveDataType.HISTORIC, symbols -> twelveDataMarketDataClient.retrieveMarketData(symbols, TwelveDataType.HISTORIC)
        ));
        this.portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    public JsonNode getRecommendations(List<String> symbolNames, PortfolioType type) throws ClientException, IOException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        Map<Symbol, List<MarketData>> marketData = symbols.stream()
                .map(symbol -> Map.entry(symbol, marketDataService.getTopForSymbolId(symbol.getId(), HISTORY_DAYS_COUNT)))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (marketData.isEmpty()) {
            return mapper.createArrayNode();
        }

        List<Symbol> symbolsWithData = marketData.keySet().stream().toList();

        ArrayNode res = mapper.createArrayNode();
        for (int i = 0; i < symbolsWithData.size(); i += RECOMMENDATIONS_CHUNK_SIZE) {
            List<Symbol> batch = symbolsWithData.subList(i, Math.min(i + RECOMMENDATIONS_CHUNK_SIZE, symbolsWithData.size()));
            Map<Symbol, List<MarketData>> marketDataBatch = new LinkedHashMap<>();
            Map<Symbol, List<News>> newsDataBatch = new LinkedHashMap<>();
            Map<Symbol, PortfolioBase> portfolioDataBatch = new LinkedHashMap<>();

            for (Symbol symbol : batch) {
                marketDataBatch.put(symbol, marketData.get(symbol));
                newsDataBatch.put(symbol, newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT));
                portfolioDataBatch.put(symbol,
                        getService(type).findBySymbol(symbol)
                                .orElseGet(() -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol))
                );
            }
            long startMillis = System.currentTimeMillis();
            res.add(recommendationsClient.getRecommendations(marketDataBatch, newsDataBatch, portfolioDataBatch));
            if (i + RECOMMENDATIONS_CHUNK_SIZE < symbolsWithData.size()) {
                backOff(Math.max(0, 60000 - (System.currentTimeMillis() - startMillis)));
            }
        }

        return res;
    }

    @Transactional
    public List<News> generateSentiment(List<String> symbolNames, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        return this.newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRange(List<String> symbolNames, LocalDate from, LocalDate now) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        List<News> news = this.newsClient.retrieveNewsByDateRange(symbols, from, now);
        this.newsService.createOrUpdate(news, MAX_SYMBOLS_TO_TRIGGER_NEWS_EMBEDDINGS_GENERATION >= symbols.size());
        return news;
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbolNames, TwelveDataType type) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        List<MarketData> mds = typeToRunner.get(type).apply(symbols);
        this.marketDataService.createIgnoringDuplicates(mds);
        return mds;
    }

    @Transactional(rollbackOn = IllegalStateException.class)
    public <T extends PortfolioBase> T executePortfolioAction(
            PortfolioType type, String symbolName, BigDecimal price,
            BigDecimal quantity, LocalDateTime timestamp, boolean isBuy
    ) throws IllegalStateException {
        Symbol symbol = symbolService.getOrCreateByName(symbolName);
        IPortfolioJpaService<T> service = this.getService(type);
        return service.executePortfolioAction(symbol, price, quantity, timestamp, isBuy);
    }

    private List<MarketData> retrieveMarketDataWithBackupStrategy(
            List<Symbol> symbols, TwelveDataMarketDataClient twelveDataMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient
    ) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            try {
                res.add(twelveDataMarketDataClient.retrieveMarketData(List.of(symbol), TwelveDataType.LAST).getFirst());
            } catch (ClientException | JsonProcessingException e) {
                logger.warn(MAIN_CLIENT_FAILED_BACKUP_WARN,
                        twelveDataMarketDataClient.getClass().getSimpleName(), symbol,
                        finnhubMarketDataClient.getClass().getSimpleName(), e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                backOff(1000);
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private <T extends PortfolioBase> IPortfolioJpaService<T> getService(PortfolioType type) {
        return (IPortfolioJpaService<T>) this.portfolioTypeToService.get(type);
    }
}
