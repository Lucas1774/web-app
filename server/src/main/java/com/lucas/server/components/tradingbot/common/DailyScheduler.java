package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.MqttPublisher;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lucas.server.common.Constants.*;

@SuppressWarnings("LoggingSimilarMessage")
@Component
public class DailyScheduler {

    private final DataManager dataManager;
    private final Map<String, AIClient> clients;
    private final MqttPublisher publisher;
    private final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);

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
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched market data", updatedMds);
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        List<MarketData> removedMds = dataManager.removeOldMarketData(DATABASE_MARKET_DATA_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed market data", removedMds);
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
        try {
            List<News> updatedNews = dataManager.retrieveNewsByName(symbolNames);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.size());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        List<Recommendation> updatedRecommendations = dataManager.getRandomRecommendations(
                symbolNames, filterClients(clients, RecommendationMode.RANDOM), PortfolioType.REAL,
                SCHEDULED_RECOMMENDATIONS_COUNT, true, true, false, false, false
        );
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());

        LocalDate now = LocalDate.now();
        List<Long> topRecommendedSymbols = new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, NEWS_FINE_GRAIN_THRESHOLD, now));
        getRecommendations(topRecommendedSymbols, RecommendationMode.NOT_RANDOM);
        getRecommendations(new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, GROK_FINE_GRAIN_THRESHOLD, now)
                .stream().limit(MAX_RECOMMENDATIONS_COUNT).toList()), RecommendationMode.FINE_GRAIN);
        publisher.publish("jobs", "job done");

        List<News> removedNews = dataManager.removeOldNews(DATABASE_NEWS_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed news", removedNews);

        List<Recommendation> removedRecommendations = dataManager.removeOldRecommendations(DATABASE_RECOMMENDATIONS_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed recommendations", removedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    private void getRecommendations(List<Long> topRecommendedSymbols, RecommendationMode mode) {
        List<Recommendation> updatedRecommendations = dataManager.getRecommendationsById(topRecommendedSymbols, filterClients(clients, mode), PortfolioType.REAL,
                true, true, false, false);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
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
        Utils.sleep(120_000, e -> logger.error(e.getMessage(), e));
    }
}
