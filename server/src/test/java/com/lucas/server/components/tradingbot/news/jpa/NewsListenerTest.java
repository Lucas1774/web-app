package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NewsListenerTest {

    @MockitoBean
    NewsEmbeddingsClient embeddingsClient;

    @Autowired
    NewsJpaService jpaService;

    @AfterEach
    void tearDown() {
        this.jpaService.deleteAll();
    }

    @Test
    void whenSaveSomeMarketData_thenItIsUpdatedWithPreviousMarketData() throws ClientException {
        // given
        News previous = new News();
        previous.setSymbol("AAPL");
        previous.setExternalId(1L);
        previous.setDate(LocalDateTime.now());
        previous.setHeadline("Headline1");
        previous.setUrl("url");

        News current = new News();
        current.setSymbol("MSFT");
        current.setExternalId(2L);
        current.setDate(LocalDateTime.now());
        current.setHeadline("Headline2");
        current.setUrl("url");

        // when
        jpaService.saveAll(List.of(previous, current));

        // then
        verify(embeddingsClient, times(1)).embed(previous);
        verify(embeddingsClient, times(1)).embed(current);
        verify(embeddingsClient, times(2)).embed(any());
    }
}
