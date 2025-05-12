package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MarketDataJpaServiceTest {

    @Autowired
    MarketDataJpaService jpaService;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    NewsJpaService mewsService;

    @BeforeEach
    void setup() {
        jpaService.deleteAll();
        mewsService.deleteAll();
        symbolService.deleteAll();
    }

    @Test
    void createIgnoringDuplicates_shouldPersistOnlyNewRecords_andHandleTrailingZeros() {
        // given:
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        Symbol symbol2 = symbolService.getOrCreateByName("IBM");
        LocalDate date1 = LocalDate.of(2023, 12, 15);
        LocalDate date2 = LocalDate.of(2023, 12, 16);

        MarketData a1 = new MarketData()
                .setSymbol(symbol)
                .setDate(date1)
                .setPrice(new BigDecimal("150.0000"));

        MarketData a2 = new MarketData()
                .setSymbol(symbol)
                .setDate(date2)
                .setPrice(new BigDecimal("150.0000"));

        List<MarketData> initialSave = jpaService.createIgnoringDuplicates(List.of(a1, a2));
        assertThat(initialSave)
                .hasSize(2)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactlyInAnyOrder(
                        tuple(symbol, date1, 150.0),
                        tuple(symbol, date2, 150.0)
                );

        // when: attempt to save a duplicate and a valid new record
        MarketData duplicate = new MarketData()
                .setSymbol(symbol)
                .setDate(date1)
                .setPrice(new BigDecimal("160.0000"));

        MarketData valid = new MarketData()
                .setSymbol(symbol2)
                .setDate(date2)
                .setPrice(new BigDecimal("155.0000"));

        List<MarketData> result = jpaService.createIgnoringDuplicates(List.of(duplicate, valid));

        // then: only the valid new record is returned, compare as double
        assertThat(result)
                .hasSize(1)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactly(tuple(symbol2, date2, 155.0));

        // and: database contains exactly 3 entries, values compared as double
        List<MarketData> all = jpaService.findAll();
        assertThat(all)
                .hasSize(3)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactlyInAnyOrder(
                        tuple(symbol, date1, 150.0),
                        tuple(symbol, date2, 150.0),
                        tuple(symbol2, date2, 155.0)
                );
    }

    @Test
    void getOrCreate() {
        // given:
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        MarketData md = new MarketData()
                .setSymbol(symbol)
                .setDate(LocalDate.of(2023, 12, 15))
                .setPrice(BigDecimal.valueOf(150.00));

        MarketData dup = new MarketData()
                .setSymbol(symbol)
                .setDate(LocalDate.of(2023, 12, 15))
                .setPrice(BigDecimal.valueOf(200));

        // when:
        jpaService.getOrCreate(md);
        MarketData result = jpaService.getOrCreate(dup);

        // then:
        assertThat(result.getPrice()).isEqualByComparingTo(md.getPrice());
    }
}
