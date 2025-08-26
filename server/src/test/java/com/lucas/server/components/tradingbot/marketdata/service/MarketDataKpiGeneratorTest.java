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
import java.util.Set;
import java.util.regex.Pattern;

import static com.lucas.server.common.Constants.KPI_RETURNED_ZERO_WARN;
import static com.lucas.server.common.Constants.NON_COMPUTABLE_KPI_WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataKpiGeneratorTest {

    private static final LogCaptor logCaptor = LogCaptor.forClass(MarketDataKpiGenerator.class);
    private static final List<MarketData> mds = Arrays.asList(
            md(10, 9, 11, 9, 4),
            md(13, 11, 14, 12, 1),
            md(12, 10, 13, 11, 3),
            md(11, 12, 12, 10, 2),
            md(14, 13, 15, 13, 0)
    );

    @MockitoSpyBean
    private MarketDataJpaService marketDataService;

    @Autowired
    private SymbolJpaService symbolService;

    @MockitoSpyBean
    private MarketDataKpiGenerator kpiGenerator;

    private static MarketData md(Integer price, Integer prevClose, Integer high, Integer low, Integer daysAgo) {
        return new MarketData()
                .setPrice(price != null ? BigDecimal.valueOf(price) : null)
                .setPreviousClose(prevClose != null ? BigDecimal.valueOf(prevClose) : null)
                .setHigh(high != null ? BigDecimal.valueOf(high) : null)
                .setLow(low != null ? BigDecimal.valueOf(low) : null)
                .setDate(LocalDate.now().minusDays(daysAgo != null ? daysAgo : 0));
    }

    @BeforeEach
    void cleanUp() {
        logCaptor.clearLogs();
    }

    @Test
    @Transactional
    void whenComputeDerivedFieldsOnCurrentMarketData_thenCorrectlyReflectsKPIs() {
        // given
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
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
        verify(marketDataService, atLeastOnce()).findTop14BySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    @Transactional
    void computeDerivedFieldsWithoutPreviousData_thenNothingHappens() {
        // given
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

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
        verify(marketDataService, atLeastOnce()).findTop14BySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    @Transactional
    void whenComputeDerivedFieldsWithZeroPreviousPrice_thenPercentageIsNull() {
        // given
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        symbolService.getOrCreateByName(Set.of(symbol.getName()));
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
        verify(marketDataService, atLeastOnce()).findTop14BySymbolIdAndDateBeforeOrderByDateDesc(symbol.getId(), currentDate);
    }

    @Test
    void testComputeMovingAverage() {
        assertThat(kpiGenerator.computeMovingAverage(mds, mds.size()).orElseThrow()).isEqualByComparingTo(new BigDecimal("12.0000"));
    }

    @Test
    void testComputeVolatility() {
        assertThat(kpiGenerator.computeVolatility(mds, mds.size()).orElseThrow()).isEqualByComparingTo(new BigDecimal("160.2067"));
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
        assertThat(kpiGenerator.computeMovingAverage(marketDataList, marketDataList.size()).orElseThrow()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log)
                        .contains(marketDataList.toString())
                        .contains(KPI_RETURNED_ZERO_WARN.replaceFirst(Pattern.quote("{}"), "moving average")
                                .replaceFirst(Pattern.quote("{}"), marketDataList.toString())));
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
        assertThat(kpiGenerator.computeVolatility(marketDataList, marketDataList.size()).orElseThrow()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log)
                        .contains(marketDataList.getFirst().toString())
                        .contains(KPI_RETURNED_ZERO_WARN.replaceFirst(Pattern.quote("{}"), "volatility")
                                .replaceFirst(Pattern.quote("{}"), marketDataList.reversed().toString())));
    }

    @Test
    void testComputeVolatility_notEnoughPreviousClose() {
        List<MarketData> marketDataList = Arrays.asList(
                md(4, 4, 5, 4, 0),
                md(4, 4, 4, 3, 1),
                md(4, 4, 7, 2, 2),
                md(4, 4, 4, 1, 3),
                md(4, null, 9, 4, 4)
        );
        assertThat(kpiGenerator.computeVolatility(marketDataList, marketDataList.size())).isNotPresent();
        assertThat(logCaptor.getLogs())
                .hasSize(1)
                .allSatisfy(log -> assertThat(log)
                        .contains(marketDataList.getFirst().toString())
                        .contains(NON_COMPUTABLE_KPI_WARN.replaceFirst(Pattern.quote("{}"), "volatility")
                                .replaceFirst(Pattern.quote("{}"), marketDataList.reversed().toString())));
    }
}
