package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataKpiGeneratorTest {

    @Autowired
    @MockitoSpyBean
    MarketDataJpaService marketDataJpaService;

    @Autowired
    MarketDataKpiGenerator kpiGenerator;

    @AfterEach
    void cleanUp() {
        marketDataJpaService.deleteAll();
    }

    @Test
    void whenComputeDerivedFieldsOnCurrentMarketData_thenCorrectlyReflectsKPIs() {
        // given
        String symbol = "AAPL";
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketData currentData = new MarketData();
        currentData.setSymbol(symbol);
        currentData.setDate(currentDate);
        currentData.setPrice(BigDecimal.valueOf(150.00));

        MarketData previousData = new MarketData();
        previousData.setSymbol(symbol);
        previousData.setDate(previousDate);
        previousData.setPrice(BigDecimal.valueOf(140.00));

        // when
        marketDataJpaService.saveAll(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(140.00));
        assertThat(currentData.getChangePercent()).isEqualTo("7.14%");
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, currentDate);
    }

    @Test
    void computeDerivedFieldsWithoutPreviousData_thenNothingHappens() {
        // given
        String symbol = "AAPL";
        LocalDate currentDate = LocalDate.of(2023, 12, 15);

        MarketData currentData = new MarketData();
        currentData.setSymbol(symbol);
        currentData.setDate(currentDate);
        currentData.setPrice(BigDecimal.valueOf(150.00));

        // when
        marketDataJpaService.saveAll(List.of(currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isNull();
        assertThat(currentData.getPreviousClose()).isNull();
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, currentDate);
    }

    @Test
    void whenComputeDerivedFieldsWithZeroPreviousPrice_thenPercentageIsNull() {
        // given
        String symbol = "AAPL";
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketData currentData = new MarketData();
        currentData.setSymbol(symbol);
        currentData.setDate(currentDate);
        currentData.setPrice(BigDecimal.valueOf(150.00));

        MarketData previousData = new MarketData();
        previousData.setSymbol(symbol);
        previousData.setDate(previousDate);
        previousData.setPrice(BigDecimal.ZERO);

        // when
        marketDataJpaService.saveAll(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(currentData.getChange()).isEqualByComparingTo("150.00");
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolAndDateBeforeOrderByDateDesc(symbol, currentDate);
    }
}
