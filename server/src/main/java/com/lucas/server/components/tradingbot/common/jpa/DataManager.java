package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.marketdata.service.YahooFinanceMarketDataClient;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import com.lucas.server.components.tradingbot.news.service.YahooFinanceNewsClient;
import com.lucas.server.components.tradingbot.portfolio.jpa.*;
import com.lucas.server.components.tradingbot.portfolio.service.PortfolioManager;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsJpaService;
import com.lucas.server.components.tradingbot.recommendation.service.RecommendationChatCompletionClient;
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
import java.util.function.LongFunction;
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
    private final NewsJpaService newsService;
    private final RecommendationsJpaService recommendationsService;
    private final YahooFinanceNewsClient yahooFinanceNewsClient;
    private final YahooFinanceMarketDataClient yahooFinanceMarketDataClient;
    private final FinnhubNewsClient finnhubNewsClient;
    private final FinnhubMarketDataClient finnhubMarketDataClient;
    private final TwelveDataMarketDataClient twelveDataMarketDataClient;
    private final PortfolioManager portfolioManager;
    private final RecommendationChatCompletionClient recommendationClient;
    private final Map<MarketDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;

    public DataManager(SymbolJpaService symbolService, MarketDataJpaService marketDataService, NewsJpaService newsService,
                       RecommendationsJpaService recommendationsService, YahooFinanceNewsClient yahooFinanceNewsClient,
                       YahooFinanceMarketDataClient yahooFinanceMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient, TwelveDataMarketDataClient twelveDataMarketDataClient,
                       RecommendationChatCompletionClient recommendationClient, PortfolioJpaService portfolioService,
                       PortfolioMockJpaService portfolioMockService, FinnhubNewsClient newsClient, PortfolioManager portfolioManager) {
        this.symbolService = symbolService;
        this.marketDataService = marketDataService;
        this.newsService = newsService;
        this.recommendationsService = recommendationsService;
        this.yahooFinanceNewsClient = yahooFinanceNewsClient;
        this.yahooFinanceMarketDataClient = yahooFinanceMarketDataClient;
        this.finnhubMarketDataClient = finnhubMarketDataClient;
        this.twelveDataMarketDataClient = twelveDataMarketDataClient;
        this.recommendationClient = recommendationClient;
        this.finnhubNewsClient = newsClient;
        this.portfolioManager = portfolioManager;
        typeToRunner = new EnumMap<>(Map.of(
                MarketDataType.LAST, this::retrieveMarketDataWithBackupStrategy,
                MarketDataType.HISTORIC, this::retrieveTwelveDataMarketData,
                MarketDataType.REAL_TIME, symbols -> {
                    List<MarketData> res = new ArrayList<>();
                    for (Symbol symbol : symbols) {
                        res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
                    }
                    return res;
                }
        ));
        portfolioTypeToService = new EnumMap<>(Map.of(
                PortfolioType.REAL, portfolioService,
                PortfolioType.MOCK, portfolioMockService
        ));
    }

    public List<Long> getTopRecommendedSymbols(String action, BigDecimal confidenceThreshold, LocalDate recommendationDate) {
        return recommendationsService.getTopRecommendedSymbols(action, confidenceThreshold, recommendationDate);
    }

    public List<Recommendation> getDailyRecommendations(BigDecimal confidenceThreshold, LocalDate date, String action, List<String> clients) {
        return recommendationsService.getDailyRecommendations(confidenceThreshold, date, action, clients);
    }

    @Transactional
    public List<Recommendation> getRecommendationsById(List<Long> symbolIds, List<AIClient> clients, PortfolioType type,
                                                       boolean overwrite, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        return getRecommendationsInParallel(symbols, clients, type, overwrite, false, onTheFlyNews, fetchPreMarket, useOldNews);
    }

    @Transactional
    public List<Recommendation> getRandomRecommendations(List<String> symbolNames, List<AIClient> clients, PortfolioType type, int count,
                                                         boolean overwrite, boolean onlyIfHasNews, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        Set<Long> already = recommendationsService.findByDateBetween(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
                .map(r -> r.getSymbol().getId())
                .collect(Collectors.toSet());

        Set<Long> candidates = symbolService.findAll().stream()
                .filter(s -> symbolNames.contains(s.getName()))
                .map(Symbol::getId)
                .collect(Collectors.toSet());
        candidates.removeAll(already);

        List<Long> finalList = new ArrayList<>();
        if (count > 0 && !candidates.isEmpty()) {
            List<Long> pool = new ArrayList<>(candidates);
            Collections.shuffle(pool);
            finalList.addAll(pool.subList(0, Math.min(count, pool.size())));
        }

        return getRecommendationsInParallel(symbolService.findAllById(finalList), clients, type, overwrite,
                onlyIfHasNews, onTheFlyNews, fetchPreMarket, useOldNews);
    }

    private List<Recommendation> getRecommendationsInParallel(List<Symbol> symbols, List<AIClient> clients, PortfolioType type, boolean overwrite,
                                                              boolean onlyIfHasNews, boolean onTheFlyNews, boolean fetchPreMarket, boolean useOldNews) {
        List<Recommendation> res = new ArrayList<>();
        List<AIClient> mutableClients = new ArrayList<>(clients);
        IPortfolioJpaService<PortfolioBase> portfolioService = getService(type);
        Supplier<PortfolioBase> portfolioSupplier = portfolioTypeToNewPortfolio.get(type);

        ZonedDateTime easternTime = ZonedDateTime.now(NY_ZONE);
        LocalDate lastTradeFinishedDate = easternTime.toLocalDate();
        if (easternTime.toLocalTime().isBefore(MARKET_CLOSE)) {
            lastTradeFinishedDate = lastTradeFinishedDate.minusDays(1);
        }
        while (!isTradingDate(lastTradeFinishedDate)) {
            lastTradeFinishedDate = lastTradeFinishedDate.minusDays(1);
        }
        LocalTime close = !EARLY_CLOSE_DATES_2025.contains(lastTradeFinishedDate) ? MARKET_CLOSE : EARLY_CLOSE;
        LocalDateTime startUtc = ZonedDateTime.of(lastTradeFinishedDate, close, NY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        List<SymbolPayload> remainingPayload = symbols.parallelStream()
                .map(symbol -> {
                    List<News> news = null;
                    if (!onTheFlyNews) {
                        if (!useOldNews) {
                            news = newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT).stream()
                                    .filter(n -> n.getDate().isAfter(startUtc)).toList();
                        } else {
                            news = newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT);
                        }
                    }
                    if (onlyIfHasNews && Objects.requireNonNull(news).isEmpty()) {
                        return Optional.<SymbolPayload>empty();
                    }

                    List<MarketData> marketData = marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT);
                    if (marketData.isEmpty()) {
                        return Optional.<SymbolPayload>empty();
                    }

                    PortfolioBase portfolio = portfolioService.findBySymbol(symbol)
                            .orElseGet(() -> portfolioSupplier.get().setSymbol(symbol));

                    return Optional.of(new SymbolPayload(symbol, marketData, portfolio).setNews(news));
                })
                .flatMap(Optional::stream)
                .toList();

        RecommendationChatCompletionClient.NewsFetcher newsFetcher;
        if (!onTheFlyNews) {
            newsFetcher = SymbolPayload::getNews;
        } else {
            if (!useOldNews) {
                newsFetcher = p -> {
                    List<News> news = retrieveYahooNews(List.of(p.getSymbol())).stream()
                            .filter(n -> n.getDate().isAfter(startUtc))
                            .sorted(Comparator.comparing(News::getDate).reversed())
                            .limit(NEWS_COUNT)
                            .toList();
                    synchronized (newsPersistLock) {
                        return newsService.createOrUpdate(news);
                    }
                };
            } else {
                newsFetcher = p -> {
                    List<News> news = retrieveYahooNews(List.of(p.getSymbol())).stream()
                            .sorted(Comparator.comparing(News::getDate).reversed())
                            .limit(NEWS_COUNT)
                            .toList();
                    synchronized (newsPersistLock) {
                        return newsService.createOrUpdate(news);
                    }
                };
            }
        }
        LongFunction<List<News>> backupNewsFetcher = id -> newsService.getTopForSymbolId(id, NEWS_COUNT).stream()
                .filter(n -> n.getDate().isAfter(startUtc)).toList();
        RecommendationChatCompletionClient.MarketDataFetcher marketDataFetcher = fetchPreMarket
                ? p -> yahooFinanceMarketDataClient.retrieveMarketData(p.getSymbol())
                : SymbolPayload::getPremarket;

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, remainingPayload.size());
        try (ExecutorService executor = Executors.newFixedThreadPool(clients.size())) {
            int attempts = 0;
            while (!remainingPayload.isEmpty() && attempts < RECOMMENDATION_MAX_ATTEMPTS) {
                List<Recommendation> fetched = doGetRecommendationsInParallel(remainingPayload, mutableClients, executor,
                        overwrite, newsFetcher, backupNewsFetcher, marketDataFetcher);
                res.addAll(fetched);
                Set<Symbol> fetchedSymbols = fetched.stream()
                        .map(Recommendation::getSymbol)
                        .collect(Collectors.toSet());
                remainingPayload = remainingPayload.stream()
                        .filter(p -> !fetchedSymbols.contains(p.getSymbol()))
                        .toList();
                attempts++;
            }
        }

        if (!remainingPayload.isEmpty()) {
            logger.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, remainingPayload.stream().map(SymbolPayload::getSymbol).toList());
        }
        return res;
    }

    private List<Recommendation> doGetRecommendationsInParallel(List<SymbolPayload> payload, List<AIClient> clients, ExecutorService executor,
                                                                boolean overwrite, RecommendationChatCompletionClient.NewsFetcher newsFetcher,
                                                                LongFunction<List<News>> backupNewsFetcher,
                                                                RecommendationChatCompletionClient.MarketDataFetcher marketDataFetcher) {
        List<BatchJob> jobs = new ArrayList<>();
        List<SymbolPayload> buffer = new ArrayList<>();

        for (SymbolPayload load : payload) {
            AIClient client = clients.getFirst();
            buffer.add(load);

            if (buffer.size() == client.getConfig().chunkSize()) {
                submitChunk(buffer, client, executor, jobs, overwrite, newsFetcher, backupNewsFetcher, marketDataFetcher);
                buffer.clear();
                Collections.rotate(clients, -1);
            }
        }

        if (!buffer.isEmpty()) {
            AIClient client = clients.getFirst();
            submitChunk(buffer, client, executor, jobs, overwrite, newsFetcher, backupNewsFetcher, marketDataFetcher);
            Collections.rotate(clients, -1);
        }

        List<Recommendation> res = new ArrayList<>();
        for (BatchJob job : jobs) {
            try {
                res.addAll(job.future().get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, job.symbols(), e);
            } catch (ExecutionException e) {
                logger.warn(RETRIEVAL_FAILED_WARN, RECOMMENDATION, job.symbols(), e);
            }
        }

        return res;
    }

    private void submitChunk(List<SymbolPayload> buffer, AIClient client, ExecutorService executor, List<BatchJob> jobs,
                             boolean overwrite, RecommendationChatCompletionClient.NewsFetcher newsFetcher,
                             LongFunction<List<News>> backupNewsFetcher,
                             RecommendationChatCompletionClient.MarketDataFetcher marketDataFetcher) {
        List<SymbolPayload> snapshot = new ArrayList<>(buffer);
        Callable<List<Recommendation>> task = () -> {
            List<Recommendation> partialRecommendations = recommendationClient.getRecommendations(snapshot,
                    client,
                    newsFetcher,
                    backupNewsFetcher,
                    marketDataFetcher);
            if (partialRecommendations.isEmpty()) {
                return partialRecommendations;
            }
            logger.info(GENERATION_SUCCESSFUL_INFO, RECOMMENDATION);
            if (overwrite) {
                return recommendationsService.createOrUpdate(partialRecommendations);
            } else {
                recommendationsService.createIgnoringDuplicates(partialRecommendations);
                return partialRecommendations;
            }
        };

        jobs.add(new BatchJob(executor.submit(task), snapshot.stream().map(SymbolPayload::getSymbol).toList()));
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> generateSentiment(List<String> symbolNames, LocalDateTime from, LocalDateTime to)
            throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        logger.info(GENERATION_SUCCESSFUL_INFO, SENTIMENT);
        return newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByName(List<String> symbolNames) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveNews(symbols);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsById(List<Long> symbolIds) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        return retrieveNews(symbols);
    }

    private List<News> retrieveNews(List<Symbol> symbols) throws ClientException, JsonProcessingException {
        List<News> news = retrieveYahooNews(symbols);
        logger.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(news);
        return news;
    }

    private List<News> retrieveYahooNews(List<Symbol> symbols) throws ClientException, JsonProcessingException {
        Map<Long, News> newsByExternalId = new HashMap<>();
        for (Symbol symbol : symbols) {
            List<News> updated = yahooFinanceNewsClient.retrieveNews(symbol);
            for (News news : updated) {
                newsByExternalId
                        .computeIfAbsent(news.getExternalId(), id -> news)
                        .addSymbol(symbol);
            }
        }

        return new ArrayList<>(newsByExternalId.values());
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRangeAndId(List<Long> symbolIds, LocalDate from,
                                                   LocalDate to) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        return retrieveNewsByDateRange(symbols, from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> retrieveNewsByDateRangeAndName(List<String> symbolNames, LocalDate from,
                                                     LocalDate to) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
        return retrieveNewsByDateRange(symbols, from, to);
    }

    private List<News> retrieveNewsByDateRange(List<Symbol> symbols, LocalDate from,
                                               LocalDate to) throws ClientException, JsonProcessingException {
        Map<Long, News> newsByExternalId = new HashMap<>();
        for (Symbol symbol : symbols) {
            List<News> updated = finnhubNewsClient.retrieveNewsByDateRange(symbol, from, to);
            for (News news : updated) {
                newsByExternalId
                        .computeIfAbsent(news.getExternalId(), id -> news)
                        .addSymbol(symbol);
            }
        }
        ArrayList<News> res = new ArrayList<>(newsByExternalId.values());
        logger.info(GENERATION_SUCCESSFUL_INFO, NEWS);
        newsService.createOrUpdate(res);
        return res;
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<MarketData> retrieveMarketData(List<String> symbolNames,
                                               MarketDataType type) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.getOrCreateByName(symbolNames);
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

    public List<PortfolioManager.SymbolStand> getPortfolioStand(List<String> symbolNames, PortfolioType type) {
        List<PortfolioBase> portfolio = getService(type).findActivePortfolio()
                .stream()
                .filter(p -> symbolNames.contains(p.getSymbol().getName()))
                .toList();
        return getStand(portfolio);
    }

    public List<PortfolioManager.SymbolStand> getAllAsPortfolioStandById(List<Long> symbolIds, PortfolioType type,
                                                                         boolean dynamic) throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        Map<Symbol, PortfolioBase> portfolioBySymbol = getService(type)
                .findActivePortfolio().stream()
                .filter(p -> symbols.contains(p.getSymbol()))
                .collect(Collectors.toMap(
                        PortfolioBase::getSymbol,
                        Function.identity()
                ));
        symbols.forEach(s -> portfolioBySymbol
                .computeIfAbsent(s, symbol -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol)));
        return dynamic ? getStandDynamically(portfolioBySymbol.values().stream().toList())
                : getStand(portfolioBySymbol.values().stream().toList());
    }

    public List<PortfolioManager.SymbolStand> getAllAsPortfolioStand(List<String> symbolNames, PortfolioType type) {
        Set<String> namesSet = new LinkedHashSet<>(symbolNames);
        List<Symbol> symbols = symbolService.getOrCreateByName(namesSet);
        Map<Symbol, PortfolioBase> portfolioBySymbol = getService(type)
                .findActivePortfolio().stream()
                .filter(p -> namesSet.contains(p.getSymbol().getName()))
                .collect(Collectors.toMap(
                        PortfolioBase::getSymbol,
                        Function.identity()
                ));
        symbols.forEach(s -> portfolioBySymbol
                .computeIfAbsent(s, symbol -> portfolioTypeToNewPortfolio.get(type).get().setSymbol(symbol)));

        return getStand(portfolioBySymbol.values().stream().toList());
    }

    private List<PortfolioManager.SymbolStand> getStand(List<PortfolioBase> portfolio) {
        return portfolio.stream()
                .flatMap(p -> marketDataService.findLatestBySymbolId(p.getSymbol().getId())
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream())
                .sorted(Comparator.comparing(
                        PortfolioManager.SymbolStand::lastMoveDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    private List<PortfolioManager.SymbolStand> getStandDynamically(List<PortfolioBase> portfolio) throws ClientException, JsonProcessingException {
        Map<String, MarketData> symbolsWithData = typeToRunner.get(MarketDataType.REAL_TIME)
                .apply(portfolio.stream().map(PortfolioBase::getSymbol).toList())
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

    private List<MarketData> retrieveMarketDataWithBackupStrategy(List<Symbol> symbols) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            try {
                res.add(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.LAST).getFirst());
            } catch (ClientException | JsonProcessingException e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, twelveDataMarketDataClient.getClass().getSimpleName(), symbol, e);
                res.add(finnhubMarketDataClient.retrieveMarketData(symbol));
            }
        }
        return res;
    }

    private List<MarketData> retrieveTwelveDataMarketData(List<Symbol> symbols) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            res.addAll(twelveDataMarketDataClient.retrieveMarketData(symbol, MarketDataType.HISTORIC));
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private <T extends PortfolioBase> IPortfolioJpaService<T> getService(PortfolioType type) {
        return (IPortfolioJpaService<T>) portfolioTypeToService.get(type);
    }

    @FunctionalInterface
    private interface TypeToMarketDataFunction {
        List<MarketData> apply(List<Symbol> symbols) throws ClientException, JsonProcessingException;
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(chain = true)
    public static class SymbolPayload {
        private final Symbol symbol;
        private final List<MarketData> marketData;
        private final PortfolioBase portfolio;
        @Setter
        private MarketData premarket;
        @Setter
        private List<News> news;
    }

    private record BatchJob(
            Future<List<Recommendation>> future,
            List<Symbol> symbols
    ) {
    }
}
