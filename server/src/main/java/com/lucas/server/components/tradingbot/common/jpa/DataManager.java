package com.lucas.server.components.tradingbot.common.jpa;

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
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsJpaService;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationClient;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationLlmClient;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
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
    private final RecommendationsJpaService recommendationsService;
    private final FinnhubNewsClient newsClient;
    private final PortfolioManager portfolioManager;
    private final Map<TwelveDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;
    private final Map<RecommendationEngineType, RecommendationClient> recommendationsClientTypeToClient;
    private final Map<RecommendationEngineType, Integer> recommendationsClientTypeToBackoff;
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
                       RecommendationsJpaService recommendationsService, PortfolioJpaService portfolioService,
                       PortfolioMockJpaService portfolioMockService, RecommendationChatCompletionClient azureRecommendationClient,
                       RecommendationLlmClient llmRecommendationClient, FinnhubNewsClient newsClient, PortfolioManager portfolioManager,
                       TwelveDataMarketDataClient twelveDataMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsService = recommendationsService;
        this.newsClient = newsClient;
        this.portfolioManager = portfolioManager;
        typeToRunner = new EnumMap<>(Map.of(
                TwelveDataType.LAST, symbols -> retrieveMarketDataWithBackupStrategy(symbols, twelveDataMarketDataClient, finnhubMarketDataClient),
                TwelveDataType.HISTORIC, symbols -> twelveDataMarketDataClient.retrieveMarketData(symbols, TwelveDataType.HISTORIC)
        ));
        portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
        recommendationsClientTypeToClient = new EnumMap<>(Map.of(
                RecommendationEngineType.RAW, llmRecommendationClient,
                RecommendationEngineType.AZURE, azureRecommendationClient
        ));
        recommendationsClientTypeToBackoff = new EnumMap<>(Map.of(
                RecommendationEngineType.RAW, 12000,
                RecommendationEngineType.AZURE, 60000
        ));
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    public List<Recommendation> getRecommendations(List<String> symbolNames, PortfolioType type, Boolean sendFixmeRequest,
                                                   RecommendationEngineType engineType) throws ClientException, IOException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        Map<Symbol, List<MarketData>> marketData = symbols.stream()
                .map(symbol -> Map.entry(symbol, marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT)))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (marketData.isEmpty()) {
            return new ArrayList<>();
        }

        List<Symbol> symbolsWithData = marketData.keySet().stream().toList();

        List<Recommendation> res = new ArrayList<>();
        logger.info(GENERATING_RECOMMENDATIONS_INFO, marketData.size());
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
            res.addAll(recommendationsClientTypeToClient.get(engineType)
                    .getRecommendations(marketDataBatch, newsDataBatch, portfolioDataBatch, sendFixmeRequest));
            if (i + RECOMMENDATIONS_CHUNK_SIZE < symbolsWithData.size()) {
                backOff(recommendationsClientTypeToBackoff.get(engineType));
            }
        }

        recommendationsService.createIgnoringDuplicates(res);
        return res;
    }

    public List<Recommendation> getRandomRecommendations(PortfolioType type, int count, Boolean sendFixmeRequest,
                                                         RecommendationEngineType engineType) throws ClientException, IOException {
        Set<String> active = portfolioTypeToService.get(type)
                .findActivePortfolio().stream()
                .map(p -> p.getSymbol().getName())
                .collect(Collectors.toSet());
        Set<String> already = recommendationsService.findByDateBetween(LocalDate.now().minusDays(1), LocalDate.now()).stream()
                .map(r -> r.getSymbol().getName())
                .collect(Collectors.toSet());

        Set<String> candidates = new HashSet<>(SP500_SYMBOLS);
        candidates.removeAll(active);
        candidates.removeAll(already);

        List<String> finalList = new ArrayList<>(active);
        finalList.removeAll(already);
        finalList = finalList.subList(0, Math.min(finalList.size(), count));
        int needed = count - finalList.size();
        if (needed > 0 && !candidates.isEmpty()) {
            List<String> pool = new ArrayList<>(candidates);
            Collections.shuffle(pool);
            finalList.addAll(pool.subList(0, Math.min(needed, pool.size())));
        }

        return getRecommendations(finalList, type, sendFixmeRequest, engineType);
    }

    @Transactional
    public List<News> generateSentiment(List<String> symbolNames, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        return newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRange(List<String> symbolNames, LocalDate from, LocalDate now) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        List<News> news = newsClient.retrieveNewsByDateRange(symbols, from, now);
        newsService.createOrUpdate(news, MAX_SYMBOLS_TO_TRIGGER_NEWS_EMBEDDINGS_GENERATION >= symbols.size());
        return news;
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbolNames, TwelveDataType type) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        List<MarketData> mds = typeToRunner.get(type).apply(symbols);
        marketDataService.createIgnoringDuplicates(mds);
        return mds;
    }

    @Transactional(rollbackOn = IllegalStateException.class)
    public <T extends PortfolioBase> T executePortfolioAction(
            PortfolioType type, String symbolName, BigDecimal price,
            BigDecimal quantity, LocalDateTime timestamp, boolean isBuy
    ) throws IllegalStateException {
        Symbol symbol = symbolService.findByName(symbolName).orElseThrow(
                () -> new IllegalStateException(MessageFormat.format(SYMBOL_NOT_FOUND_ERROR, symbolName))
        );
        IPortfolioJpaService<T> service = getService(type);
        return service.executePortfolioAction(symbol, price, quantity, timestamp, isBuy);
    }

    public List<PortfolioManager.SymbolStand> getPortfolioStand(PortfolioType type) {
        return getService(type).findLatest()
                .stream()
                .flatMap(p -> marketDataService.getTopForSymbolId(p.getSymbol().getId(), 1)
                        .stream()
                        .findFirst()
                        .map(top -> portfolioManager.computeStand(p, top))
                        .stream()
                ).toList();
    }

    @Transactional
    public List<News> removeOldNews(int keepCount) {
        List<News> res = new ArrayList<>();
        List<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            List<Long> keepIds = newsService.getTopForSymbolId(symbol.getId(), keepCount).stream().map(News::getId).toList();
            List<News> toRemove = symbol.getNews().stream()
                    .filter(news -> !keepIds.contains(news.getId()))
                    .toList();
            res.addAll(toRemove);
            toRemove.forEach(n -> {
                symbol.getNews().remove(n);
                n.getSymbols().remove(symbol);
            });
        }
        newsService.removeOrphanedNews();
        return res;
    }

    @Transactional
    public List<MarketData> removeOldMarketData(int keepCount) {
        List<MarketData> res = new ArrayList<>();
        List<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            List<MarketData> toKeep = marketDataService.getTopForSymbolId(symbol.getId(), keepCount);
            List<MarketData> toRemove = marketDataService.findBySymbolId(symbol.getId()).stream()
                    .filter(marketData -> !toKeep.contains(marketData))
                    .toList();
            res.addAll(toRemove);
        }
        marketDataService.deleteAll(res);
        return res;
    }

    public List<Recommendation> removeOldRecommendations(int keepCount) {
        List<Recommendation> res = new ArrayList<>();
        List<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            List<Recommendation> toKeep = recommendationsService.getTopForSymbolId(symbol.getId(), keepCount);
            List<Recommendation> toRemove = recommendationsService.findBySymbolId(symbol.getId()).stream()
                    .filter(recommendation -> !toKeep.contains(recommendation))
                    .toList();
            res.addAll(toRemove);
        }
        recommendationsService.deleteAll(res);
        return res;
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
        return (IPortfolioJpaService<T>) portfolioTypeToService.get(type);
    }
}
