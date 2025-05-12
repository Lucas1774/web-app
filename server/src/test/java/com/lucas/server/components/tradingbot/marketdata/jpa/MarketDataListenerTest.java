package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    NewsJpaService newsService;

    @BeforeEach
    void setup() {
        this.jpaService.deleteAll();
        this.newsService.deleteAll();
        this.symbolService.deleteAll();
    }

    @Test
    void whenSaveSomeMarketData_thenItIsUpdatedWithPreviousMarketData() {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        MarketData previous = new MarketData()
                .setSymbol(symbol)
                .setDate(LocalDate.of(2024, 4, 20))
                .setPrice(new BigDecimal("150"));

        MarketData current = new MarketData()
                .setSymbol(symbol)
                .setDate(LocalDate.of(2024, 4, 21))
                .setPrice(new BigDecimal("155"));

        // when
        jpaService.createIgnoringDuplicates(List.of(previous, current));

        // then
        assertThat(current.getPreviousClose()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(current.getChange()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(current.getChangePercent()).isEqualByComparingTo(new BigDecimal("3.3333"));
        verify(kpiGenerator, times(1)).computeDerivedFields(previous);
        verify(kpiGenerator, times(1)).computeDerivedFields(current);
        verify(kpiGenerator, times(2)).computeDerivedFields(any());
    }
}
