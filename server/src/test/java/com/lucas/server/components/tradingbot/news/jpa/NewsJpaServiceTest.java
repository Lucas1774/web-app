package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NewsJpaServiceTest {

    @Autowired
    NewsJpaService jpaService;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    MarketDataJpaService marketDataService;

    @BeforeEach
    void setup() {
        jpaService.deleteAll();
        marketDataService.deleteAll();
        symbolService.deleteAll();
    }

    @Test
    void createIgnoringDuplicates_shouldPersistOnlyNewRecords() {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        Symbol symbol2 = symbolService.getOrCreateByName("MSFT");
        News n1 = new News()
                .setExternalId(1L)
                .addSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1")
                .setSummary("Summary1")
                .setUrl("url1")
                .setSource("src1")
                .setCategory("cat1")
                .setImage("img1");

        News n2 = new News()
                .setExternalId(2L)
                .addSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline2")
                .setSummary("Summary2")
                .setUrl("url2")
                .setSource("src2")
                .setCategory("cat2")
                .setImage("img2");

        // when: initial save
        List<News> initial = jpaService.createIgnoringDuplicates(List.of(n1, n2), false);

        // then: both persisted
        assertThat(initial)
                .hasSize(2)
                .extracting(News::getExternalId, News::getSymbols, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, Set.of(symbol), "Headline1"),
                        tuple(2L, Set.of(symbol), "Headline2")
                );

        List<Symbol> symbols = symbolService.findAll();
        assertThat(symbols.getFirst().getNews())
                .hasSize(2)
                .extracting(News::getExternalId, News::getSymbols, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, Set.of(symbol), "Headline1"),
                        tuple(2L, Set.of(symbol), "Headline2")
                );
        assertThat(symbols.getLast().getNews())
                .isEmpty();

        // when: attempt to save duplicate and a new record
        News dup = new News()
                .setExternalId(1L)
                .addSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1-dup")
                .setSummary("Summary-dup")
                .setUrl("url-dup")
                .setSource("src-dup")
                .setCategory("cat-dup")
                .setImage("img-dup");

        News n3 = new News()
                .setExternalId(3L)
                .addSymbol(symbol2)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline3")
                .setSummary("Summary3")
                .setUrl("url3")
                .setSource("src3")
                .setCategory("cat3")
                .setImage("img3");

        List<News> second = jpaService.createIgnoringDuplicates(List.of(dup, n3), false);

        // then: only new record returned
        assertThat(second)
                .hasSize(1)
                .extracting(News::getExternalId, News::getSymbols, News::getHeadline)
                .containsExactly(tuple(3L, Set.of(symbol2), "Headline3"));

        // and: repository contains 3 entries total
        List<News> all = jpaService.findAll();
        assertThat(all)
                .hasSize(3)
                .extracting(News::getExternalId, News::getSymbols, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, Set.of(symbol), "Headline1"),
                        tuple(2L, Set.of(symbol), "Headline2"),
                        tuple(3L, Set.of(symbol2), "Headline3")
                );
    }
}
