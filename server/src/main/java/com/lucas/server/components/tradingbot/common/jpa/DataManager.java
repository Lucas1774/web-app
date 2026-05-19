package com.lucas.server.components.tradingbot.common.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import static com.lucas.server.common.Constants.isTradingDate;

@SuppressWarnings("LoggingSimilarMessage")
@Service
@Slf4j
public class DataManager {

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
    private final Map<PortfolioType, PortfolioService> portfolioTypeToService;

    public DataManager(SymbolJpaService symbolService,
                       MarketDataJpaService marketDataService,
                       MarketSnapshotJpaService marketSnapshotService,
                       NewsJpaService newsService,
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
                                                            PortfolioType type,
                                                            boolean overwrite,
                                                            boolean onTheFlyNews,
                                                            boolean fetchPreMarket,
                                                            boolean useOldNews) {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        return getRecommendationsInParallel(symbols,
                clients,
                type,
                overwrite,
                false,
                onTheFlyNews,
                fetchPreMarket,
                useOldNews);
    }

    private Set<RecommendationDomain> getRecommendationsInParallel(Set<SymbolDomain> symbols,
                                                                   Set<AiClient> clients,
                                                                   PortfolioType type,
                                                                   boolean overwrite,
                                                                   boolean onlyIfHasNews,
                                                                   boolean onTheFlyNews,
                                                                   boolean fetchPreMarket,
                                                                   boolean useOldNews) {
        Set<RecommendationDomain> res = new HashSet<>();
        Deque<AiClient> mutableClients = new ConcurrentLinkedDeque<>(clients);
        PortfolioService portfolioService = portfolioTypeToService.get(type);
        int minChunkSize = clients.stream().mapToInt(c -> c.getConfig().chunkSize()).min().orElseThrow();

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
            LocalTime close = !EARLY_CLOSE_DATES_2026.contains(lastTradeFinishedDate) ? MARKET_CLOSE : EARLY_CLOSE;
            startUtc = ZonedDateTime.of(lastTradeFinishedDate, close, NY_ZONE)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        }

        Set<SymbolDomain> remainingSymbols = new HashSet<>(symbols);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            int attempts = 0;
            while (!remainingSymbols.isEmpty() && RECOMMENDATION_MAX_ATTEMPTS > attempts) {
                BlockingQueue<Set<RecommendationDomain>> resultsQueue = new LinkedBlockingQueue<>();
                int submitted = 0;
                Set<SymbolPayload> recommendationBuffer = new HashSet<>();
                log.info(RETRIEVING_DATA_INFO, RECOMMENDATION, remainingSymbols.size());
                for (SymbolDomain symbol : Set.copyOf(remainingSymbols)) {
                    OrderedIndexedSet<MarketDataDomain> marketData =
                            marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT);
                    if (marketData.isEmpty()) {
                        remainingSymbols.remove(symbol);
                        continue;
                    }
                    OrderedIndexedSet<NewsDomain> news = provideNews(onTheFlyNews, useOldNews, symbol, startUtc);
                    if (onlyIfHasNews && news.isEmpty()) {
                        remainingSymbols.remove(symbol);
                        continue;
                    }
                    PortfolioDomain portfolio = portfolioService.findBySymbol(symbol)
                            .orElseGet(() -> new PortfolioDomain().setSymbol(symbol));

                    SymbolPayload payload = new SymbolPayload(symbol, marketData, portfolio).setNews(news);
                    if (fetchPreMarket) {
                        try {
                            payload.setPremarket(yahooFinanceMarketSnapshotClient.retrieveMarketSnapshot(symbol));
                        } catch (ClientException | MappingException e) {
                            log.warn(RETRIEVAL_FAILED_WARN, MARKET_SNAPSHOT, symbol, e);
                        }
                    }
                    recommendationBuffer.add(payload);
                    if (recommendationBuffer.size() == minChunkSize) {
                        submitChunk(Set.copyOf(recommendationBuffer),
                                OrderedIndexedSet.copyOf(mutableClients),
                                executor,
                                resultsQueue,
                                overwrite,
                                useOldNews);
                        submitted++;
                        mutableClients.add(mutableClients.pollFirst());
                        recommendationBuffer.clear();
                    }
                }
                if (!recommendationBuffer.isEmpty()) {
                    submitChunk(Set.copyOf(recommendationBuffer),
                            OrderedIndexedSet.copyOf(mutableClients),
                            executor,
                            resultsQueue,
                            overwrite,
                            useOldNews);
                    submitted++;
                    mutableClients.add(mutableClients.pollFirst());
                }

