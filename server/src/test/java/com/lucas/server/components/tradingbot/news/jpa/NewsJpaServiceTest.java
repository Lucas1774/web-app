package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
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

    @MockitoBean
    @SuppressWarnings("unused")
    NewsEmbeddingsClient newsEmbeddingsClient;

    @AfterEach
    void tearDown() {
        newsJpaService.deleteAll();
    }

    @Test
    void saveAll_shouldPersistOnlyNewRecords() {
        // given
        News n1 = new News();
        n1.setExternalId(1L);
        n1.setSymbol("AAPL");
        n1.setDate(LocalDateTime.now());
        n1.setHeadline("Headline1");
        n1.setSummary("Summary1");
        n1.setUrl("url1");
        n1.setSource("src1");
        n1.setCategory("cat1");
        n1.setImage("img1");

        News n2 = new News();
        n2.setExternalId(2L);
        n2.setSymbol("AAPL");
        n2.setDate(LocalDateTime.now());
        n2.setHeadline("Headline2");
        n2.setSummary("Summary2");
        n2.setUrl("url2");
        n2.setSource("src2");
        n2.setCategory("cat2");
        n2.setImage("img2");

        // when: initial save
        List<News> initial = newsJpaService.saveAll(List.of(n1, n2));

        // then: both persisted
        assertThat(initial)
                .hasSize(2)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, "AAPL", "Headline1"),
                        tuple(2L, "AAPL", "Headline2")
                );

        // when: attempt to save duplicate and a new record
        News dup = new News();
        dup.setExternalId(1L);
        dup.setSymbol("AAPL");
        dup.setDate(LocalDateTime.now());
        dup.setHeadline("Headline1-dup");
        dup.setSummary("Summary-dup");
        dup.setUrl("url-dup");
        dup.setSource("src-dup");
        dup.setCategory("cat-dup");
        dup.setImage("img-dup");

        News n3 = new News();
        n3.setExternalId(3L);
        n3.setSymbol("MSFT");
        n3.setDate(LocalDateTime.now());
        n3.setHeadline("Headline3");
        n3.setSummary("Summary3");
        n3.setUrl("url3");
        n3.setSource("src3");
        n3.setCategory("cat3");
        n3.setImage("img3");

        List<News> second = newsJpaService.saveAll(List.of(dup, n3));

        // then: only new record returned
        assertThat(second)
                .hasSize(1)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactly(tuple(3L, "MSFT", "Headline3"));

        // and: repository contains 3 entries total
        List<News> all = newsJpaService.findAll();
        assertThat(all)
                .hasSize(3)
                .extracting(News::getExternalId, News::getSymbol, News::getHeadline)
                .containsExactlyInAnyOrder(
                        tuple(1L, "AAPL", "Headline1"),
                        tuple(2L, "AAPL", "Headline2"),
                        tuple(3L, "MSFT", "Headline3")
                );
    }
}
