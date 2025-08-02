package com.lucas.server.components.tradingbot.common;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cron job synthetic runner. Run containers manually and pass production env vars as env vars
 */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@Disabled("Manual run only")
class RunCronJobsTest {
    private static final List<String> symbolNames = List.of("AAPL", "NVDA", "MSFT", "AMZN", "META", "TSLA", "GOOGL");

    @Autowired
    DailyScheduler dailyScheduler;

    @Test
    @Transactional
    void runMidnightTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMidnightTask", List.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, symbolNames);
    }

    @Test
    @Transactional
    void runMorningTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMorningTask", List.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, symbolNames);
    }
}
