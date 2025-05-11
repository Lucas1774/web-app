package com.lucas.server.components.tradingbot.common;

import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailyScheduler {

    private final DataManager dataManager;
    private static final Logger logger = LoggerFactory.getLogger(DailyScheduler.class);

    public DailyScheduler(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Scheduled(cron = "${scheduler.daily-cron}")
    public void dailyTask() {
        List<MarketData> updated;
        try {
            updated = this.dataManager.retrieveMarketData(Constants.SP500_SYMBOLS);
            logger.info(Constants.SCHEDULED_TASK_SUCCESS_INFO, updated);
        } catch (ClientException | JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
