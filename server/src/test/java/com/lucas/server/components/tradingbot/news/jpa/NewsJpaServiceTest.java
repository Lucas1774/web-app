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
        News n1 = new News(1L, "AAPL", LocalDateTime.now(),
                "Headline1", "Summary1", "url1", "src1", "cat1", "img1");
        News n2 = new News(2L, "AAPL", LocalDateTime.now(),
                "Headline2", "Summary2", "url2", "src2", "cat2", "img2");

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
        News dup = new News(1L, "AAPL", LocalDateTime.now(),
                "Headline1-dup", "Summary-dup", "url-dup", "src-dup", "cat-dup", "img-dup");
        News n3 = new News(3L, "MSFT", LocalDateTime.now(),
                "Headline3", "Summary3", "url3", "src3", "cat3", "img3");

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
