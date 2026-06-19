package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshotJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.YahooFinanceMarketSnapshotClient;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.jpa.NewsPersistenceOrchestrator;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.news.service.YahooFinanceNewsClient;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioJpaService;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioMockJpaService;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioService;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsJpaService;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
import com.lucas.utils.Interrupts;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.CLIENT_FAILED_BACKUP_WARN;
import static com.lucas.server.common.Constants.EARLY_CLOSE;
import static com.lucas.server.common.Constants.EARLY_CLOSE_DATES_2026;
import static com.lucas.server.common.Constants.GENERATION_SUCCESSFUL_INFO;
import static com.lucas.server.common.Constants.MARKET_CLOSE;
import static com.lucas.server.common.Constants.MARKET_DATA;
import static com.lucas.server.common.Constants.MARKET_DATA_RELEVANT_DAYS_COUNT;
import static com.lucas.server.common.Constants.MARKET_SNAPSHOT;
import static com.lucas.server.common.Constants.MarketDataType;
import static com.lucas.server.common.Constants.NEWS;
import static com.lucas.server.common.Constants.NEWS_COUNT;
import static com.lucas.server.common.Constants.NY_ZONE;
import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RECOMMENDATION;
import static com.lucas.server.common.Constants.RECOMMENDATION_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVAL_FAILED_WARN;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.SENTIMENT;
import static com.lucas.server.common.Constants.SYMBOL_NOT_FOUND_ERROR;
import static com.lucas.server.common.Constants.getFinnhubRateLimiterNames;
import static com.lucas.server.common.Constants.isTradingDate;
import static com.lucas.server.common.Constants.toPastOrFutureTradeDate;

@SuppressWarnings("LoggingSimilarMessage")
@Service
@Slf4j
public class DataManager {

    private final SymbolJpaService symbolService;
    private final MarketDataJpaService marketDataService;
    private final MarketSnapshotJpaService marketSnapshotService;
    private final NewsJpaService newsService;
    private final NewsPersistenceOrchestrator newsPersistenceOrchestrator;
    private final RecommendationsJpaService recommendationsService;
    private final YahooFinanceNewsClient yahooFinanceNewsClient;
    private final YahooFinanceMarketSnapshotClient yahooFinanceMarketSnapshotClient;
    private final FinnhubNewsClient finnhubNewsClient;
    private final FinnhubMarketDataClient finnhubMarketDataClient;
    private final TwelveDataMarketDataClient twelveDataMarketDataClient;
    private final PortfolioManager portfolioManager;
    private final RecommendationChatCompletionClient recommendationClient;
    private final Map<MarketDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, PortfolioService> portfolioTypeToService;

