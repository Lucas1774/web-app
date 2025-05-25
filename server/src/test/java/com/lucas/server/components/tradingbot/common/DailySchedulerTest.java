package com.lucas.server.components.tradingbot.common;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
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
    DataManager dataManager;

    @Autowired
    ThreadPoolTaskScheduler scheduler;

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    @Transactional
    void schedulerShouldInvokeClientRepeatedly() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(1);
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                            verify(dataManager, atLeastOnce()).retrieveMarketData(any(), any());
                            verify(dataManager, atLeastOnce()).retrieveNewsByDateRange(any(), eq(from), eq(to), eq(false));
                        }
                );
    }
}
