package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataRepository;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Pattern;

import static com.lucas.server.common.Constants.KPI_RETURNED_ZERO_WARN;
import static com.lucas.server.common.Constants.NON_COMPUTABLE_KPI_WARN;
import static com.lucas.server.common.Constants.VOLATILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class MarketDataKpiGeneratorTest extends ConfiguredTest {

    private static final LogCaptor logCaptor = LogCaptor.forClass(MarketDataKpiGenerator.class);
    private static final OrderedIndexedSet<MarketData> mds = OrderedIndexedSet.of(md(10, 9, 11, 9, 4),
            md(13, 11, 14, 12, 1),
            md(12, 10, 13, 11, 3),
            md(11, 12, 12, 10, 2),
            md(14, 13, 15, 13, 0));

    @Autowired
    private SymbolJpaService symbolService;

    @MockitoSpyBean
    private MarketDataJpaService marketDataService;

    @MockitoSpyBean
    private MarketDataRepository marketDataRepository;

    @MockitoSpyBean
    private MarketDataKpiGenerator kpiGenerator;

    @BeforeEach
    void cleanUp() {
        logCaptor.clearLogs();
    }

    @Test
    void whenComputeDerivedFieldsOnCurrentMarketData_thenCorrectlyReflectsKpis() {
        // given
        SymbolDomain symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketDataDomain currentData =
                new MarketDataDomain().setSymbol(symbol).setDate(currentDate).setPrice(BigDecimal.valueOf(150.00));

        MarketDataDomain previousData =
                new MarketDataDomain().setSymbol(symbol).setDate(previousDate).setPrice(BigDecimal.valueOf(140.00));

        // when
        marketDataService.createIgnoringDuplicates(OrderedIndexedSet.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        MarketDataDomain saved =
                marketDataService.findAll().stream().max(Comparator.comparing(MarketDataDomain::getDate)).orElseThrow();
        assertThat(saved.getChange()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(saved.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(140.00));
        assertThat(saved.getChangePercent()).isEqualByComparingTo(BigDecimal.valueOf(7.1429));
        verify(marketDataRepository, atLeastOnce()).findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(symbol.getId(),
                currentDate);
    }

    @Test
    void whenComputeDerivedFieldsWithoutPreviousData_thenNothingHappens() {
        // given
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        SymbolDomain symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

        MarketDataDomain currentData =
                new MarketDataDomain().setSymbol(symbol).setDate(currentDate).setPrice(BigDecimal.valueOf(150.00));

        // when
        marketDataService.createIgnoringDuplicates(OrderedIndexedSet.of(currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        assertThat(currentData.getChange()).isNull();
        assertThat(currentData.getPreviousClose()).isNull();
        assertThat(currentData.getChangePercent()).isNull();
        verify(marketDataRepository, atLeastOnce()).findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(symbol.getId(),
                currentDate);
    }

    @Test
    void whenComputeDerivedFieldsWithZeroPreviousPrice_thenPercentageIsNull() {
        // given
        SymbolDomain symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        symbolService.getOrCreateByName(Set.of(symbol.getName()));
        LocalDate currentDate = LocalDate.of(2023, 12, 15);
        LocalDate previousDate = LocalDate.of(2023, 12, 14);

        MarketDataDomain currentData =
                new MarketDataDomain().setSymbol(symbol).setDate(currentDate).setPrice(BigDecimal.valueOf(150.00));

        MarketDataDomain previousData =
                new MarketDataDomain().setSymbol(symbol).setDate(previousDate).setPrice(BigDecimal.ZERO);

        // when
        marketDataService.createIgnoringDuplicates(OrderedIndexedSet.of(previousData, currentData));
        kpiGenerator.computeDerivedFields(currentData);

        // then
        MarketDataDomain saved =
                marketDataService.findAll().stream().max(Comparator.comparing(MarketDataDomain::getDate)).orElseThrow();
        assertThat(saved.getPreviousClose()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getChange()).isEqualByComparingTo("150.00");
        assertThat(saved.getChangePercent()).isNull();
        verify(marketDataRepository, atLeastOnce()).findTop14BySymbol_IdAndDateBeforeOrderByDateDesc(symbol.getId(),
                currentDate);
    }

    @Test
    void computeMovingAverage() {
        assertThat(kpiGenerator.computeMovingAverage(mds, mds.size())
                .orElseThrow()).isEqualByComparingTo(new BigDecimal("12.0000"));
    }

    @Test
    void computeVolatility() {
        assertThat(kpiGenerator.computeVolatility(mds, mds.size()).orElseThrow()).isEqualByComparingTo(new BigDecimal(
                "160.2067"));
    }

    @Test
    void computeMovingAverage_prizesAreZero() {
        OrderedIndexedSet<MarketData> marketDataList = OrderedIndexedSet.of(md(0, 5, 1, 1, 4),
                md(0, 0, 1, 1, 3),
                md(0, 0, 1, 1, 2),
                md(0, 0, 1, 1, 1),
                md(0, 0, 1, 1, 0));
        assertThat(kpiGenerator.computeMovingAverage(marketDataList, marketDataList.size())
                .orElseThrow()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs()).hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.toString())
                        .contains(KPI_RETURNED_ZERO_WARN.replaceFirst(Pattern.quote("{}"), "moving average")
                                .replaceFirst(Pattern.quote("{}"), marketDataList.toString())));
    }

    @Test
    void computeVolatility_noGainsNoLoses() {
        OrderedIndexedSet<MarketData> marketDataList = OrderedIndexedSet.of(md(4, 4, 5, 4, 0),
                md(4, 4, 4, 3, 1),
                md(4, 4, 7, 2, 2),
                md(4, 4, 4, 1, 3),
                md(4, 4, 9, 4, 4));
        assertThat(kpiGenerator.computeVolatility(marketDataList, marketDataList.size())
                .orElseThrow()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(logCaptor.getLogs()).hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getFirst().toString())
                        .contains(KPI_RETURNED_ZERO_WARN.replaceFirst(Pattern.quote("{}"), VOLATILITY)
                                .replaceFirst(Pattern.quote("{}"), marketDataList.reversed().toString())));
    }

    @Test
    void computeVolatility_notEnoughPreviousClose() {
        OrderedIndexedSet<MarketData> marketDataList = OrderedIndexedSet.of(md(4, 4, 5, 4, 0),
                md(4, 4, 4, 3, 1),
                md(4, 4, 7, 2, 2),
                md(4, 4, 4, 1, 3),
                md(4, null, 9, 4, 4));
        assertThat(kpiGenerator.computeVolatility(marketDataList, marketDataList.size())).isNotPresent();
        assertThat(logCaptor.getLogs()).hasSize(1)
                .allSatisfy(log -> assertThat(log).contains(marketDataList.getFirst().toString())
                        .contains(NON_COMPUTABLE_KPI_WARN.replaceFirst(Pattern.quote("{}"), VOLATILITY)
                                .replaceFirst(Pattern.quote("{}"), marketDataList.reversed().toString())));
    }

    private static MarketData md(Integer price, Integer prevClose, Integer high, Integer low, Integer daysAgo) {
        return new MarketData().setPrice(null != price ? BigDecimal.valueOf(price) : null)
                .setPreviousClose(null != prevClose ? BigDecimal.valueOf(prevClose) : null)
                .setHigh(null != high ? BigDecimal.valueOf(high) : null)
                .setLow(null != low ? BigDecimal.valueOf(low) : null)
                .setDate(LocalDate.now().minusDays(null != daysAgo ? daysAgo : 0));
    }
}
