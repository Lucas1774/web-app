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
        updateMarketData();
        removeOldMarketData();
    }

    @Scheduled(cron = "${scheduler.news-recommendations-cron}", zone = "America/New_York")
    public void morningTask() {
        updateNews();
        getRandomRecommendations();
        LocalDate now = LocalDate.now();
        List<Long> topRecommendedSymbols = new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, NEWS_FINE_GRAIN_THRESHOLD, now));
        updateNewsForTopRecommendedSymbols(topRecommendedSymbols);
        getRecommendations(topRecommendedSymbols, RecommendationMode.NOT_RANDOM);
        getRecommendations(new ArrayList<>(dataManager.getTopRecommendedSymbols(BUY, GROK_FINE_GRAIN_THRESHOLD, now)), RecommendationMode.FINE_GRAIN);
        removeOldNews();
        removeOldRecommendations();
    }

    private void updateMarketData() {
        try {
            List<MarketData> updatedMds = dataManager.retrieveMarketData(SP500_SYMBOLS, MarketDataType.LAST);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched market data", updatedMds);
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void updateNews() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        try {
            List<News> updatedNews = dataManager.retrieveNewsByDateRangeAndName(SP500_SYMBOLS, from, to);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.stream()
                    .map(News::getHeadline).toList());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getRandomRecommendations() {
        List<Recommendation> updatedRecommendations = dataManager.getRandomRecommendations(PortfolioType.REAL,
                SCHEDULED_RECOMMENDATIONS_COUNT, false, true, false, filterClients(clients, RecommendationMode.RANDOM));
        logger.info(SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }

    private void updateNewsForTopRecommendedSymbols(List<Long> topRecommendedSymbols) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        try {
            List<News> updatedNews = dataManager.retrieveNewsByDateRangeAndId(topRecommendedSymbols, from, to);
            logger.info(SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.stream()
                    .map(News::getHeadline).toList());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getRecommendations(List<Long> topRecommendedSymbols, RecommendationMode mode) {
        List<Recommendation> updatedRecommendations = dataManager.getRecommendationsById(topRecommendedSymbols, PortfolioType.REAL,
                true, false, filterClients(clients, mode));
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
}
