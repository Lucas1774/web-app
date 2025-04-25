package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.ClientException;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

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
        News previous = new News(1L, "AAPL", LocalDateTime.now(),
                "Headline1", null, "url", null, null, null);
        News current = new News(2L, "MSFT", LocalDateTime.now(),
                "Headline2", null, "url", null, null, null);

        // When
        jpaService.save(previous);
        jpaService.save(current);

        // then
        verify(embeddingsClient, times(1)).embed(previous);
        verify(embeddingsClient, times(1)).embed(current);
        verify(embeddingsClient, times(2)).embed(any());
    }
}
