package com.lucas.server.components.tradingbot.common;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.marketdata.service.FinnhubMarketDataClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "scheduler.daily-cron=* * * * * *")
@Import(TestcontainersConfiguration.class)
class DailySchedulerTest {

    @MockitoBean
    FinnhubMarketDataClient marketDataClient;

    @Test
    void schedulerShouldInvokeClientRepeatedly() {
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() ->
                        verify(marketDataClient, atLeastOnce()).retrieveMarketData(anyList())
                );
    }
}
