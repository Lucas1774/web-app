package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
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

@Component
public class DailyScheduler {

    private final DataManager dataManager;
    private final Map<String, AIClient> clients;
    private final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);

    public DailyScheduler(DataManager dataManager, Map<String, AIClient> clients) {
        this.dataManager = dataManager;
        this.clients = clients;
    }

    @Scheduled(cron = "${scheduler.market-data-cron}", zone = "UTC")
    public void midnightTask() {
        if (shouldRun(LocalDate.now().minusDays(1))) {
            doMidnightTask(SP500_SYMBOLS);
        }
    }

    private void doMidnightTask(List<String> symbolNames) {
        updateMarketData(symbolNames);
        removeOldMarketData();
    }

    @Scheduled(cron = "${scheduler.news-recommendations-cron}", zone = "America/New_York")
    public void morningTask() {
        if (shouldRun(ZonedDateTime.now(NY_ZONE).toLocalDate())) {
            doMorningTask(SP500_SYMBOLS);
        }
    }

    private void doMorningTask(List<String> symbolNames) {
        updateNews(symbolNames);
        getRandomRecommendations(symbolNames);
        LocalDate now = LocalDate.now();
        List<Long> topRecommendedSymbols = new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, NEWS_FINE_GRAIN_THRESHOLD, now));
        updateNewsForTopRecommendedSymbols(topRecommendedSymbols);
        getRecommendations(topRecommendedSymbols, RecommendationMode.NOT_RANDOM);
        getRecommendations(new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, GROK_FINE_GRAIN_THRESHOLD, now)), RecommendationMode.FINE_GRAIN);
        removeOldNews();
        removeOldRecommendations();
    }

    private void updateMarketData(List<String> symbolNames) {
        try {
            List<MarketData> updatedMds = dataManager.retrieveMarketData(symbolNames, MarketDataType.LAST);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched market data", updatedMds);
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void updateNews(List<String> symbolNames) {
        try {
            List<News> updatedNews = dataManager.retrieveNewsByName(symbolNames);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.stream()
                    .map(News::getHeadline).toList());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getRandomRecommendations(List<String> symbolNames) {
        List<Recommendation> updatedRecommendations = dataManager.getRandomRecommendations(symbolNames, PortfolioType.REAL,
                SCHEDULED_RECOMMENDATIONS_COUNT, true, true, true, filterClients(clients, RecommendationMode.RANDOM));
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    private void updateNewsForTopRecommendedSymbols(List<Long> topRecommendedSymbols) {
        try {
            List<News> updatedNews = dataManager.retrieveNewsById(topRecommendedSymbols);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.stream()
                    .map(News::getHeadline).toList());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getRecommendations(List<Long> topRecommendedSymbols, RecommendationMode mode) {
        List<Recommendation> updatedRecommendations = dataManager.getRecommendationsById(topRecommendedSymbols, PortfolioType.REAL,
                true, true, filterClients(clients, mode));
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    private void removeOldNews() {
        List<News> removedNews = dataManager.removeOldNews(DATABASE_NEWS_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed news", removedNews.stream()
                .map(News::getHeadline).toList());
    }

    private void removeOldMarketData() {
        List<MarketData> removedMds = dataManager.removeOldMarketData(DATABASE_MARKET_DATA_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed market data", removedMds);
    }

    private void removeOldRecommendations() {
        List<Recommendation> removedRecommendations = dataManager.removeOldRecommendations(DATABASE_RECOMMENDATIONS_PER_SYMBOL);
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "removed recommendations", removedRecommendations.stream()
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
}
