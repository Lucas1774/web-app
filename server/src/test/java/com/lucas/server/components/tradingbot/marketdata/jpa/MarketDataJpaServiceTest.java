package com.lucas.server.components.tradingbot.marketdata.jpa;

import com.lucas.server.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
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
    MarketDataJpaService marketDataJpaService;

    @AfterEach
    void tearDown() {
        marketDataJpaService.deleteAll();
    }

    @Test
    void saveAllIgnoringDuplicates_shouldPersistOnlyNewRecords_andHandleTrailingZeros() {
        // given:
        String symbolA = "AAPL";
        String symbolB = "IBM";
        LocalDate date1 = LocalDate.of(2023, 12, 15);
        LocalDate date2 = LocalDate.of(2023, 12, 16);

        MarketData a1 = new MarketData();
        a1.setSymbol(symbolA);
        a1.setDate(date1);
        a1.setPrice(new BigDecimal("150.0000"));

        MarketData a2 = new MarketData();
        a2.setSymbol(symbolA);
        a2.setDate(date2);
        a2.setPrice(new BigDecimal("150.0000"));

        List<MarketData> initialSave = marketDataJpaService.saveAll(List.of(a1, a2));
        assertThat(initialSave)
                .hasSize(2)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactlyInAnyOrder(
                        tuple(symbolA, date1, 150.0),
                        tuple(symbolA, date2, 150.0)
                );

        // when: attempt to save a duplicate and a valid new record
        MarketData duplicate = new MarketData();
        duplicate.setSymbol(symbolA);
        duplicate.setDate(date1);
        duplicate.setPrice(new BigDecimal("160.0000"));

        MarketData valid = new MarketData();
        valid.setSymbol(symbolB);
        valid.setDate(date2);
        valid.setPrice(new BigDecimal("155.0000"));

        List<MarketData> result = marketDataJpaService.saveAllIgnoringDuplicates(List.of(duplicate, valid));

        // then: only the valid new record is returned, compare as double
        assertThat(result)
                .hasSize(1)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactly(tuple(symbolB, date2, 155.0));

        // and: database contains exactly 3 entries, values compared as double
        List<MarketData> all = marketDataJpaService.findAll();
        assertThat(all)
                .hasSize(3)
                .extracting(
                        MarketData::getSymbol,
                        MarketData::getDate,
                        md -> md.getPrice().doubleValue()
                )
                .containsExactlyInAnyOrder(
                        tuple(symbolA, date1, 150.0),
                        tuple(symbolA, date2, 150.0),
                        tuple(symbolB, date2, 155.0)
                );
    }

}
