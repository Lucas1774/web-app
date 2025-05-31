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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.*;

@Service
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final NewsJpaService newsService;
    private final RecommendationsJpaService recommendationsService;
    private final FinnhubNewsClient newsClient;
    private final PortfolioManager portfolioManager;
    private final RecommendationChatCompletionClient recommendationClient;
    private final Map<MarketDataType, TypeToMarketDataFunction> typeToRunner;
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
                       RecommendationsJpaService recommendationsService, RecommendationChatCompletionClient recommendationClient,
                       PortfolioJpaService portfolioService, PortfolioMockJpaService portfolioMockService,
                       FinnhubNewsClient newsClient, PortfolioManager portfolioManager,
                       TwelveDataMarketDataClient twelveDataMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsService = recommendationsService;
        this.recommendationClient = recommendationClient;
        this.newsClient = newsClient;
        this.portfolioManager = portfolioManager;
        typeToRunner = new EnumMap<>(Map.of(
                MarketDataType.LAST, symbols -> retrieveMarketDataWithBackupStrategy(symbols, twelveDataMarketDataClient, finnhubMarketDataClient),
                MarketDataType.HISTORIC, symbols -> twelveDataMarketDataClient.retrieveMarketData(symbols, MarketDataType.HISTORIC),
                MarketDataType.REAL_TIME, finnhubMarketDataClient::retrieveMarketData
        ));
        portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    public List<Recommendation> getRecommendationsById(List<Long> symbolIds, PortfolioType type, boolean sendFixmeRequest,
                                                       boolean overwrite, List<Clients> clients) throws ClientException, IOException {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        return getRecommendations(symbols, type, sendFixmeRequest, overwrite, clients);
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    private List<Recommendation> getRecommendations(List<Symbol> symbols, PortfolioType type, boolean sendFixmeRequest,
                                                    boolean overwrite, List<Clients> clients) throws ClientException, IOException {
        Map<Symbol, List<MarketData>> marketData = symbols.stream()
                .map(symbol -> Map.entry(symbol, marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT)))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (marketData.isEmpty()) {
            return new ArrayList<>();
        }

        List<Symbol> symbolsWithData = marketData.keySet().stream().toList();

        List<Recommendation> res = new ArrayList<>();
        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, marketData.size());
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
            res.addAll(recommendationClient.getRecommendations(marketDataBatch, newsDataBatch, portfolioDataBatch,
                    sendFixmeRequest, clients));
            if (i + RECOMMENDATIONS_CHUNK_SIZE < symbolsWithData.size()) {
                backOff(CHAT_COMPLETIONS_BACKOFF_MILLIS);
            }
        }

        logger.info(GENERATION_SUCCESSFUL_INFO, RECOMMENDATION);
        if (overwrite) {
            return recommendationsService.createOrUpdate(res);
        }
        recommendationsService.createIgnoringDuplicates(res);
        return res;
    }

    @Transactional(rollbackOn = {ClientException.class, IOException.class})
    public List<Recommendation> getRandomRecommendations(PortfolioType type, int count, boolean sendFixmeRequest,
                                                         boolean overwrite) throws ClientException, IOException {
        Set<Long> active = portfolioTypeToService.get(type)
                .findActivePortfolio().stream()
                .map(p -> p.getSymbol().getId())
                .collect(Collectors.toSet());
        Set<Long> already = recommendationsService.findByDateBetween(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
                .map(r -> r.getSymbol().getId())
                .collect(Collectors.toSet());

        Set<Long> candidates = symbolService.findAll().stream()
                .filter(s -> SP500_SYMBOLS.contains(s.getName()) && !marketDataService.findBySymbolId(s.getId()).isEmpty())
                .map(Symbol::getId)
                .collect(Collectors.toSet());
        candidates.removeAll(active);
        candidates.removeAll(already);

        List<Long> finalList = new ArrayList<>(active.stream().filter(s -> !already.contains(s)).toList());
        finalList = finalList.subList(0, Math.min(finalList.size(), count));
        int needed = count - finalList.size();
        if (needed > 0 && !candidates.isEmpty()) {
            List<Long> pool = new ArrayList<>(candidates);
            Collections.shuffle(pool);
            finalList.addAll(pool.subList(0, Math.min(needed, pool.size())));
        }

        return getRecommendationsById(finalList, type, sendFixmeRequest, overwrite, RANDOM_RECOMMENDATION_CLIENTS);
    }

    @Transactional
    public List<News> generateSentiment(List<String> symbolNames, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        logger.info(GENERATION_SUCCESSFUL_INFO, SENTIMENT);
        return newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRange(List<String> symbolNames, LocalDate from,
                                              LocalDate now) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        List<News> news = newsClient.retrieveNewsByDateRange(symbols, from, now);
        logger.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(news);
        return news;
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbolNames,
                                               MarketDataType type) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        List<MarketData> mds = typeToRunner.get(type).apply(symbols);
        logger.info(GENERATION_SUCCESSFUL_INFO, MARKET_DATA);
        marketDataService.createIgnoringDuplicates(mds);
        return mds;
    }

    @Transactional(rollbackOn = IllegalStateException.class)
    public <T extends PortfolioBase> T executePortfolioAction(PortfolioType type, Long symbolId, BigDecimal price,
                                                              BigDecimal quantity, BigDecimal commission, LocalDateTime timestamp,
                                                              boolean isBuy) throws IllegalStateException {
        Symbol symbol = symbolService.findById(symbolId).orElseThrow(
                () -> new IllegalStateException(MessageFormat.format(SYMBOL_NOT_FOUND_ERROR, symbolId))
        );
        IPortfolioJpaService<T> service = getService(type);
        return service.executePortfolioAction(symbol, price, quantity, commission, timestamp, isBuy);
    }

    public List<PortfolioManager.SymbolStand> getPortfolioStand(List<String> symbolNames, PortfolioType type,
                                                                boolean dynamic) throws ClientException, JsonProcessingException {
        List<PortfolioBase> portfolio = getService(type).findActivePortfolio()
                .stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .toList();
        Stream<PortfolioManager.SymbolStand> res;
        if (dynamic) {
            Map<String, MarketData> symbolsWithData = typeToRunner.get(MarketDataType.REAL_TIME)
                    .apply(portfolio.stream().map(PortfolioBase::getSymbol).toList())
                    .stream()
                    .collect(Collectors.toMap(md -> md.getSymbol().getName(), Function.identity()));
            res = portfolio.stream()
                    .flatMap(p -> marketDataService.findBySymbolId(p.getSymbol().getId())
                            .stream()
                            .max(Comparator.comparing(MarketData::getDate))
                            .map(md -> portfolioManager.computeStand(p, symbolsWithData.get(md.getSymbol().getName())
                                    .setRecommendations(md.getRecommendations()))) // artificially link the recommendations to the volatile mds
                            .stream());
        } else {
            res = portfolio.stream()
                    .flatMap(p -> marketDataService.findBySymbolId(p.getSymbol().getId())
                            .stream()
                            .max(Comparator.comparing(MarketData::getDate))
                            .map(md -> portfolioManager.computeStand(p, md))
                            .stream());
        }

        return res.sorted(Comparator.comparing(PortfolioManager.SymbolStand::lastMoveDate).reversed())
                .toList();
    }

    public List<PortfolioManager.SymbolStand> getAllAsPortfolio(List<String> symbolNames, PortfolioType type) {
        Set<String> namesSet = new HashSet<>(symbolNames);
        List<Symbol> symbols = namesSet.stream().map(symbolService::getOrCreateByName).toList();
        Map<Symbol, PortfolioBase> portfolioBySymbol = getService(type)
                .findActivePortfolio().stream()
                .filter(p -> namesSet.contains(p.getSymbol().getName()))
                .collect(Collectors.toMap(
                        PortfolioBase::getSymbol,
                        Function.identity()
                ));
        symbols.forEach(s -> portfolioBySymbol
                .computeIfAbsent(s, symbol -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol)));

        return portfolioBySymbol.values().stream()
                .flatMap(p -> marketDataService.findBySymbolId(p.getSymbol().getId()).stream()
                        .max(Comparator.comparing(MarketData::getDate))
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream()
                )
                .sorted(Comparator.comparing(PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .toList();
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

    @Transactional
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
                res.add(twelveDataMarketDataClient.retrieveMarketData(List.of(symbol), MarketDataType.LAST).getFirst());
            } catch (ClientException | JsonProcessingException e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, twelveDataMarketDataClient.getClass().getSimpleName(), symbol, e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                backOff(FINNHUB_BACKOFF_MILLIS);
            }
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private <T extends PortfolioBase> IPortfolioJpaService<T> getService(PortfolioType type) {
        return (IPortfolioJpaService<T>) portfolioTypeToService.get(type);
    }
}
