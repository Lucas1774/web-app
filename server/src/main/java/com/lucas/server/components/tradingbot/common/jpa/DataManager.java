package com.lucas.server.components.tradingbot.common.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshotJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.YahooFinanceMarketSnapshotClient;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.news.service.YahooFinanceNewsClient;
import com.lucas.server.components.tradingbot.portfolio.jpa.*;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsJpaService;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import com.lucas.utils.Interrupts;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import com.lucas.utils.orderedindexedset.UnmodifiableOrderedIndexedSet;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;

@SuppressWarnings("LoggingSimilarMessage")
@Service
public class DataManager {

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    private static final Map<PortfolioType, Supplier<PortfolioBase>> portfolioTypeToNewPortfolio = Map.of(
            PortfolioType.REAL, Portfolio::new,
            PortfolioType.MOCK, PortfolioMock::new
    );
    private final Object newsPersistLock = new Object();
    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final MarketSnapshotJpaService marketSnapshotService;
    private final NewsJpaService newsService;
    private final RecommendationsJpaService recommendationsService;
    private final YahooFinanceNewsClient yahooFinanceNewsClient;
    private final YahooFinanceMarketSnapshotClient yahooFinanceMarketSnapshotClient;
    private final FinnhubNewsClient finnhubNewsClient;
    private final FinnhubMarketDataClient finnhubMarketDataClient;
    private final TwelveDataMarketDataClient twelveDataMarketDataClient;
    private final PortfolioManager portfolioManager;
    private final RecommendationChatCompletionClient recommendationClient;
    private final Map<MarketDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, MarketSnapshotJpaService marketSnapshotService,
                       NewsJpaService newsService, RecommendationsJpaService recommendationsService,
                       YahooFinanceNewsClient yahooFinanceNewsClient, YahooFinanceMarketSnapshotClient yahooFinanceMarketSnapshotClient,
                       FinnhubMarketDataClient finnhubMarketDataClient, TwelveDataMarketDataClient twelveDataMarketDataClient,
                       RecommendationChatCompletionClient recommendationClient, PortfolioJpaService portfolioService,
                       PortfolioMockJpaService portfolioMockService, FinnhubNewsClient newsClient, PortfolioManager portfolioManager) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.marketSnapshotService = marketSnapshotService;
        this.newsService = newsService;
        this.recommendationsService = recommendationsService;
        this.yahooFinanceNewsClient = yahooFinanceNewsClient;
        this.yahooFinanceMarketSnapshotClient = yahooFinanceMarketSnapshotClient;
        this.finnhubMarketDataClient = finnhubMarketDataClient;
        this.twelveDataMarketDataClient = twelveDataMarketDataClient;
        this.recommendationClient = recommendationClient;
        finnhubNewsClient = newsClient;
        this.portfolioManager = portfolioManager;
        typeToRunner = Map.of(
                MarketDataType.LAST, this::retrieveMarketDataWithBackupStrategy,
                MarketDataType.HISTORIC, this::retrieveTwelveDataMarketData,
                MarketDataType.REAL_TIME, symbols -> {
                    OrderedIndexedSet<MarketData> res = new OrderedIndexedSetImpl<>();
                    for (Symbol symbol : symbols) {
                        res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                    }
                    return new UnmodifiableOrderedIndexedSet<>(res);
                }
        );
        portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
    }

    public OrderedIndexedSet<Long> getTopRecommendedSymbols(String action, BigDecimal confidenceThreshold, LocalDate recommendationDate) {
        return recommendationsService.getTopRecommendedSymbols(action, confidenceThreshold, recommendationDate);
    }

    public Set<Recommendation> getDailyRecommendations(BigDecimal confidenceThreshold, LocalDate date, String action, Set<String> clients) {
        return recommendationsService.getDailyRecommendations(confidenceThreshold, date, action, clients);
    }

    @Transactional
    public Set<Recommendation> getRecommendationsById(Set<Long> symbolIds, Set<AIClient> clients, PortfolioType type,
                                                      boolean overwrite, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        Set<Symbol> symbols = symbolService.findAllById(symbolIds);
        return getRecommendationsInParallel(symbols, clients, type, overwrite, false, onTheFlyNews, fetchPreMarket, useOldNews);
    }

    @Transactional
    public Set<Recommendation> getRandomRecommendations(Set<String> symbolNames, Set<AIClient> clients, PortfolioType type, int count,
                                                        boolean overwrite, boolean onlyIfHasNews, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        Set<Long> already = recommendationsService.findByDateBetween(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
                .map(r -> r.getSymbol().getId())
                .collect(Collectors.toSet());

        Set<Long> candidates = symbolService.findAll().stream()
                .filter(s -> symbolNames.contains(s.getName()))
                .map(Symbol::getId)
                .collect(Collectors.toSet());
        candidates.removeAll(already);

        Set<Long> finalList = new HashSet<>();
        if (0 < count && !candidates.isEmpty()) {
            List<Long> pool = new ArrayList<>(candidates);
            Collections.shuffle(pool);
            finalList.addAll(pool.subList(0, Math.min(count, pool.size())));
        }

        return getRecommendationsInParallel(symbolService.findAllById(finalList), clients, type, overwrite,
                onlyIfHasNews, onTheFlyNews, fetchPreMarket, useOldNews);
    }

    private Set<Recommendation> getRecommendationsInParallel(Set<Symbol> symbols, Set<AIClient> clients, PortfolioType type, boolean overwrite,
                                                             boolean onlyIfHasNews, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        Set<Recommendation> res = new HashSet<>();
        Deque<AIClient> mutableClients = new ConcurrentLinkedDeque<>(clients);
        IPortfolioJpaService<PortfolioBase> portfolioService = getService(type);
        Supplier<PortfolioBase> portfolioSupplier = portfolioTypeToNewPortfolio.get(type);

        LocalDateTime startUtc;
        if (useOldNews) {
            startUtc = null;
        } else {
            ZonedDateTime easternTime = ZonedDateTime.now(NY_ZONE);
            LocalDate lastTradeFinishedDate = easternTime.toLocalDate();
            if (easternTime.toLocalTime().isBefore(MARKET_CLOSE)) {
                lastTradeFinishedDate = lastTradeFinishedDate.minusDays(1);
            }
            while (!isTradingDate(lastTradeFinishedDate)) {
                lastTradeFinishedDate = lastTradeFinishedDate.minusDays(1);
            }
            LocalTime close = !EARLY_CLOSE_DATES_2025.contains(lastTradeFinishedDate) ? MARKET_CLOSE : EARLY_CLOSE;
            startUtc = ZonedDateTime.of(lastTradeFinishedDate, close, NY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }

        Set<Symbol> remainingSymbols = new HashSet<>(symbols);
        try (ExecutorService executor = Executors.newFixedThreadPool(clients.size())) {
            int attempts = 0;
            while (!remainingSymbols.isEmpty() && RECOMMENDATION_MAX_ATTEMPTS > attempts) {
                BlockingQueue<Set<Recommendation>> resultsQueue = new LinkedBlockingQueue<>();
                int submitted = 0;
                Set<SymbolPayload> recommendationBuffer = new HashSet<>();
                logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, remainingSymbols.size());
                for (Symbol symbol : new HashSet<>(remainingSymbols)) {
                    OrderedIndexedSet<MarketData> marketData = marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT);
                    if (marketData.isEmpty()) {
                        remainingSymbols.remove(symbol);
                        continue;
                    }
                    OrderedIndexedSet<News> news = provideNews(onTheFlyNews, useOldNews, symbol, startUtc);
                    if (onlyIfHasNews && news.isEmpty()) {
                        remainingSymbols.remove(symbol);
                        continue;
                    }
                    PortfolioBase portfolio = portfolioService.findBySymbol(symbol)
                            .orElseGet(() -> portfolioSupplier.get().setSymbol(symbol));

                    SymbolPayload payload = new SymbolPayload(symbol, marketData, portfolio).setNews(news);
                    providePremarket(fetchPreMarket, symbol, payload);
                    recommendationBuffer.add(payload);
                    AIClient client = mutableClients.peekFirst();
                    if (recommendationBuffer.size() == Objects.requireNonNull(client).getConfig().chunkSize()) {
                        submitChunk(new HashSet<>(recommendationBuffer), client, executor, resultsQueue, overwrite, useOldNews);
                        submitted++;
                        mutableClients.add(mutableClients.pollFirst());
                        recommendationBuffer.clear();
                    }
                }
                if (!recommendationBuffer.isEmpty()) {
                    AIClient client = mutableClients.peekFirst();
                    submitChunk(new HashSet<>(recommendationBuffer), client, executor, resultsQueue, overwrite, useOldNews);
                    submitted++;
                    mutableClients.add(mutableClients.pollFirst());
                }

                for (int i = 0; i < submitted; i++) {
                    Set<Recommendation> fetched = Interrupts.callOrSwallow(resultsQueue::take,
                            Collections::emptySet,
                            e -> logger.error(e.getMessage(), e));
                    if (!Objects.requireNonNull(fetched).isEmpty()) {
                        res.addAll(fetched);
                        Set<Symbol> fetchedSymbols = fetched.stream()
                                .map(Recommendation::getSymbol)
                                .collect(Collectors.toSet());
                        remainingSymbols.removeIf(fetchedSymbols::contains);
                    }
                }
                attempts++;
            }
        }

        if (!remainingSymbols.isEmpty()) {
            logger.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, remainingSymbols);
        }
        return res;
    }

    private void providePremarket(boolean fetchPreMarket, Symbol symbol, SymbolPayload payload) {
        if (fetchPreMarket) {
            try {
                payload.setPremarket(yahooFinanceMarketSnapshotClient.retrieveMarketSnapshot(symbol));
            } catch (ClientException | MappingException e) {
                logger.warn(RETRIEVAL_FAILED_WARN, MARKET_SNAPSHOT, symbol, e);
            }
        }
    }

    private OrderedIndexedSet<News> provideNews(boolean onTheFlyNews, boolean useOldNews, Symbol symbol, LocalDateTime startUtc) {
        if (!onTheFlyNews) {
            if (!useOldNews) {
                return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT).stream()
                        .filter(n -> n.getDate().isAfter(startUtc)).collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
            } else {
                return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT);
            }
        } else {
            if (!useOldNews) {
                try {
                    return retrieveYahooNews(Set.of(symbol)).stream()
                            .filter(n -> n.getDate().isAfter(startUtc))
                            .sorted(Comparator.comparing(News::getDate).reversed())
                            .limit(NEWS_COUNT)
                            .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
                } catch (ClientException | MappingException e) {
                    logger.warn(RETRIEVAL_FAILED_WARN, NEWS, symbol, e);
                    return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT).stream()
                            .filter(n -> n.getDate().isAfter(startUtc)).collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
                }
            } else {
                try {
                    return retrieveYahooNews(Set.of(symbol)).stream()
                            .sorted(Comparator.comparing(News::getDate).reversed())
                            .limit(NEWS_COUNT)
                            .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
                } catch (ClientException | MappingException e) {
                    logger.warn(RETRIEVAL_FAILED_WARN, NEWS, symbol, e);
                    return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT);
                }
            }
        }
    }

    private void submitChunk(Set<SymbolPayload> buffer, AIClient client, ExecutorService executor,
                             BlockingQueue<Set<Recommendation>> resultsQueue, boolean overwrite, boolean useOldNews) {
        executor.submit(() -> {
            try {
                Set<Recommendation> partial = recommendationClient.getRecommendations(buffer, client, useOldNews);
                logger.info(GENERATION_SUCCESSFUL_INFO, RECOMMENDATION);
                Set<Recommendation> finalPartial;
                synchronized (newsPersistLock) {
                    Map<Long, News> persistedNews = newsService.createOrUpdate(partial.stream()
                                    .flatMap(r -> r.getNews().stream())
                                    .collect(Collectors.toSet())).stream()
                            .collect(Collectors.toMap(News::getExternalId, Function.identity()));
                    partial.forEach(r -> r.setNews(r.getNews().stream()
                            .map(n -> persistedNews.get(n.getExternalId()))
                            .collect(Collectors.toSet())));
                }
                if (overwrite) {
                    finalPartial = recommendationsService.createOrUpdate(partial);
                } else {
                    finalPartial = partial;
                    recommendationsService.createIgnoringDuplicates(partial);
                }
                Interrupts.runOrThrow(() -> resultsQueue.put(finalPartial), e -> logger.error(e.getMessage(), e));
            } catch (JsonProcessingException | ClientException | MappingException e) {
                logger.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, buffer.stream().map(SymbolPayload::getSymbol).toList(), e);
                Interrupts.runOrThrow(() -> resultsQueue.put(Collections.emptySet()), ie -> logger.error(ie.getMessage(), ie));
            }
        });
    }

    @Transactional
    public Set<News> generateSentiment(Set<String> symbolNames, LocalDateTime from, LocalDateTime to) {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        logger.info(GENERATION_SUCCESSFUL_INFO, SENTIMENT);
        return newsService.generateSentiment(symbols.stream().map(Symbol::getId).collect(Collectors.toSet()), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<News> retrieveNewsByName(Set<String> symbolNames) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveNews(symbols);
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<News> retrieveNewsById(Set<Long> symbolIds) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.findAllById(symbolIds);
        return retrieveNews(symbols);
    }

    private Set<News> retrieveNews(Set<Symbol> symbols) throws ClientException, MappingException {
        Set<News> news = retrieveYahooNews(symbols);
        logger.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(news);
        return news;
    }

    private Set<News> retrieveYahooNews(Set<Symbol> symbols) throws ClientException, MappingException {
        Map<Long, News> newsByExternalId = new HashMap<>();
        for (Symbol symbol : symbols) {
            Set<News> updated = yahooFinanceNewsClient.retrieveNews(symbol);
            for (News news : updated) {
                newsByExternalId
                        .computeIfAbsent(news.getExternalId(), id -> news)
                        .addSymbol(symbol);
            }
        }

        return new HashSet<>(newsByExternalId.values());
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<News> retrieveNewsByDateRangeAndId(Set<Long> symbolIds, LocalDate from,
                                                  LocalDate to) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.findAllById(symbolIds);
        return retrieveNewsByDateRange(symbols, from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<News> retrieveNewsByDateRangeAndName(Set<String> symbolNames, LocalDate from,
                                                    LocalDate to) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveNewsByDateRange(symbols, from, to);
    }

    private Set<News> retrieveNewsByDateRange(Set<Symbol> symbols, LocalDate from,
                                              LocalDate to) throws ClientException, MappingException {
        Map<Long, News> newsByExternalId = new HashMap<>();
        for (Symbol symbol : symbols) {
            Set<News> updated = finnhubNewsClient.retrieveNewsByDateRange(symbol, from, to);
            for (News news : updated) {
                newsByExternalId
                        .computeIfAbsent(news.getExternalId(), id -> news)
                        .addSymbol(symbol);
            }
        }
        Set<News> res = new HashSet<>(newsByExternalId.values());
        logger.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(res);
        return res;
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<MarketData> retrieveMarketData(Set<String> symbolNames,
                                              MarketDataType type) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        OrderedIndexedSet<MarketData> mds = typeToRunner.get(type).apply(symbols);
        logger.info(GENERATION_SUCCESSFUL_INFO, MARKET_DATA);
        marketDataService.createIgnoringDuplicates(mds);
        return mds;
    }

    @Transactional(rollbackOn = {ClientException.class, MappingException.class})
    public Set<MarketSnapshot> retrieveSnapshotsByName(Set<String> symbolNames) {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveSnapshots(symbols);
    }

    private Set<MarketSnapshot> retrieveSnapshots(Set<Symbol> symbols) {
        Set<MarketSnapshot> mss = new HashSet<>();
        for (Symbol symbol : symbols) {
            try {
                mss.add(yahooFinanceMarketSnapshotClient.retrieveMarketSnapshot(symbol));
            } catch (ClientException | MappingException e) {
                logger.warn(RETRIEVAL_FAILED_WARN, MARKET_SNAPSHOT, symbol, e);
            }
        }
        logger.info(GENERATION_SUCCESSFUL_INFO, MARKET_SNAPSHOT);
        return marketSnapshotService.createAll(mss);
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

    public Set<PortfolioManager.SymbolStand> getPortfolioStand(Set<String> symbolNames, PortfolioType type) {
        Set<PortfolioBase> portfolio = getService(type).findActivePortfolio()
                .stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .collect(Collectors.toSet());
        return getStand(portfolio);
    }

    public Set<PortfolioManager.SymbolStand> getAllAsPortfolioStandById(Set<Long> symbolIds, PortfolioType type,
                                                                        boolean dynamic) throws ClientException, MappingException {
        Set<Symbol> symbols = symbolService.findAllById(symbolIds);
        Map<Symbol, PortfolioBase> portfolioBySymbol = getService(type)
                .findActivePortfolio().stream()
                .filter(p -> symbols.contains(p.getSymbol()))
                .collect(Collectors.toMap(
                        PortfolioBase::getSymbol,
                        Function.identity()
                ));
        symbols.forEach(s -> portfolioBySymbol
                .computeIfAbsent(s, symbol -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol)));
        return dynamic ? getStandDynamically(new HashSet<>(portfolioBySymbol.values()))
                : getStand(new HashSet<>(portfolioBySymbol.values()));
    }

    public Set<PortfolioManager.SymbolStand> getAllAsPortfolioStand(Set<String> symbolNames, PortfolioType type) {
        Set<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        Map<Symbol, PortfolioBase> portfolioBySymbol = getService(type)
                .findActivePortfolio().stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .collect(Collectors.toMap(
                        PortfolioBase::getSymbol,
                        Function.identity()
                ));
        symbols.forEach(s -> portfolioBySymbol
                .computeIfAbsent(s, symbol -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol)));

        return getStand(new HashSet<>(portfolioBySymbol.values()));
    }

    private OrderedIndexedSet<PortfolioManager.SymbolStand> getStand(Set<PortfolioBase> portfolio) {
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream())
                .sorted(Comparator.comparing(
                        PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    private OrderedIndexedSet<PortfolioManager.SymbolStand> getStandDynamically(Set<PortfolioBase> portfolio) throws ClientException, MappingException {
        Map<String, MarketData> symbolsWithData = typeToRunner.get(MarketDataType.REAL_TIME)
                .apply(portfolio.stream().map(PortfolioBase::getSymbol).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(md -> md.getSymbol().getName(), Function.identity()));
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p, symbolsWithData.get(md.getSymbol().getName())
                                .setRecommendations(md.getRecommendations()))) // artificially link the recommendations to the volatile mds
                        .stream())
                .sorted(Comparator.comparing(
                        PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    @Transactional
    public Set<News> removeOldNews(int keepCount) {
        Set<News> res = new HashSet<>();
        Set<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            Set<Long> keepIds = newsService.getTopForSymbolId(symbol.getId(), keepCount).stream().map(News::getId)
                    .collect(Collectors.toSet());
            Set<News> toRemove = symbol.getNews().stream().filter(news -> !keepIds.contains(news.getId()))
                    .collect(Collectors.toSet());
            res.addAll(toRemove);
            toRemove.forEach(n -> {
                symbol.getNews().remove(n);
                n.getSymbols().remove(symbol);
            });
        }

        Set<News> orphanedNews = newsService.findOrphanedNews();
        Set<Long> orphanedIds = orphanedNews.stream()
                .map(News::getId)
                .collect(Collectors.toSet());
        recommendationsService.findByNewsId(orphanedIds).forEach(
                r -> r.getNews().removeIf(n -> orphanedIds.contains(n.getId()))
        );
        newsService.deleteAll(orphanedNews);

        return res;
    }

    @Transactional
    public Set<MarketData> removeOldMarketData(int keepCount) {
        Set<MarketData> res = new HashSet<>();
        Set<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            Set<MarketData> toKeep = marketDataService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<MarketData> toRemove = marketDataService.findBySymbolId(symbol.getId()).stream()
                    .filter(marketData -> !toKeep.contains(marketData))
                    .collect(Collectors.toSet());
            res.addAll(toRemove);
        }
        marketDataService.deleteAll(res);
        return res;
    }

    @Transactional
    public Set<MarketSnapshot> removeOldSnapshots(int keepCount) {
        Set<MarketSnapshot> res = new HashSet<>();
        Set<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            Set<MarketSnapshot> toKeep = marketSnapshotService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<MarketSnapshot> toRemove = marketSnapshotService.findBySymbolId(symbol.getId()).stream()
                    .filter(marketSnapshot -> !toKeep.contains(marketSnapshot))
                    .collect(Collectors.toSet());
            res.addAll(toRemove);
        }
        marketSnapshotService.deleteAll(res);
        return res;
    }

    @Transactional
    public Set<Recommendation> removeOldRecommendations(int keepCount) {
        Set<Recommendation> res = new HashSet<>();
        Set<Symbol> symbols = symbolService.findAll();
        for (Symbol symbol : symbols) {
            Set<Recommendation> toKeep = recommendationsService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<Recommendation> toRemove = recommendationsService.findBySymbolId(symbol.getId()).stream()
                    .filter(recommendation -> !toKeep.contains(recommendation))
                    .collect(Collectors.toSet());
            res.addAll(toRemove);
        }
        recommendationsService.deleteAll(res);
        return res;
    }

    private OrderedIndexedSet<MarketData> retrieveMarketDataWithBackupStrategy(Set<Symbol> symbols) throws ClientException, MappingException {
        OrderedIndexedSet<MarketData> res = new OrderedIndexedSetImpl<>();
        for (Symbol symbol : symbols) {
            try {
                res.add(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.LAST).getFirst());
            } catch (ClientException | MappingException e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, twelveDataMarketDataClient.getClass().getSimpleName(), symbol, e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
            }
        }
        return new UnmodifiableOrderedIndexedSet<>(res);
    }

    private OrderedIndexedSet<MarketData> retrieveTwelveDataMarketData(Set<Symbol> symbols) throws ClientException, MappingException {
        OrderedIndexedSet<MarketData> res = new OrderedIndexedSetImpl<>();
        for (Symbol symbol : symbols) {
            res.addAll(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.HISTORIC));
        }
        return new UnmodifiableOrderedIndexedSet<>(res);
    }

    @SuppressWarnings("unchecked")
    private <T extends PortfolioBase> IPortfolioJpaService<T> getService(PortfolioType type) {
        return (IPortfolioJpaService<T>) portfolioTypeToService.get(type);
    }

    @FunctionalInterface
    private interface TypeToMarketDataFunction {
        OrderedIndexedSet<MarketData> apply(Set<Symbol> symbols) throws ClientException, MappingException;
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(chain = true)
    public static class SymbolPayload {
        private final Symbol symbol;
        private final OrderedIndexedSet<MarketData> marketData;
        private final PortfolioBase portfolio;
        @Setter
        private MarketSnapshot premarket;
        @Setter
        private OrderedIndexedSet<News> news;
    }
}