                for (int i = 0; i < submitted; i++) {
                    Set<RecommendationDomain> fetched = Interrupts.callOrSwallow(resultsQueue::take,
                            Collections::emptySet,
                            e -> log.error(e.getMessage(), e));
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
            try {
                newsService.createOrUpdate(retrieveYahooNews(Set.of(symbol)));
            } catch (ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN, NEWS, symbol, e);
            }
        }

        return newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT)
                .stream()
                .filter(n -> useOldNews || n.getDate().isAfter(startUtc))
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
    }

    private void submitChunk(Set<SymbolPayload> buffer,
                             Set<AiClient> clients,
                             ExecutorService executor,
                             BlockingQueue<Set<RecommendationDomain>> resultsQueue,
                             boolean overwrite,
                             boolean useOldNews) {
        executor.submit(() -> {
            try {
                Set<RecommendationDomain> partial =
                        recommendationClient.getRecommendations(buffer, clients, useOldNews);
                log.info(GENERATION_SUCCESSFUL_INFO, RECOMMENDATION);
                Set<NewsDomain> mergedNews = Set.copyOf(partial.stream()
                        .flatMap(r -> r.getNews().stream())
                        .collect(Collectors.toUnmodifiableMap(NewsDomain::getId, Function.identity(), (a, b) -> {
                            a.getSymbols().addAll(b.getSymbols());
                            return a;
                        }))
                        .values());
                Map<Long, NewsDomain> persistedNews;
                synchronized (newsPersistLock) {
                    persistedNews = newsService.createOrUpdate(mergedNews)
                            .stream()
                            .collect(Collectors.toUnmodifiableMap(NewsDomain::getExternalId, Function.identity()));
                }
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
            } catch (JsonProcessingException | ClientException | MappingException e) {
                log.warn(RETRIEVAL_FAILED_WARN,
                        RECOMMENDATION,
                        buffer.stream().map(SymbolPayload::getSymbol).toList(),
                        e);
                Interrupts.runOrThrow(() -> resultsQueue.put(Collections.emptySet()),
                        ie -> log.error(ie.getMessage(), ie));
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

        return Set.copyOf(newsByExternalId.values());
    }

    @Transactional
    public Set<RecommendationDomain> getRandomRecommendations(Set<String> symbolNames,
                                                              Set<AiClient> clients,
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
        return retrieveNews(symbols);
    }

    private Set<NewsDomain> retrieveNews(Set<SymbolDomain> symbols) throws ClientException, MappingException {
        Set<NewsDomain> news = retrieveYahooNews(symbols);
        log.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(news);
        return news;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsById(Set<Long> symbolIds) throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        return retrieveNews(symbols);
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsByDateRangeAndId(Set<Long> symbolIds, LocalDate from, LocalDate to)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.findAllById(symbolIds);
        return retrieveNewsByDateRange(symbols, from, to, false);
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
        newsService.createOrUpdate(res);
        return res;
    }

    @Transactional(rollbackFor = {ClientException.class, MappingException.class})
    public Set<NewsDomain> retrieveNewsByDateRangeAndName(Set<String> symbolNames,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          boolean withYahooNews)
            throws ClientException, MappingException {
        Set<SymbolDomain> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveNewsByDateRange(symbols, from, to, withYahooNews);
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

    private OrderedIndexedSet<PortfolioManager.SymbolStand> getStand(Set<PortfolioDomain> portfolio) {
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream())
                .sorted(Comparator.comparing(PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
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
    }
}
