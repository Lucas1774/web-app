package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.ClientException;
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
    SymbolJpaService symbolJpaService;

    @AfterEach
    void tearDown() {
        this.jpaService.deleteAll();
        this.symbolJpaService.deleteAll();
    }

    @Test
    void whenSaveSomeMarketData_thenItIsUpdatedWithPreviousMarketData() throws ClientException {
        // given
        Symbol symbol = new Symbol().setName("AAPL");
        News previous = new News()
                .setSymbol(symbol)
                .setExternalId(1L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline1")
                .setUrl("url");

        Symbol symbol2 = new Symbol().setName("MSFT");
        News current = new News()
                .setSymbol(symbol2)
                .setExternalId(2L)
                .setDate(LocalDateTime.now())
                .setHeadline("Headline2")
                .setUrl("url");

        symbolJpaService.saveAll(List.of(symbol, symbol2));

        // when
        jpaService.saveAll(List.of(previous, current));

        // then
        verify(embeddingsClient, times(1)).embed(previous);
        verify(embeddingsClient, times(1)).embed(current);
        verify(embeddingsClient, times(2)).embed(any());
    }
}
