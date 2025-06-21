package com.lucas.server.components.tradingbot.common.jpa;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.AIClient;
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

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
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
    private final RecommendationChatCompletionClient recommendationClient;
    private final Map<MarketDataType, TypeToMarketDataFunction> typeToRunner;
    private final Map<PortfolioType, IPortfolioJpaService<?>> portfolioTypeToService;
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    private static final Map<PortfolioType, Supplier<PortfolioBase>> portfolioTypeToNewPortfolio = Map.of(
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

    public record SymbolPayload(
            Symbol symbol,
            List<MarketData> marketData,
            List<News> news,
            PortfolioBase portfolio
    ) {
    }

    private record BatchJob(
            Future<List<Recommendation>> future,
            List<Symbol> symbols
    ) {
    }

    @Transactional
    public List<Recommendation> getRecommendationsById(List<Long> symbolIds, PortfolioType type,
                                                       boolean overwrite, boolean bailout, List<AIClient> clients) {
        List<Symbol> symbols = symbolService.findAllById(symbolIds);
        return getRecommendationsInParallel(symbols, type, overwrite, clients, false, bailout);
    }

    @Transactional
    public List<Recommendation> getRandomRecommendations(PortfolioType type, int count,
                                                         boolean overwrite, boolean onlyIfHasNews, boolean bailout, List<AIClient> clients) {
        Set<Long> already = recommendationsService.findByDateBetween(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
                .map(r -> r.getSymbol().getId())
                .collect(Collectors.toSet());

        Set<Long> candidates = symbolService.findAll().stream()
                .filter(s -> SP500_SYMBOLS.contains(s.getName()))
                .map(Symbol::getId)
                .collect(Collectors.toSet());
        candidates.removeAll(already);

        List<Long> finalList = new ArrayList<>();
        if (count > 0 && !candidates.isEmpty()) {
            List<Long> pool = new ArrayList<>(candidates);
            Collections.shuffle(pool);
            finalList.addAll(pool.subList(0, Math.min(count, pool.size())));
        }

        return getRecommendationsInParallel(symbolService.findAllById(finalList), type, overwrite,
                clients, onlyIfHasNews, bailout);
    }

    private List<Recommendation> getRecommendationsInParallel(List<Symbol> symbols, PortfolioType type, boolean overwrite,
                                                              List<AIClient> clients, boolean onlyIfHasNews, boolean bailout) {
        List<Recommendation> res = new ArrayList<>();
        List<AIClient> mutableClients = new ArrayList<>(clients);
        IPortfolioJpaService<PortfolioBase> portfolioService = getService(type);
        Supplier<PortfolioBase> portfolioSupplier = portfolioTypeToNewPortfolio.get(type);

        List<SymbolPayload> remainingPayload = symbols.stream()
                .map(symbol -> {
                    List<MarketData> marketData = marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT);
                    if (marketData.isEmpty()) {
                        return Optional.<SymbolPayload>empty();
                    }

                    List<News> news = newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT);
                    if (onlyIfHasNews && news.isEmpty()) {
                        return Optional.<SymbolPayload>empty();
                    }

                    PortfolioBase portfolio = portfolioService.findBySymbol(symbol)
                            .orElseGet(() -> portfolioSupplier.get().setSymbol(symbol));

                    return Optional.of(new SymbolPayload(symbol, marketData, news, portfolio));
                })
                .flatMap(Optional::stream)
                .toList();

        logger.info(RETRIEVING_DATA_INFO, RECOMMENDATION, remainingPayload.size());
        try (ExecutorService executor = Executors.newFixedThreadPool(clients.size() * 5)) {
            int retries = 0;
            while (!remainingPayload.isEmpty() && (!bailout || retries < RECOMMENDATION_W_BAILOUT_MAX_RETRIES)) {
                retries++;
                List<Recommendation> fetched = doGetRecommendationsInParallel(executor, remainingPayload,
                        overwrite, mutableClients);
                res.addAll(fetched);
                Set<Symbol> fetchedSymbols = fetched.stream()
                        .map(Recommendation::getSymbol)
                        .collect(Collectors.toSet());
                remainingPayload = remainingPayload.stream()
                        .filter(p -> !fetchedSymbols.contains(p.symbol()))
                        .toList();
            }
        }

        return res;
    }

    private List<Recommendation> doGetRecommendationsInParallel(ExecutorService executor, List<SymbolPayload> payload,
                                                                boolean overwrite, List<AIClient> clients) {
        List<BatchJob> jobs = new ArrayList<>();
        List<SymbolPayload> buffer = new ArrayList<>();

        for (SymbolPayload load : payload) {
            AIClient client = clients.getFirst();
            buffer.add(load);

            if (buffer.size() == client.getChunkSize()) {
                submitChunk(overwrite, client, buffer, jobs, executor);
                buffer.clear();
                Collections.rotate(clients, -1);
            }
        }

        if (!buffer.isEmpty()) {
            AIClient client = clients.getFirst();
            submitChunk(overwrite, client, buffer, jobs, executor);
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

    private void submitChunk(boolean overwrite, AIClient client, List<SymbolPayload> buffer,
                             List<BatchJob> jobs, ExecutorService executor) {
        List<SymbolPayload> snapshot = new ArrayList<>(buffer);
        Callable<List<Recommendation>> task = () -> {
            List<Recommendation> partialRecommendations = recommendationClient.getRecommendations(snapshot, client);
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

        jobs.add(new BatchJob(executor.submit(task), snapshot.stream().map(SymbolPayload::symbol).toList()));
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    public List<News> generateSentiment(List<String> symbolNames, LocalDate from, LocalDate to)
            throws ClientException, JsonProcessingException {
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        logger.info(GENERATION_SUCCESSFUL_INFO, SENTIMENT);
        return newsService.generateSentiment(symbols.stream().map(Symbol::getId).toList(), from, to);
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
        List<Symbol> symbols = symbolNames.stream().distinct().map(symbolService::getOrCreateByName).toList();
        return retrieveNewsByDateRange(symbols, from, to);
    }

    @Transactional(rollbackOn = {ClientException.class, JsonProcessingException.class})
    private List<News> retrieveNewsByDateRange(List<Symbol> symbols, LocalDate from,
                                               LocalDate now) throws ClientException, JsonProcessingException {
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

        return getStand(portfolioBySymbol.values().stream().toList());
    }

    private List<PortfolioManager.SymbolStand> getStand(List<PortfolioBase> portfolio) {
        return portfolio.stream()
                .flatMap(p -> marketDataService.findBySymbolId(p.getSymbol().getId())
                        .stream()
                        .max(Comparator.comparing(MarketData::getDate))
                        .map(md -> portfolioManager.computeStand(p, md))
                        .stream()).sorted(Comparator.comparing(
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
                .flatMap(p -> marketDataService.findBySymbolId(p.getSymbol().getId())
                        .stream()
                        .max(Comparator.comparing(MarketData::getDate))
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

    private List<MarketData> retrieveMarketDataWithBackupStrategy(
            List<Symbol> symbols, TwelveDataMarketDataClient twelveDataMarketDataClient, FinnhubMarketDataClient finnhubMarketDataClient
    ) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            try {
                res.add(twelveDataMarketDataClient.retrieveMarketData(List.of(symbol), MarketDataType.LAST).getFirst());
            } catch (ClientException | JsonProcessingException e) {
                logger.warn(CLIENT_FAILED_BACKUP_WARN, twelveDataMarketDataClient.getClass().getSimpleName(), symbol, e.getMessage());
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
