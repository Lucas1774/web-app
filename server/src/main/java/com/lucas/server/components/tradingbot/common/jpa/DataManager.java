package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.AlphavantageMarketDataClient;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@Service
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final RecommendationChatCompletionClient recommendationsClient;
    private final FinnhubNewsClient newsClient;
    private final Map<Granularity, GranularityToMarketDataFunction> granularityToClientRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    private static final Map<PortfolioType, Supplier<? extends PortfolioBase>> portfolioTypeToNewPortfolio = Map.of(
            PortfolioType.REAL, Portfolio::new,
            PortfolioType.MOCK, PortfolioMock::new
    );

    @FunctionalInterface
    private interface GranularityToMarketDataFunction {
        List<MarketData> apply(List<Symbol> symbols) throws ClientException, JsonProcessingException;
    }

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, NewsJpaService newsService,
                       PortfolioJpaService portfolioService, PortfolioMockJpaService portfolioMockService,
                       RecommendationChatCompletionClient recommendationsClient, FinnhubNewsClient newsClient,
                       TwelveDataMarketDataClient twelveDataMarketDataClient, AlphavantageMarketDataClient alphavantageMarketDataClient,
                       FinnhubMarketDataClient finnhubMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsClient = recommendationsClient;
        this.newsClient = newsClient;
        this.granularityToClientRunner = new EnumMap<>(Map.of(
                Granularity.DAILY, symbols -> retrieveMarketDataWithBackupStrategy(symbols, twelveDataMarketDataClient, finnhubMarketDataClient),
                Granularity.WEEKLY, symbols ->
                        alphavantageMarketDataClient.retrieveMarketData(symbols, Granularity.WEEKLY)
        ));
        this.portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    public String getRecommendations(List<String> symbolNames, PortfolioType type) throws ClientException, IOException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        Map<Symbol, List<MarketData>> marketData = symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> this.marketDataService.getTopForSymbolId(symbol.getId(), Constants.HISTORY_DAYS_COUNT)
                ));
        Map<Symbol, List<News>> newsData = symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> this.newsService.getTopForSymbolId(symbol.getId(), Constants.NEWS_COUNT)
                ));

        Map<Symbol, ? extends PortfolioBase> portfolioData = symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> this.getService(type).findBySymbol(symbol)
                                .orElseGet(() -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol))
                ));

        return this.recommendationsClient.getRecommendations(marketData, newsData, portfolioData);
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
    public List<MarketData> retrieveMarketData(List<String> symbolNames, Granularity granularity) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(this.symbolService::getOrCreateByName).toList();
        List<MarketData> mds = granularityToClientRunner.get(granularity).apply(symbols);
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
                res.add(twelveDataMarketDataClient.retrieveMarketData(symbol));
                Constants.backOff(7500);
            } catch (ClientException | JsonProcessingException e) {
                logger.warn(Constants.MAIN_CLIENT_FAILED_BACKUP_WARN,
                        twelveDataMarketDataClient.getClass().getSimpleName(), symbol,
                        finnhubMarketDataClient.getClass().getSimpleName(), e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                Constants.backOff(1000);
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private <T extends PortfolioBase> IPortfolioJpaService<T> getService(PortfolioType type) {
        return (IPortfolioJpaService<T>) this.portfolioTypeToService.get(type);
    }
}
