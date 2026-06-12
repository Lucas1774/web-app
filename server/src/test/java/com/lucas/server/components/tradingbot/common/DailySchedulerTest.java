package com.lucas.server.components.tradingbot.common;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import com.lucas.utils.Interrupts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lucas.server.common.Constants.SP500_SYMBOLS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "scheduler.market-data-cron=* * * * * *",
        "scheduler.finnhub-news-cron=* * * * * *",
        "scheduler.news-recommendations-cron=* * * * * *",
        "scheduler.recommendation-inference-five-cron=* * * * * *",
        "scheduler.recommendation-inference-fifteen-cron=* * * * * *"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"prod", "dev"}) // scheduler is not active in other profiles than prod. Dev still overrides prod config
class DailySchedulerTest extends ConfiguredTest {

    @MockitoSpyBean
    private DailyScheduler dailyScheduler;

    @MockitoBean
    private DataManager dataManager;

    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @AfterAll
    static void tearDown(@Autowired ThreadPoolTaskScheduler scheduler) {
        scheduler.shutdown();
    }

    @Test
    @Order(1)
    void schedulerInvokesDataManagerRepeatedly() {
        doReturn(true).when(dailyScheduler).shouldRun(any());
        doNothing().when(dailyScheduler).sleep();
        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            verify(dataManager, times(1)).retrieveMarketData(any(), any(), anyBoolean());
            verify(dataManager, times(1)).getRandomRecommendations(any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    anyInt(),
                    anyBoolean(),
                    anyBoolean(),
                    anyBoolean(),
                    anyBoolean(),
                    anyBoolean());
            verify(dataManager, times(2)).retrieveSnapshotsByName(any());
            verify(dataManager, times(SP500_SYMBOLS.size())).retrieveNewsByDateRangeAndName(anyString(),
                    any(),
                    any(),
                    anyBoolean());
        });
    }

    @Test
    @Order(2)
    void tasksRunConcurrentlyWithThreadPoolSize() throws InterruptedException {
        assertEquals(4, scheduler.getPoolSize(), "Thread pool should be configured with size 4");

        AtomicInteger maxConcurrentTasks = new AtomicInteger(0);
        AtomicInteger currentConcurrentTasks = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            scheduler.execute(() -> {
                int current = currentConcurrentTasks.incrementAndGet();
                maxConcurrentTasks.updateAndGet(max -> Math.max(max, current));
                Interrupts.runOrSwallow(() -> await().timeout(Duration.ofMillis(500)), e -> {
                });
                currentConcurrentTasks.decrementAndGet();
                completionLatch.countDown();
            });
        }

        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All tasks should complete within timeout");
        assertEquals(4,
                maxConcurrentTasks.get(),
                "At least 4 tasks should run concurrently with thread pool size 4, but only " + maxConcurrentTasks.get()
                + " ran concurrently");
    }
}
