package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.MqttPublisher;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.utils.Interrupts;
import com.lucas.utils.exception.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@SuppressWarnings("LoggingSimilarMessage")
@Component
public class DailyScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);
    private final DataManager dataManager;
    private final Map<String, AIClient> clients;
    private final MqttPublisher publisher;

    public DailyScheduler(DataManager dataManager, Map<String, AIClient> clients, MqttPublisher publisher) {
        this.dataManager = dataManager;
        this.clients = clients;
        this.publisher = publisher;
    }

    @Scheduled(cron = "${scheduler.market-data-cron}", zone = "UTC")
    public void midnightTask() {
        if (shouldRun(LocalDate.now().minusDays(1))) {
            doMidnightTask(SP500_SYMBOLS);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void doMidnightTask(List<String> symbolNames) {
        try {
            List<MarketData> updatedMds = dataManager.retrieveMarketData(symbolNames, MarketDataType.LAST);
            String message = String.format("fetched %d market data", updatedMds.size());
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, message, updatedMds);
        } catch (ClientException | MappingException e) {
            logger.error(e.getMessage(), e);
        }

        List<MarketData> removedMds = dataManager.removeOldMarketData(DATABASE_MARKET_DATA_PER_SYMBOL);
        String message = String.format("removed %d market data", removedMds.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message, removedMds);
    }

    @Scheduled(cron = "${scheduler.news-recommendations-cron}", zone = "America/New_York")
    public void morningTask() {
        LocalDate nowEt = ZonedDateTime.now(NY_ZONE).toLocalDate();
        if (shouldRun(nowEt)) {
            if (isTradingDate(nowEt.minusDays(1))) {
                sleep();
            }
            doMorningTask(SP500_SYMBOLS);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void doMorningTask(List<String> symbolNames) {
        List<Recommendation> updatedRecommendations = dataManager.getRandomRecommendations(
                symbolNames, filterClients(clients, RecommendationMode.RANDOM), PortfolioType.REAL,
                SCHEDULED_RECOMMENDATIONS_COUNT, true, true, true, false, false
        );
        String message = String.format("generated %d recommendations", updatedRecommendations.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message, updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());

        LocalDate now = LocalDate.now();
        List<Long> topRecommendedSymbols = dataManager.getTopRecommendedSymbols(BUY, RECOMMENDATION_MEDIUM_GRAIN_THRESHOLD, now);
        getRecommendations(topRecommendedSymbols, RecommendationMode.NOT_RANDOM);
        List<Long> topRecommendedSymbolsAfterMediumGrain = dataManager.getTopRecommendedSymbols(BUY, RECOMMENDATION_FINE_GRAIN_THRESHOLD, now)
                .stream().limit(MAX_RECOMMENDATIONS_COUNT).toList();
        getRecommendations(topRecommendedSymbolsAfterMediumGrain, RecommendationMode.FINE_GRAIN);
        publisher.publish("jobs", "job done");

        List<News> removedNews = dataManager.removeOldNews(DATABASE_NEWS_PER_SYMBOL);
        String message2 = String.format("removed %d news", removedNews.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message2, removedNews);

        List<Recommendation> removedRecommendations = dataManager.removeOldRecommendations(DATABASE_RECOMMENDATIONS_PER_SYMBOL);
        String message3 = String.format("removed %d recommendations", removedRecommendations.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message3, removedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    private void getRecommendations(List<Long> topRecommendedSymbols, RecommendationMode mode) {
        List<Recommendation> updatedRecommendations = dataManager.getRecommendationsById(topRecommendedSymbols, filterClients(clients, mode), PortfolioType.REAL,
                true, true, false, true);
        String message = String.format("generated %d recommendations", updatedRecommendations.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message, updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    @Scheduled(cron = "${scheduler.recommendation-inference-cron}", zone = "America/New_York")
    public void earlyMorningTask() {
        LocalDate nowEt = ZonedDateTime.now(NY_ZONE).toLocalDate();
        if (shouldRun(nowEt)) {
            doEarlyMorningTask(SP500_SYMBOLS);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void doEarlyMorningTask(List<String> symbolNames) {
        List<MarketSnapshot> updatedSnapshots = dataManager.retrieveSnapshotsByName(symbolNames);
        String message = String.format("fetched %d market snapshots", updatedSnapshots.size());
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, message, updatedSnapshots);
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
        Interrupts.runOrSwallow(() -> Thread.sleep(120_000), e -> logger.error(e.getMessage(), e));
    }
}
