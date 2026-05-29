package com.lucas.server;

import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataRepository;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

@Import(TestConfiguration.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class ConfiguredTest extends BaseTest {

    @MockitoSpyBean
    protected MarketDataKpiGenerator kpiGenerator;

    @MockitoSpyBean
    protected MarketDataJpaService marketDataService;

    @MockitoSpyBean
    protected MarketDataRepository marketDataRepository;
}
