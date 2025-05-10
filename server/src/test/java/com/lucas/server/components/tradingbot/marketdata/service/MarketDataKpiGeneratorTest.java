package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataKpiGeneratorTest {

    @MockitoSpyBean
    MarketDataJpaService marketDataJpaService;

    @Autowired
    SymbolJpaService symbolJpaService;

    @MockitoSpyBean
    MarketDataKpiGenerator kpiGenerator;

    private static final LogCaptor logCaptor = LogCaptor.forClass(MarketDataKpiGenerator.class);

    private static final List<MarketData> mds = Arrays.asList(
            md(10, 9, 11, 9, 4),
            md(13, 11, 14, 12, 1),
            md(12, 10, 13, 11, 3),
            md(11, 12, 12, 10, 2),
            md(14, 13, 15, 13, 0)
    );

    private static MarketData md(int price, int prevClose, int high, int low, int daysAgo) {
        return new MarketData()
                .setPrice(BigDecimal.valueOf(price))
                .setPreviousClose(BigDecimal.valueOf(prevClose))
                .setHigh(BigDecimal.valueOf(high))
                .setLow(BigDecimal.valueOf(low))
                .setDate(LocalDate.now().minusDays(daysAgo));
    }

    @AfterEach
    void cleanUp() {
        marketDataJpaService.deleteAll();
        symbolJpaService.deleteAll();
        logCaptor.clearLogs();
    }

    @Test
    void whenComputeDerivedFieldsOnCurrentMarketData_thenCorrectlyReflectsKPIs() {
        // given
        Symbol symbol = new Symbol().setName("AAPL");
        symbolJpaService.save(symbol);
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketData currentData = new MarketData()
                .setSymbol(symbol)
                .setDate(currentDate)
                .setPrice(BigDecimal.valueOf(150.00));

        MarketData previousData = new MarketData()
                .setSymbol(symbol)
                .setDate(previousDate)
                .setPrice(BigDecimal.valueOf(140.00));

        // when
        marketDataJpaService.saveAll(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(140.00));
        assertThat(currentData.getChangePercent()).isEqualTo("7.14%");
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    void computeDerivedFieldsWithoutPreviousData_thenNothingHappens() {
        // given
        Symbol symbol = new Symbol().setName("AAPL");
        symbolJpaService.save(symbol);
        LocalDate currentDate = LocalDate.of(2023, 12, 15);

        MarketData currentData = new MarketData()
                .setSymbol(symbol)
                .setDate(currentDate)
                .setPrice(BigDecimal.valueOf(150.00));

        // when
        marketDataJpaService.saveAll(List.of(currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isNull();
        assertThat(currentData.getPreviousClose()).isNull();
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    void whenComputeDerivedFieldsWithZeroPreviousPrice_thenPercentageIsNull() {
        // given
        Symbol symbol = new Symbol().setName("AAPL");
        symbolJpaService.save(symbol);
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketData currentData = new MarketData()
                .setSymbol(symbol)
                .setDate(currentDate)
                .setPrice(BigDecimal.valueOf(150.00));

        MarketData previousData = new MarketData()
                .setSymbol(symbol)
                .setDate(previousDate)
                .setPrice(BigDecimal.ZERO);

        // when
        marketDataJpaService.saveAll(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(currentData.getChange()).isEqualByComparingTo("150.00");
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataJpaService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    void testComputeMovingAverage() throws IllegalStateException {
        assertThat(kpiGenerator.computeMovingAverage(mds)).isEqualByComparingTo(new BigDecimal("12.0000"));
    }

    @Test
    void testComputeAtr() throws IllegalStateException {
        assertThat(kpiGenerator.computeAtr(mds)).isEqualByComparingTo(new BigDecimal("2.4000"));
    }

    @Test
    void testComputeRsi() throws IllegalStateException {
        assertThat(kpiGenerator.computeRsi(mds)).isEqualByComparingTo(new BigDecimal("85.7143"));
    }

    @Test
    void testComputeVolatility() throws IllegalStateException {
        assertThat(kpiGenerator.computeVolatility(mds)).isEqualByComparingTo(new BigDecimal("160.2067"));
    }

    @Test
    void testComputeMovingAverage_prizesAreZero() throws IllegalStateException {
        List<MarketData> marketDataList = Arrays.asList(
                md(0, 5, 1, 1, 4),
                md(0, 0, 1, 1, 3),
                md(0, 0, 1, 1, 2),
                md(0, 0, 1, 1, 1),
                md(0, 0, 1, 1, 0)
        );
        assertThat(kpiGenerator.computeMovingAverage(marketDataList)).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getLast().toString())
                        .contains(Constants.KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeAtr_totalTrueRangeIsZero() throws IllegalStateException {
        List<MarketData> marketDataList = Arrays.asList(
                md(0, 4, 4, 4, 0),
                md(4, 3, 3, 3, 1),
                md(3, 2, 2, 2, 2),
                md(2, 1, 1, 1, 3),
                md(1, 4, 4, 4, 4)
        );
        assertThat(kpiGenerator.computeAtr(marketDataList)).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getFirst().toString())
                        .contains(Constants.KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeRsi_noGainsNoLoses() throws IllegalStateException {
        List<MarketData> marketDataList = Arrays.asList(
                md(4, 4, 5, 4, 0),
                md(4, 4, 4, 3, 1),
                md(4, 4, 7, 2, 2),
                md(4, 4, 4, 1, 3),
                md(4, 4, 9, 4, 4)
        );
        assertThat(kpiGenerator.computeRsi(marketDataList)).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getFirst().toString())
                        .contains(Constants.KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeVolatility_noGainsNoLoses() throws IllegalStateException {
        List<MarketData> marketDataList = Arrays.asList(
                md(4, 4, 5, 4, 0),
                md(4, 4, 4, 3, 1),
                md(4, 4, 7, 2, 2),
                md(4, 4, 4, 1, 3),
                md(4, 4, 9, 4, 4)
        );
        assertThat(kpiGenerator.computeVolatility(marketDataList)).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getFirst().toString())
                        .contains(Constants.KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeMovingAverage_emptyRange() {
        assertThatThrownBy(() -> kpiGenerator.computeMovingAverage(new ArrayList<>()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "SMA Calculation"));
    }

    @Test
    void testComputeAtr_emptyRange() {
        assertThatThrownBy(() -> kpiGenerator.computeAtr(new ArrayList<>()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "ATR Calculation"));
    }

    @Test
    void testComputeRsi_emptyRange() {
        assertThatThrownBy(() -> kpiGenerator.computeRsi(new ArrayList<>()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "RSI Calculation"));
    }

    @Test
    void testComputeVolatility_emptyRange() {
        assertThatThrownBy(() -> kpiGenerator.computeVolatility(new ArrayList<>()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format(Constants.KPI_EMPTY_DATA_ERROR, "Volatility Calculation"));
    }
}
