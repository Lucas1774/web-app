package com.lucas.server.components.tradingbot.common;

import com.lucas.server.TestConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "scheduler.market-data-cron=* * * * * *",
        "scheduler.news-recommendations-cron=* * * * * *",
        "scheduler.recommendation-inference-five-cron=* * * * * *",
        "scheduler.recommendation-inference-fifteen-cron=* * * * * *"
})
@Import(TestConfiguration.class)
class DailySchedulerTest {

    @MockitoSpyBean
    private DailyScheduler dailyScheduler;

    @MockitoBean
    private DataManager dataManager;

    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @Transactional
    void schedulerShouldInvokeClientRepeatedly() {
        doReturn(true).when(dailyScheduler).shouldRun(any());
        doNothing().when(dailyScheduler).sleep();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(dataManager, atLeastOnce()).retrieveMarketData(any(), any());
                    verify(dataManager, atLeastOnce()).getRandomRecommendations(
                            any(), any(), any(), anyInt(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()
                    );
                    verify(dataManager, times(2)).retrieveSnapshotsByName(any());
                });
    }
}
