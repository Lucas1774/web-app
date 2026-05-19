package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MarketDataJpaServiceTest extends ConfiguredTest {

    @MockitoSpyBean
    private MarketDataKpiGenerator kpiGenerator;

    @Autowired
    private MarketDataJpaService jpaService;

    @Autowired
    private SymbolJpaService symbolService;

    @Test
    void createIgnoringDuplicates_shouldPersistOnlyNewRecords_andHandleTrailingZeros() {
        // given:
        SymbolDomain symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        SymbolDomain symbol2 = symbolService.getOrCreateByName(Set.of("IBM")).stream().findFirst().orElseThrow();
        LocalDate date1 = LocalDate.of(2023, 12, 15);
        LocalDate date2 = LocalDate.of(2023, 12, 16);

        MarketDataDomain a1 =
                new MarketDataDomain().setSymbol(symbol).setDate(date1).setPrice(new BigDecimal("150.0000"));

        MarketDataDomain a2 =
                new MarketDataDomain().setSymbol(symbol).setDate(date2).setPrice(new BigDecimal("150.0000"));

        Set<MarketDataDomain> initialSave = jpaService.createIgnoringDuplicates(OrderedIndexedSet.of(a1, a2));
        assertThat(initialSave).hasSize(2)
                .extracting(MarketDataDomain::getSymbol, MarketDataDomain::getDate, md -> md.getPrice().doubleValue())
                .containsExactlyInAnyOrder(tuple(symbol, date1, 150.0), tuple(symbol, date2, 150.0));

        // when: attempt to save a duplicate and a valid new record
        MarketDataDomain duplicate =
                new MarketDataDomain().setSymbol(symbol).setDate(date1).setPrice(new BigDecimal("160.0000"));

        MarketDataDomain valid =
                new MarketDataDomain().setSymbol(symbol2).setDate(date2).setPrice(new BigDecimal("155.0000"));

        Set<MarketDataDomain> result = jpaService.createIgnoringDuplicates(OrderedIndexedSet.of(duplicate, valid));

        // then: only the valid new record is returned, compare as double
        assertThat(result).hasSize(1)
                .extracting(MarketDataDomain::getSymbol, MarketDataDomain::getDate, md -> md.getPrice().doubleValue())
                .containsExactly(tuple(symbol2, date2, 155.0));

        // and: database contains exactly 3 entries, values compared as double
        Set<MarketDataDomain> all = jpaService.findAll();
        assertThat(all).hasSize(3)
                .extracting(MarketDataDomain::getSymbol, MarketDataDomain::getDate, md -> md.getPrice().doubleValue())
                .containsExactlyInAnyOrder(tuple(symbol, date1, 150.0),
                        tuple(symbol, date2, 150.0),
                        tuple(symbol2, date2, 155.0));
    }

    @Test
    void whenSaveSomeMarketData_thenItIsUpdatedWithPreviousMarketData() {
        // given
        SymbolDomain symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        MarketDataDomain previous = new MarketDataDomain().setSymbol(symbol)
                .setDate(LocalDate.of(2024, 4, 20))
                .setPrice(new BigDecimal("150"));

        MarketDataDomain current = new MarketDataDomain().setSymbol(symbol)
                .setDate(LocalDate.of(2024, 4, 21))
                .setPrice(new BigDecimal("155"));

        // when
        jpaService.createIgnoringDuplicates(OrderedIndexedSet.of(previous, current));

        // then
        MarketDataDomain saved =
                jpaService.findAll().stream().max(Comparator.comparing(MarketDataDomain::getDate)).orElseThrow();
        assertThat(saved.getPreviousClose()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(saved.getChange()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(saved.getChangePercent()).isEqualByComparingTo(new BigDecimal("3.3333"));
        verify(kpiGenerator, times(1)).computeDerivedFields(previous);
        verify(kpiGenerator, times(1)).computeDerivedFields(current);
        verify(kpiGenerator, times(2)).computeDerivedFields(any());
    }
}
