package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import jakarta.transaction.Transactional;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.lucas.server.common.Constants.KPI_RETURNED_ZERO_WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataKpiGeneratorTest {

    @MockitoSpyBean
    MarketDataJpaService marketDataService;

    @Autowired
    SymbolJpaService symbolService;

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

    @BeforeEach
    void cleanUp() {
        logCaptor.clearLogs();
    }

    @Test
    @Transactional
    void whenComputeDerivedFieldsOnCurrentMarketData_thenCorrectlyReflectsKPIs() {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
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
        marketDataService.createIgnoringDuplicates(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(140.00));
        assertThat(currentData.getChangePercent()).isEqualByComparingTo(BigDecimal.valueOf(7.1429));
        verify(marketDataService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    @Transactional
    void computeDerivedFieldsWithoutPreviousData_thenNothingHappens() {
        // given
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        Symbol symbol = symbolService.getOrCreateByName("AAPL");

        MarketData currentData = new MarketData()
                .setSymbol(symbol)
                .setDate(currentDate)
                .setPrice(BigDecimal.valueOf(150.00));

        // when
        marketDataService.createIgnoringDuplicates(List.of(currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isNull();
        assertThat(currentData.getPreviousClose()).isNull();
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    @Transactional
    void whenComputeDerivedFieldsWithZeroPreviousPrice_thenPercentageIsNull() {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        symbolService.getOrCreateByName(symbol.getName());
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
        marketDataService.createIgnoringDuplicates(List.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getPreviousClose()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(currentData.getChange()).isEqualByComparingTo("150.00");
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataService, atLeastOnce()).findTopBySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    void testComputeMovingAverage() {
        assertThat(kpiGenerator.computeMovingAverage(mds)).isEqualByComparingTo(new BigDecimal("12.0000"));
    }

    @Test
    void testComputeAtr() {
        assertThat(kpiGenerator.computeAtr(mds)).isEqualByComparingTo(new BigDecimal("2.4000"));
    }

    @Test
    void testComputeRsi() {
        assertThat(kpiGenerator.computeRsi(mds)).isEqualByComparingTo(new BigDecimal("85.7143"));
    }

    @Test
    void testComputeVolatility() {
        assertThat(kpiGenerator.computeVolatility(mds)).isEqualByComparingTo(new BigDecimal("160.2067"));
    }

    @Test
    void testComputeMovingAverage_prizesAreZero() {
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
                        .contains(KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeAtr_totalTrueRangeIsZero() {
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
                        .contains(KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeRsi_noGainsNoLoses() {
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
                        .contains(KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }

    @Test
    void testComputeVolatility_noGainsNoLoses() {
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
                        .contains(KPI_RETURNED_ZERO_WARN.replace("{}", "")));
    }
}
