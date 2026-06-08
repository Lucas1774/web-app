package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.MqttPublisher;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.utils.Interrupts;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.lucas.server.common.Constants.AMERICA_NY;
import static com.lucas.server.common.Constants.BUY;
import static com.lucas.server.common.Constants.DATABASE_MARKET_DATA_PER_SYMBOL;
import static com.lucas.server.common.Constants.DATABASE_NEWS_PER_SYMBOL;
import static com.lucas.server.common.Constants.DATABASE_RECOMMENDATIONS_PER_SYMBOL;
import static com.lucas.server.common.Constants.MAX_RECOMMENDATIONS_COUNT;
import static com.lucas.server.common.Constants.MarketDataType;
import static com.lucas.server.common.Constants.NY_ZONE;
import static com.lucas.server.common.Constants.PortfolioType;
import static com.lucas.server.common.Constants.RECOMMENDATION_FINE_GRAIN_THRESHOLD;
import static com.lucas.server.common.Constants.RECOMMENDATION_MEDIUM_GRAIN_THRESHOLD;
import static com.lucas.server.common.Constants.RecommendationMode;
import static com.lucas.server.common.Constants.SCHEDULED_RECOMMENDATIONS_COUNT;
import static com.lucas.server.common.Constants.SCHEDULED_TASK_SUCCESS_INFO;
import static com.lucas.server.common.Constants.SP500_SYMBOLS;
import static com.lucas.server.common.Constants.filterClients;
import static com.lucas.server.common.Constants.isTradingDate;
import static com.lucas.server.common.Constants.toPastOrFutureTradeDate;

