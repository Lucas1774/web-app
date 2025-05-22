package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.Constants;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
public class DailyScheduler {

    private final DataManager dataManager;
    private final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);

    public DailyScheduler(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Scheduled(cron = "${scheduler.daily-cron}")
    public void dailyTask() {
        updateNews();
        removeOldNews();
        updateMarketData();
        removeOldMarketData();
    }

    @Scheduled(cron = "${scheduler.midnight-cron}")
    public void midnightTask() {
        getRandomRecommendations();
        removeOldRecommendations();
    }

    private void updateMarketData() {
        try {
            List<MarketData> updatedMds = dataManager.retrieveMarketData(Constants.SP500_SYMBOLS, Constants.MarketDataType.LAST);
            logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "fetched market data", updatedMds);
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void updateNews() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        try {
            List<News> updatedNews = dataManager.retrieveNewsByDateRange(Constants.SP500_SYMBOLS, from, to);
            logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "fetched news", updatedNews.stream()
                    .map(News::getHeadline).toList());
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void getRandomRecommendations() {
        try {
            List<Recommendation> updatedRecommendations = dataManager.getRandomRecommendations(Constants.PortfolioType.MOCK, Constants.SCHEDULED_RECOMMENDATIONS_COUNT, false, Constants.RecommendationEngineType.RAW);
            logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "generated recommendations", updatedRecommendations.stream()
                    .map(Recommendation::getSymbol).toList());
        } catch (ClientException | IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void removeOldNews() {
        List<News> removedNews = dataManager.removeOldNews(Constants.DATABASE_NEWS_PER_SYMBOL);
        logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "removed news", removedNews.stream()
                .map(News::getHeadline).toList());
    }

    private void removeOldMarketData() {
        List<MarketData> removedMds = dataManager.removeOldMarketData(Constants.DATABASE_MARKET_DATA_PER_SYMBOL);
        logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "removed market data", removedMds);
    }

    private void removeOldRecommendations() {
        List<Recommendation> removedRecommendations = dataManager.removeOldRecommendations(Constants.DATABASE_RECOMMENDATIONS_PER_SYMBOL);
        logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, "removed recommendations", removedRecommendations.stream()
                .map(Recommendation::getSymbol).toList());
    }
}
