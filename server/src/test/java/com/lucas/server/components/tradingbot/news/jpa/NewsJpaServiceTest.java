package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NewsJpaServiceTest {

    @Autowired
    NewsJpaService newsJpaService;

    @Autowired
    SymbolJpaService symbolJpaService;

    @MockitoBean
    @SuppressWarnings("unused")
    NewsEmbeddingsClient newsEmbeddingsClient;

    @AfterEach
    void tearDown() {
        newsJpaService.deleteAll();
        symbolJpaService.deleteAll();
    }

    @Test
    void saveAll_shouldPersistOnlyNewRecords() {
        // given
        Symbol symbol = new Symbol().setName("AAPL");
        Symbol symbol2 = new Symbol().setName("MSFT");
        symbolJpaService.saveAll(List.of(symbol, symbol2));
        News n1 = new News()
                .setExternalId(1L)
                .setSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1")
                .setSummary("Summary1")
                .setUrl("url1")
                .setSource("src1")
                .setCategory("cat1")
                .setImage("img1");

        News n2 = new News()
                .setExternalId(2L)
                .setSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline2")
                .setSummary("Summary2")
                .setUrl("url2")
                .setSource("src2")
                .setCategory("cat2")
                .setImage("img2");

        // when: initial save
        List<News> initial = newsJpaService.saveAll(List.of(n1, n2));

        // then: both persisted
        assertThat(initial)
                .hasSize(2)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, symbol, "Headline1"),
                        tuple(2L, symbol, "Headline2")
                );

        // when: attempt to save duplicate and a new record
        News dup = new News()
                .setExternalId(1L)
                .setSymbol(symbol)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1-dup")
                .setSummary("Summary-dup")
                .setUrl("url-dup")
                .setSource("src-dup")
                .setCategory("cat-dup")
                .setImage("img-dup");

        News n3 = new News()
                .setExternalId(3L)
                .setSymbol(symbol2)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline3")
                .setSummary("Summary3")
                .setUrl("url3")
                .setSource("src3")
                .setCategory("cat3")
                .setImage("img3");

        List<News> second = newsJpaService.saveAll(List.of(dup, n3));

        // then: only new record returned
        assertThat(second)
                .hasSize(1)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactly(tuple(3L, symbol2, "Headline3"));

        // and: repository contains 3 entries total
        List<News> all = newsJpaService.findAll();
        assertThat(all)
                .hasSize(3)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, symbol, "Headline1"),
                        tuple(2L, symbol, "Headline2"),
                        tuple(3L, symbol2, "Headline3")
                );
    }
}