@SuppressWarnings("LoggingSimilarMessage")
@Profile("prod")
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyScheduler {

    private final DataManager dataManager;
    private final Map<String, AiClient> clients;
    private final MqttPublisher publisher;

    @Scheduled(cron = "${scheduler.market-data-cron}", zone = "UTC")
    public void midnightTask() {
        if (shouldRun(LocalDate.now().minusDays(1))) {
            doMidnightTask(SP500_SYMBOLS);
        }
    }

    /**
     * hack for testing purposes
     *
     * @param date date
     * @return true if the scheduled task should run
     */
    public boolean shouldRun(LocalDate date) {
        return isTradingDate(date);
    }

    /**
     * Abstraction for testing purposes
     */
    public void sleep() {
        Interrupts.runOrSwallow(() -> Thread.sleep(120_000), e -> log.error(e.getMessage(), e));
    }

    @Scheduled(cron = "${scheduler.news-recommendations-cron}", zone = AMERICA_NY)
    private void morningTask() {
        LocalDate nowEt = ZonedDateTime.now(NY_ZONE).toLocalDate();
        if (shouldRun(nowEt)) {
            if (isTradingDate(nowEt.minusDays(1))) {
                sleep();
            }
            doMorningTask(SP500_SYMBOLS);
        }
    }

    @Scheduled(cron = "${scheduler.recommendation-inference-fifteen-cron}", zone = "America/New_York")
    @Scheduled(cron = "${scheduler.recommendation-inference-five-cron}", zone = "America/New_York")
    private void earlyMorningTask() {
        LocalDate nowEt = ZonedDateTime.now(NY_ZONE).toLocalDate();
        if (shouldRun(nowEt)) {
            doEarlyMorningTask(SP500_SYMBOLS);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void doMidnightTask(Set<String> symbolNames) {
        try {
            Set<MarketDataDomain> updatedMds = dataManager.retrieveMarketData(symbolNames, MarketDataType.LAST, true);
            String message = String.format("fetched %d market data", updatedMds.size());
            log.info(SCHEDULED_TASK_SUCCESS_INFO,
                    message,
                    updatedMds.stream().map(MarketDataDomain::getSymbol).toList());
        } catch (ClientException | MappingException e) {
            log.error(e.getMessage(), e);
        }

        Set<MarketDataDomain> removedMds = dataManager.removeOldMarketData(DATABASE_MARKET_DATA_PER_SYMBOL);
        String message = String.format("removed %d market data", removedMds.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO, message, removedMds.stream().map(MarketDataDomain::getSymbol).toList());
    }

    @SuppressWarnings("SameParameterValue")
    private void doMorningTask(Set<String> symbolNames) {
        Set<AiClient> firstIterationClients = filterClients(clients, RecommendationMode.FIRST_ITERATION);
        Set<AiClient> secondIterationClients = filterClients(clients, RecommendationMode.SECOND_ITERATION);
        Set<AiClient> fineGrainClients = filterClients(clients, RecommendationMode.FINE_GRAIN);
        Set<AiClient> backupClients = filterClients(clients, RecommendationMode.BACKUP);
        int cheekyRuns = MAX_RECOMMENDATIONS_COUNT / fineGrainClients.stream()
                .mapToInt(c -> c.getConfig().chunkSize())
                .min()
                .orElseThrow();
        Set<RecommendationDomain> updatedRecommendations = dataManager.getRandomRecommendations(symbolNames,
                firstIterationClients,
                new DataManager.CheekyClients(fineGrainClients, cheekyRuns),
                backupClients,
                PortfolioType.REAL,
                SCHEDULED_RECOMMENDATIONS_COUNT,
                true,
                true,
                true,
                false,
                false);
        String message = String.format("generated %d recommendations", updatedRecommendations.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO,
                message,
                updatedRecommendations.stream().map(RecommendationDomain::getSymbol).toList());

        LocalDate now = LocalDate.now();
        Set<Long> topRecommendedSymbols =
                dataManager.getTopRecommendedSymbols(BUY, RECOMMENDATION_MEDIUM_GRAIN_THRESHOLD, now);
        getRecommendations(topRecommendedSymbols, secondIterationClients, backupClients, false, false);
        OrderedIndexedSet<Long> topRecommendedSymbolsAfterMediumGrain =
                dataManager.getTopRecommendedSymbols(BUY, RECOMMENDATION_FINE_GRAIN_THRESHOLD, now)
                        .stream()
                        .limit(MAX_RECOMMENDATIONS_COUNT)
                        .collect(OrderedIndexedSet.toUnmodifiableOrderedIndexedSet());
        getRecommendations(topRecommendedSymbolsAfterMediumGrain, fineGrainClients, Set.of(), true, true);
        publisher.publish("jobs", "job done");

        Set<NewsDomain> removedNews = dataManager.removeOldNews(DATABASE_NEWS_PER_SYMBOL);
        String message2 = String.format("removed %d news", removedNews.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO, message2, removedNews.stream().map(NewsDomain::getSymbols).toList());

        Set<RecommendationDomain> removedRecommendations =
                dataManager.removeOldRecommendations(DATABASE_RECOMMENDATIONS_PER_SYMBOL);
        String message3 = String.format("removed %d recommendations", removedRecommendations.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO,
                message3,
                removedRecommendations.stream().map(RecommendationDomain::getSymbol).toList());
    }

    private void getRecommendations(Set<Long> topRecommendedSymbols,
                                    Set<AiClient> clients,
                                    Set<AiClient> backupClients,
                                    boolean fetchPremarket,
                                    boolean useOldNews) {
        Set<RecommendationDomain> updatedRecommendations = dataManager.getRecommendationsById(topRecommendedSymbols,
                clients,
                DataManager.CheekyClients.empty(),
                backupClients,
                PortfolioType.REAL,
                true,
                true,
                fetchPremarket,
                useOldNews);
        String message = String.format("generated %d recommendations", updatedRecommendations.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO,
                message,
                updatedRecommendations.stream().map(RecommendationDomain::getSymbol).toList());
    }

    @SuppressWarnings("SameParameterValue")
    private void doEarlyMorningTask(Set<String> symbolNames) {
        Set<MarketSnapshotDomain> updatedSnapshots = dataManager.retrieveSnapshotsByName(symbolNames);
        String message = String.format("fetched %d market snapshots", updatedSnapshots.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO,
                message,
                updatedSnapshots.stream().map(MarketSnapshotDomain::getSymbol).toList());

        Set<MarketSnapshotDomain> removedSnapshots = dataManager.removeOldSnapshots(DATABASE_MARKET_DATA_PER_SYMBOL);
        String message2 = String.format("removed %d market snapshots", removedSnapshots.size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO,
                message2,
                removedSnapshots.stream().map(MarketSnapshotDomain::getSymbol).toList());
    }

    @Scheduled(cron = "${scheduler.finnhub-news-cron}", zone = "America/New_York")
    private void beforeMorningTask() {
        LocalDate nowEt = ZonedDateTime.now(NY_ZONE).toLocalDate();
        if (shouldRun(nowEt)) {
            doBeforeMorningTask(SP500_SYMBOLS);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void doBeforeMorningTask(Set<String> symbolNames) {
        LocalDate to = LocalDate.now();
        LocalDate from = toPastOrFutureTradeDate(to, 1, d -> d.minusDays(1));
        Set<NewsDomain> news = null;
        try {
            news = dataManager.retrieveNewsByDateRangeAndName(symbolNames, from, to, false);
        } catch (ClientException | MappingException e) {
            log.error(e.getMessage(), e);
        }
        String message = String.format("fetched %d news", Objects.requireNonNull(news).size());
        log.info(SCHEDULED_TASK_SUCCESS_INFO, message, news);
    }
}
