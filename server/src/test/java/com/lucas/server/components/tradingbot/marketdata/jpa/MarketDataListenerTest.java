package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataListenerTest {

    @MockitoSpyBean
    MarketDataKpiGenerator kpiGenerator;

    @Autowired
    MarketDataJpaService jpaService;

    @AfterEach
    void tearDown() {
        this.jpaService.deleteAll();
    }

    @Test
    void whenSaveSomeMarketData_thenItIsUpdatedWithPreviousMarketData() {
        // given
        MarketData previous = new MarketData("AAPL", null, null, null,
                new BigDecimal("150"), null, LocalDate.of(2024, 4, 20),
                null, null, null);

        MarketData current = new MarketData("AAPL", null, null, null,
                new BigDecimal("155"), null, LocalDate.of(2024, 4, 21),
                null, null, null);

        // When
        jpaService.save(previous);
        jpaService.save(current);

        // then
        assertThat(current.getPreviousClose()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(current.getChange()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(current.getChangePercent()).isEqualTo("3.33%");
        verify(kpiGenerator, times(1)).computeDerivedFields(previous);
        verify(kpiGenerator, times(1)).computeDerivedFields(current);
        verify(kpiGenerator, times(2)).computeDerivedFields(any());
    }
}
