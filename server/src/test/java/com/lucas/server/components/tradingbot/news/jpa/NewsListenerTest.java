package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    SymbolJpaService symbolService;

    @BeforeEach
    void setup() {
        this.jpaService.deleteAll();
        this.symbolService.deleteAll();
    }

    @Test
    void whenSaveSomeNewsWithCallback_thenItIsUpdatedWithPreviousNewsData() throws ClientException {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        News previous = new News()
                .setSymbol(symbol)
                .setExternalId(1L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1")
                .setUrl("url");

        Symbol symbol2 = symbolService.getOrCreateByName("MSFT");
        News current = new News()
                .setSymbol(symbol2)
                .setExternalId(2L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline2")
                .setUrl("url");

        // when
        jpaService.saveAll(List.of(previous, current), true);

        // then
        verify(embeddingsClient, times(1)).embed(previous);
        verify(embeddingsClient, times(1)).embed(current);
        verify(embeddingsClient, times(2)).embed(any());
    }

    @Test
    void whenSaveSomeNewsNoCallback_thenItIsNotUpdatedWithPreviousNewsData() throws ClientException {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        News previous = new News()
                .setSymbol(symbol)
                .setExternalId(1L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1")
                .setUrl("url");

        Symbol symbol2 = symbolService.getOrCreateByName("MSFT");
        News current = new News()
                .setSymbol(symbol2)
                .setExternalId(2L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline2")
                .setUrl("url");

        // when
        jpaService.saveAll(List.of(previous, current), false);

        // then
        verify(embeddingsClient, times(0)).embed(previous);
        verify(embeddingsClient, times(0)).embed(current);
        verify(embeddingsClient, times(0)).embed(any());
    }
}