    public DataManager(SymbolJpaService symbolService,
                       MarketDataJpaService marketDataService,
                       MarketSnapshotJpaService marketSnapshotService,
                       NewsJpaService newsService,
                       NewsPersistenceOrchestrator newsPersistenceOrchestrator,
                       RecommendationsJpaService recommendationsService,
                       YahooFinanceNewsClient yahooFinanceNewsClient,
                       YahooFinanceMarketSnapshotClient yahooFinanceMarketSnapshotClient,
                       FinnhubMarketDataClient finnhubMarketDataClient,
                       TwelveDataMarketDataClient twelveDataMarketDataClient,
                       RecommendationChatCompletionClient recommendationClient,
                       PortfolioJpaService portfolioService,
                       PortfolioMockJpaService portfolioMockService,
                       FinnhubNewsClient newsClient,
                       PortfolioManager portfolioManager) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.marketSnapshotService = marketSnapshotService;
        this.newsService = newsService;
        this.newsPersistenceOrchestrator = newsPersistenceOrchestrator;
        this.recommendationsService = recommendationsService;
        this.yahooFinanceNewsClient = yahooFinanceNewsClient;
        this.yahooFinanceMarketSnapshotClient = yahooFinanceMarketSnapshotClient;
        this.finnhubMarketDataClient = finnhubMarketDataClient;
        this.twelveDataMarketDataClient = twelveDataMarketDataClient;
        this.recommendationClient = recommendationClient;
        finnhubNewsClient = newsClient;
        this.portfolioManager = portfolioManager;
        typeToRunner = Map.of(MarketDataType.LAST,
                this::retrieveMarketDataWithBackupStrategy,
                MarketDataType.HISTORIC,
                this::retrieveTwelveDataMarketData,
                MarketDataType.REAL_TIME,
                symbols -> {
                    OrderedIndexedSet<MarketDataDomain> res = new OrderedIndexedSetImpl<>();
                    for (SymbolDomain symbol : symbols) {
                        res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                    }
                    return OrderedIndexedSet.copyOf(res);
                });
        portfolioTypeToService =
                new EnumMap<>(Map.of(PortfolioType.REAL, portfolioService, PortfolioType.MOCK, portfolioMockService));
    }

    public OrderedIndexedSet<Long> getTopRecommendedSymbols(String action,
                                                            BigDecimal confidenceThreshold,
                                                            LocalDate recommendationDate) {
        return recommendationsService.getTopRecommendedSymbols(action, confidenceThreshold, recommendationDate);
    }

    public Set<RecommendationDomain> getDailyRecommendations(BigDecimal confidenceThreshold,
                                                             LocalDate date,
                                                             String action,
                                                             Set<String> clients) {
        return recommendationsService.getDailyRecommendations(confidenceThreshold, date, action, clients);
    }

    public Set<RecommendationDomain> getRecommendationsById(Set<Long> symbolIds,
                                                            Set<AiClient> clients,
                                                            CheekyClients cheekyClients,
                                                            Set<AiClient> backupClients,
                                                            PortfolioType type,
                                                            boolean overwrite,
                                                            boolean onTheFlyNews,
                                                            boolean fetchPreMarket,
                                                            boolean useOldNews) {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        return getRecommendationsInParallel(symbols,
                clients,
                cheekyClients,
                backupClients,
                type,
                overwrite,
                false,
                onTheFlyNews,
                fetchPreMarket,
                useOldNews);
    }

    @Transactional
    public Set<RecommendationDomain> getRandomRecommendations(Set<String> symbolNames,
                                                              Set<AiClient> clients,
                                                              CheekyClients cheekyClients,
                                                              Set<AiClient> backupClients,
                                                              PortfolioType type,
                                                              int count,
                                                              boolean overwrite,
                                                              boolean onlyIfHasNews,
                                                              boolean onTheFlyNews,
                                                              boolean fetchPreMarket,
                                                              boolean useOldNews) {

        Set<Long> candidates = symbolService.findAll()
                .stream()
                .filter(s -> symbolNames.contains(s.getName()))
                .map(SymbolDomain::getId)
                .collect(Collectors.toSet());
        if (!overwrite) {
            Set<Long> already = recommendationsService.findByDateBetween(LocalDate.now(), LocalDate.now().plusDays(1))
                    .stream()
                    .map(r -> r.getSymbol().getId())
                    .collect(Collectors.toUnmodifiableSet());
            candidates.removeAll(already);
        }

        if (0 >= count || candidates.isEmpty()) {
            return Set.of();
        }
        List<Long> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool);
        Set<Long> finalList = Set.copyOf(pool.subList(0, Math.min(count, pool.size())));

        return getRecommendationsInParallel(symbolService.findAllById(finalList),
                clients,
                cheekyClients,
                backupClients,
                type,
                overwrite,
                onlyIfHasNews,
                onTheFlyNews,
                fetchPreMarket,
                useOldNews);
    }

    @Transactional
    public Set<NewsDomain> generateSentiment(Set<String> symbolNames, LocalDateTime from, LocalDateTime to) {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        log.info(GENERATION_SUCCESSFUL_INFO, SENTIMENT);
        return newsService.generateSentiment(symbols.stream()
                .map(SymbolDomain::getId)
                .collect(Collectors.toUnmodifiableSet()), from, to);
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsByName(Set<String> symbolNames) throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        Set<NewsDomain> news = retrieveYahooNews(symbols);
        newsPersistenceOrchestrator.persistNews(news);
        return news;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsById(Set<Long> symbolIds) throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        Set<NewsDomain> news = retrieveYahooNews(symbols);
        newsPersistenceOrchestrator.persistNews(news);
        return news;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsByDateRangeAndId(Set<Long> symbolIds, LocalDate from, LocalDate to)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        Set<NewsDomain> news = retrieveNewsByDateRange(symbols, from, to, false);
        newsPersistenceOrchestrator.persistNews(news);
        return news;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsByDateRangeAndName(Set<String> symbolNames,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          boolean withYahooNews)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        Set<NewsDomain> news = retrieveNewsByDateRange(symbols, from, to, withYahooNews);
        newsPersistenceOrchestrator.persistNews(news);
        return news;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<MarketDataDomain> retrieveMarketData(Set<String> symbolNames, MarketDataType type, boolean override)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        Set<MarketDataDomain> mds = new HashSet<>();
        for (SymbolDomain symbol : symbols) {
            OrderedIndexedSet<MarketDataDomain> retrieved = typeToRunner.get(type).apply(Set.of(symbol));
            if (override) {
                marketDataService.createOrUpdate(retrieved);
            } else {
                marketDataService.createIgnoringDuplicates(retrieved);
            }
            mds.addAll(retrieved);
        }
        log.info(GENERATION_SUCCESSFUL_INFO, MARKET_DATA);
        return mds;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<MarketSnapshotDomain> retrieveSnapshotsByName(Set<String> symbolNames) {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveSnapshots(symbols);
    }

    @Transactional(rollbackFor = IllegalStateException.class)
    public PortfolioDomain executePortfolioAction(PortfolioType type,
                                                  Long symbolId,
                                                  BigDecimal price,
                                                  BigDecimal quantity,
                                                  BigDecimal commission,
                                                  LocalDateTime timestamp,
                                                  boolean isBuy) throws IllegalStateException {
        SymbolDomain symbol = symbolService.findById(symbolId)
                .orElseThrow(() -> new IllegalStateException(MessageFormat.format(SYMBOL_NOT_FOUND_ERROR, symbolId)));
        PortfolioService service = portfolioTypeToService.get(type);
        return service.executePortfolioAction(symbol, price, quantity, commission, timestamp, isBuy);
    }

    public Set<PortfolioManager.SymbolStand> getPortfolioStand(Set<String> symbolNames, PortfolioType type) {
        Set<PortfolioDomain> portfolio = portfolioTypeToService.get(type)
                .findActivePortfolio()
                .stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .collect(Collectors.toUnmodifiableSet());
        return getStand(portfolio);
    }

    public Set<PortfolioManager.SymbolStand> getAllAsPortfolioStandById(Set<Long> symbolIds,
                                                                        PortfolioType type,
                                                                        boolean dynamic)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        Map<SymbolDomain, PortfolioDomain> portfolioBySymbol = portfolioTypeToService.get(type)
                .findActivePortfolio()
                .stream()
                .filter(p -> symbols.contains(p.getSymbol()))
                .collect(Collectors.toMap(PortfolioDomain::getSymbol, Function.identity()));
        symbols.forEach(s -> portfolioBySymbol.computeIfAbsent(s, symbol -> new PortfolioDomain().setSymbol(symbol)));
        return dynamic
                ? getStandDynamically(Set.copyOf(portfolioBySymbol.values()))
                : getStand(Set.copyOf(portfolioBySymbol.values()));
    }

    public Set<PortfolioManager.SymbolStand> getAllAsPortfolioStand(Set<String> symbolNames, PortfolioType type) {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        Map<SymbolDomain, PortfolioDomain> portfolioBySymbol = portfolioTypeToService.get(type)
                .findActivePortfolio()
                .stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .collect(Collectors.toMap(PortfolioDomain::getSymbol, Function.identity()));
        symbols.forEach(s -> portfolioBySymbol.computeIfAbsent(s, symbol -> new PortfolioDomain().setSymbol(symbol)));

        return getStand(Set.copyOf(portfolioBySymbol.values()));
    }

    @Transactional
    public Set<NewsDomain> removeOldNews(int keepCount) {
        Set<NewsDomain> res = new HashSet<>();
        Set<SymbolDomain> symbols = symbolService.findAll();
        for (SymbolDomain symbol : symbols) {
            Set<Long> keepIds = newsService.getTopForSymbolId(symbol.getId(), keepCount)
                    .stream()
                    .map(NewsDomain::getId)
                    .collect(Collectors.toUnmodifiableSet());
            Set<NewsDomain> toRemove = newsService.findByIdIn(symbol.getNewsIds()
                    .stream()
                    .filter(id -> !keepIds.contains(id))
                    .collect(Collectors.toUnmodifiableSet()));
            res.addAll(toRemove);
            toRemove.forEach(n -> {
                symbol.getNewsIds().remove(n.getId());
                n.getSymbols().remove(symbol);
            });
            newsService.saveAll(toRemove);
        }

        Set<NewsDomain> orphanedNews = newsService.findOrphanedNews();
        Set<Long> orphanedIds = orphanedNews.stream().map(NewsDomain::getId).collect(Collectors.toUnmodifiableSet());
        Set<RecommendationDomain> recommendations = recommendationsService.findByNewsId(orphanedIds);
        recommendations.forEach(r -> r.getNews().removeIf(n -> orphanedIds.contains(n.getId())));
        recommendationsService.saveAll(recommendations);
        newsService.deleteAll(orphanedNews);

        return res;
    }

    @Transactional
    public Set<MarketDataDomain> removeOldMarketData(int keepCount) {
        Set<MarketDataDomain> res = new HashSet<>();
        Set<SymbolDomain> symbols = symbolService.findAll();
        for (SymbolDomain symbol : symbols) {
            Set<MarketDataDomain> toKeep = marketDataService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<MarketDataDomain> toRemove = marketDataService.findBySymbolId(symbol.getId())
                    .stream()
                    .filter(marketData -> !toKeep.contains(marketData))
                    .collect(Collectors.toUnmodifiableSet());
            res.addAll(toRemove);
        }
        marketDataService.deleteAll(res);
        return res;
    }

    @Transactional
    public Set<MarketSnapshotDomain> removeOldSnapshots(int keepCount) {
        Set<MarketSnapshotDomain> res = new HashSet<>();
        Set<SymbolDomain> symbols = symbolService.findAll();
        for (SymbolDomain symbol : symbols) {
            Set<MarketSnapshotDomain> toKeep = marketSnapshotService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<MarketSnapshotDomain> toRemove = marketSnapshotService.findBySymbolId(symbol.getId())
                    .stream()
                    .filter(marketSnapshot -> !toKeep.contains(marketSnapshot))
                    .collect(Collectors.toUnmodifiableSet());
            res.addAll(toRemove);
        }
        marketSnapshotService.deleteAll(res);
        return res;
    }

    @Transactional
    public Set<RecommendationDomain> removeOldRecommendations(int keepCount) {
        Set<RecommendationDomain> res = new HashSet<>();
        Set<SymbolDomain> symbols = symbolService.findAll();
        for (SymbolDomain symbol : symbols) {
            Set<RecommendationDomain> toKeep = recommendationsService.getTopForSymbolId(symbol.getId(), keepCount);
            Set<RecommendationDomain> toRemove = recommendationsService.findBySymbolId(symbol.getId())
                    .stream()
                    .filter(recommendation -> !toKeep.contains(recommendation))
                    .collect(Collectors.toUnmodifiableSet());
            res.addAll(toRemove);
        }
        recommendationsService.deleteAll(res);
        return res;
    }

    private Set<RecommendationDomain> getRecommendationsWithBackup(Set<SymbolPayload> buffer,
                                                                   OrderedIndexedSet<AiClient> clients,
                                                                   Deque<AiClient> backupClients,
                                                                   boolean useOldNews)
            throws ClientException, MappingException {
        try {
            return recommendationClient.getRecommendations(buffer, clients, useOldNews);
        } catch (ClientException e) {
            if (backupClients.isEmpty()) {
                throw e;
            }
            backupClients.add(backupClients.pollFirst());
            log.warn(CLIENT_FAILED_BACKUP_WARN,
                    clients.stream().map(c -> c.getConfig().name()).toList(),
                    buffer.stream().map(SymbolPayload::getSymbol).toList(),
                    e);
            return recommendationClient.getRecommendations(buffer, OrderedIndexedSet.copyOf(backupClients), useOldNews);
        }
    }

    private OrderedIndexedSet<MarketDataDomain> retrieveMarketDataWithBackupStrategy(Set<SymbolDomain> symbols)
            throws ClientException, MappingException {
        OrderedIndexedSet<MarketDataDomain> res = new OrderedIndexedSetImpl<>();
        for (SymbolDomain symbol : symbols) {
            try {
                res.add(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.LAST).getFirst());
            } catch (ClientException | MappingException e) {
                log.warn(CLIENT_FAILED_BACKUP_WARN, twelveDataMarketDataClient.getClass().getSimpleName(), symbol, e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
            }
        }
        return OrderedIndexedSet.copyOf(res);
    }

    private OrderedIndexedSet<MarketDataDomain> retrieveTwelveDataMarketData(Set<SymbolDomain> symbols)
            throws ClientException, MappingException {
        OrderedIndexedSet<MarketDataDomain> res = new OrderedIndexedSetImpl<>();
        for (SymbolDomain symbol : symbols) {
            res.addAll(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.HISTORIC));
        }
        return OrderedIndexedSet.copyOf(res);
    }

    private Set<RecommendationDomain> getRecommendationsInParallel(Set<SymbolDomain> symbols,
                                                                   Set<AiClient> clients,
                                                                   CheekyClients cheekyClients,
                                                                   Set<AiClient> backupClients,
                                                                   PortfolioType type,
                                                                   boolean overwrite,
                                                                   boolean onlyIfHasNews,
                                                                   boolean onTheFlyNews,
                                                                   boolean fetchPreMarket,
                                                                   boolean useOldNews) {
        Set<RecommendationDomain> res = new HashSet<>();
        Deque<AiClient> mutableClients = new ConcurrentLinkedDeque<>(clients);
        Deque<AiClient> mutableCheekyClients = new ConcurrentLinkedDeque<>(cheekyClients.getClients());
        Deque<AiClient> mutableBackupClients = new ConcurrentLinkedDeque<>(backupClients);
        PortfolioService portfolioService = portfolioTypeToService.get(type);

        LocalDateTime startUtc;
        if (useOldNews && !onTheFlyNews) {
            startUtc = null;
        } else {
            ZonedDateTime easternTime = ZonedDateTime.now(NY_ZONE);
            LocalDate today = easternTime.toLocalDate();
            LocalDate lastTradeFinishedDate = !easternTime.toLocalTime().isBefore(MARKET_CLOSE) && isTradingDate(today)
                    ? today
                    : toPastOrFutureTradeDate(today, 1, d -> d.minusDays(1));
            LocalTime close = !EARLY_CLOSE_DATES_2026.contains(lastTradeFinishedDate) ? MARKET_CLOSE : EARLY_CLOSE;
            startUtc = ZonedDateTime.of(lastTradeFinishedDate, close, NY_ZONE)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        }

        Set<SymbolDomain> remainingSymbols = ConcurrentHashMap.newKeySet();
        remainingSymbols.addAll(symbols);

        int attempts = 0;
        while (!remainingSymbols.isEmpty() && RECOMMENDATION_MAX_ATTEMPTS > attempts) {
            BlockingQueue<SymbolPayload> payloadQueue = new LinkedBlockingQueue<>();
            int payloadsSubmitted = 0;
            BlockingQueue<Set<RecommendationDomain>> resultsQueue = new LinkedBlockingQueue<>();
            int submitted = 0;
            log.info(RETRIEVING_DATA_INFO, RECOMMENDATION, remainingSymbols.size());
            // fixed payload computer size to not overwhelm Hikari
            int poolSize = getFinnhubRateLimiterNames().size();
            // ExecutorService::close would block until tasks finish, so only a single try-with-resources
            try (ExecutorService payloadComputationExecutor = Executors.newFixedThreadPool(poolSize);
                 ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (SymbolDomain symbol : Set.copyOf(remainingSymbols)) {
                    payloadComputationExecutor.submit(() -> {
                        try {
                            OrderedIndexedSet<MarketDataDomain> marketData =
                                    marketDataService.getTopForSymbolId(symbol.getId(),
                                            MARKET_DATA_RELEVANT_DAYS_COUNT);
                            if (marketData.isEmpty()) {
                                remainingSymbols.remove(symbol);
                                Interrupts.runOrThrow(() -> payloadQueue.put(SymbolPayload.empty()),
                                        e -> log.error(e.getMessage(), e));
                                return;
                            }
                            OrderedIndexedSet<NewsDomain> news =
                                    provideNews(onTheFlyNews, useOldNews, symbol, startUtc);
                            if (onlyIfHasNews && news.isEmpty()) {
                                remainingSymbols.remove(symbol);
                                Interrupts.runOrThrow(() -> payloadQueue.put(SymbolPayload.empty()),
                                        e -> log.error(e.getMessage(), e));
                                return;
                            }
                            PortfolioDomain portfolio = portfolioService.findBySymbol(symbol)
                                    .orElseGet(() -> new PortfolioDomain().setSymbol(symbol));

                            SymbolPayload payload = new SymbolPayload(symbol, marketData, portfolio).setNews(news)
                                    .setPremarket(providePremarket(fetchPreMarket, symbol));
                            Interrupts.runOrThrow(() -> payloadQueue.put(payload), e -> log.error(e.getMessage(), e));
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                    payloadsSubmitted++;
                }

                Set<SymbolPayload> recommendationBuffer = new HashSet<>();
                for (int i = 0; i < payloadsSubmitted; i++) {
                    SymbolPayload payload = Interrupts.callOrSwallow(payloadQueue::take,
                            SymbolPayload::empty,
                            e -> log.error(e.getMessage(), e));
                    if (null == Objects.requireNonNull(payload).getSymbol()) {
                        continue;
                    }
                    recommendationBuffer.add(payload);

                    boolean withCheekyClients = !mutableCheekyClients.isEmpty() && 0 < cheekyClients.getRemaining();
                    Deque<AiClient> clientsRef = withCheekyClients ? mutableCheekyClients : mutableClients;
                    int minChunkSize = clientsRef.stream().mapToInt(c -> c.getConfig().chunkSize()).min().orElseThrow();
                    if (recommendationBuffer.size() == minChunkSize) {
                        submitChunk(Set.copyOf(recommendationBuffer),
                                OrderedIndexedSet.copyOf(clientsRef),
                                mutableBackupClients,
                                executor,
                                resultsQueue,
                                overwrite,
                                useOldNews);
                        if (withCheekyClients) {
                            cheekyClients.decrementRemaining();
                        }
                        submitted++;
                        clientsRef.add(clientsRef.pollFirst());
                        recommendationBuffer.clear();
                    }
                }
                if (!recommendationBuffer.isEmpty()) {
                    boolean withCheekyClients = !mutableCheekyClients.isEmpty() && 0 < cheekyClients.getRemaining();
                    Deque<AiClient> clientsRef = withCheekyClients ? mutableCheekyClients : mutableClients;
                    submitChunk(Set.copyOf(recommendationBuffer),
                            OrderedIndexedSet.copyOf(clientsRef),
                            mutableBackupClients,
                            executor,
                            resultsQueue,
                            overwrite,
                            useOldNews);
                    if (withCheekyClients) {
                        cheekyClients.decrementRemaining();
                    }
                    submitted++;
                    clientsRef.add(clientsRef.pollFirst());
                }
            }

            for (int i = 0; i < submitted; i++) {
                Set<RecommendationDomain> fetched =
                        Interrupts.callOrSwallow(resultsQueue::take, Set::of, e -> log.error(e.getMessage(), e));
                if (!Objects.requireNonNull(fetched).isEmpty()) {
                    res.addAll(fetched);
                    Set<SymbolDomain> fetchedSymbols = fetched.stream()
                            .map(RecommendationDomain::getSymbol)
                            .collect(Collectors.toUnmodifiableSet());
                    remainingSymbols.removeIf(fetchedSymbols::contains);
                }
            }
            attempts++;
        }

        if (!remainingSymbols.isEmpty()) {
            log.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, remainingSymbols);
        }
        return res;
    }

    private OrderedIndexedSet<NewsDomain> provideNews(boolean onTheFlyNews,
                                                      boolean useOldNews,
                                                      SymbolDomain symbol,
                                                      LocalDateTime startUtc) {
        if (onTheFlyNews) {
            Set<SymbolDomain> symbols = Set.of(symbol);
            Set<NewsDomain> news = new HashSet<>();
            try {
                news.addAll(retrieveNewsByDateRange(symbols, startUtc.toLocalDate(), LocalDate.now(), false));
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN, NEWS, symbol, e);
            }
            try {
                news.addAll(retrieveYahooNews(symbols));
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN, NEWS, symbol, e);
            }
            newsPersistenceOrchestrator.persistNews(news);
        }

        return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT)
                .stream()
                .filter(n -> useOldNews || n.getDate().isAfter(startUtc))
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    private MarketSnapshotDomain providePremarket(boolean fetchPreMarket, SymbolDomain symbol) {
        if (fetchPreMarket) {
            try {
                return yahooFinanceMarketSnapshotClient.retrieveMarketSnapshot(symbol);
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN, MARKET_SNAPSHOT, symbol, e);
            }
        }
        return null;
    }

    private void submitChunk(Set<SymbolPayload> buffer,
                             OrderedIndexedSet<AiClient> clients,
                             Deque<AiClient> backupClients,
                             ExecutorService executor,
                             BlockingQueue<Set<RecommendationDomain>> resultsQueue,
                             boolean overwrite,
                             boolean useOldNews) {
        executor.submit(() -> {
            try {
                Set<RecommendationDomain> partial =
                        getRecommendationsWithBackup(buffer, clients, backupClients, useOldNews);
                log.info(GENERATION_SUCCESSFUL_INFO, RECOMMENDATION);
                Set<NewsDomain> mergedNews = Set.copyOf(partial.stream()
                        .flatMap(r -> r.getNews().stream())
                        .collect(Collectors.toUnmodifiableMap(NewsDomain::getId, Function.identity(), (a, b) -> {
                            a.getSymbols().addAll(b.getSymbols());
                            return a;
                        }))
                        .values());
                Map<Long, NewsDomain> persistedNews;
                persistedNews = newsPersistenceOrchestrator.persistNews(mergedNews)
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(NewsDomain::getExternalId, Function.identity()));
                partial.forEach(r -> r.setNews(r.getNews()
                        .stream()
                        .map(n -> persistedNews.get(n.getExternalId()))
                        .collect(Collectors.toUnmodifiableSet())));
                Set<RecommendationDomain> finalPartial;
                if (overwrite) {
                    finalPartial = recommendationsService.createOrUpdate(partial);
                } else {
                    finalPartial = partial;
                    recommendationsService.createIgnoringDuplicates(partial);
                }
                Interrupts.runOrThrow(() -> resultsQueue.put(finalPartial), e -> log.error(e.getMessage(), e));
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN,
                        RECOMMENDATION,
                        buffer.stream().map(SymbolPayload::getSymbol).toList(),
                        e);
                Interrupts.runOrThrow(() -> resultsQueue.put(Set.of()), ie -> log.error(ie.getMessage(), ie));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private Set<NewsDomain> retrieveYahooNews(Set<SymbolDomain> symbols) throws ClientException, MappingException {
        Map<Long, NewsDomain> newsByExternalId = new HashMap<>();
        for (SymbolDomain symbol : symbols) {
            Set<NewsDomain> updated = yahooFinanceNewsClient.retrieveNews(symbol);
            for (NewsDomain news : updated) {
                newsByExternalId.computeIfAbsent(news.getExternalId(), id -> news).addSymbol(symbol);
            }
        }
        log.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        return Set.copyOf(newsByExternalId.values());
    }

    private Set<NewsDomain> retrieveNewsByDateRange(Set<SymbolDomain> symbols,
                                                    LocalDate from,
                                                    LocalDate to,
                                                    boolean withYahooNews) throws ClientException, MappingException {
        Map<Long, NewsDomain> newsByExternalId = new HashMap<>();
        for (SymbolDomain symbol : symbols) {
            Set<NewsDomain> updated = finnhubNewsClient.retrieveNewsByDateRange(symbol, from, to);
            for (NewsDomain news : updated) {
                newsByExternalId.computeIfAbsent(news.getExternalId(), id -> news).addSymbol(symbol);
            }
        }
        Set<NewsDomain> res = newsByExternalId.values()
                .stream()
                .filter(n -> withYahooNews || !"Yahoo".equals(n.getSource()))
                .collect(Collectors.toUnmodifiableSet());
        log.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        return res;
    }

    private Set<MarketSnapshotDomain> retrieveSnapshots(Set<SymbolDomain> symbols) {
        Set<MarketSnapshotDomain> mss = new HashSet<>();
        for (SymbolDomain symbol : symbols) {
            try {
                mss.add(yahooFinanceMarketSnapshotClient.retrieveMarketSnapshot(symbol));
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN, MARKET_SNAPSHOT, symbol, e);
            }
        }
        log.info(GENERATION_SUCCESSFUL_INFO, MARKET_SNAPSHOT);
        return marketSnapshotService.saveAll(mss);
    }

    private OrderedIndexedSet<PortfolioManager.SymbolStand> getStand(Set<PortfolioDomain> portfolio) {
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream())
                .sorted(Comparator.comparing(PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    private OrderedIndexedSet<PortfolioManager.SymbolStand> getStandDynamically(Set<PortfolioDomain> portfolio)
            throws ClientException, MappingException {
        Map<String, MarketDataDomain> symbolsWithData = typeToRunner.get(MarketDataType.REAL_TIME)
                .apply(portfolio.stream().map(PortfolioDomain::getSymbol).collect(Collectors.toUnmodifiableSet()))
                .stream()
                .collect(Collectors.toUnmodifiableMap(md -> md.getSymbol().getName(), Function.identity()));
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p,
                                symbolsWithData.get(md.getSymbol().getName()).setRecommendations(
                                        // artificially link the recommendations to the volatile mds
                                        md.getRecommendations())))
                        .stream())
                .sorted(Comparator.comparing(PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    @FunctionalInterface
    private interface TypeToMarketDataFunction {
        OrderedIndexedSet<MarketDataDomain> apply(Set<SymbolDomain> symbols) throws ClientException, MappingException;
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(chain = true)
    public static class SymbolPayload {
        private final SymbolDomain symbol;
        private final OrderedIndexedSet<MarketDataDomain> marketData;
        private final PortfolioDomain portfolio;
        @Setter
        private MarketSnapshotDomain premarket;
        @Setter
        private OrderedIndexedSet<NewsDomain> news;

        public static SymbolPayload empty() {
            return new SymbolPayload(null, null, null);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class CheekyClients {

        private final Set<AiClient> clients;
        private int remaining;

        public static CheekyClients empty() {
            return new CheekyClients(Set.of(), 0);
        }

        public void decrementRemaining() {
            remaining--;
        }
    }
}
