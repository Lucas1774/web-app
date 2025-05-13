package com.lucas.server.components.tradingbot.common;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.TwelveDataMarketDataClient;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.news.service.FinnhubNewsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDate;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "scheduler.daily-cron=* * * * * *")
@Import(TestcontainersConfiguration.class)
class DailySchedulerTest {

    @MockitoBean
    TwelveDataMarketDataClient marketDataClient;

    @MockitoBean
    FinnhubNewsClient newsClient;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    MarketDataJpaService marketDataService;

    @Autowired
    NewsJpaService newsService;

    @Autowired
    ThreadPoolTaskScheduler scheduler;

    @BeforeEach
    void setup() {
        marketDataService.deleteAll();
        newsService.deleteAll();
        symbolService.deleteAll();
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    void schedulerShouldInvokeClientRepeatedly() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                            verify(marketDataClient, atLeastOnce()).retrieveMarketData(any());
                            verify(newsClient, atLeastOnce()).retrieveNewsByDateRange(any(), eq(from), eq(to));
                        }
                );
    }
}
