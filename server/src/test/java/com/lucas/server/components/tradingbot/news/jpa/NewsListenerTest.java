package com.lucas.server.components.tradingbot.news.jpa;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.service.NewsEmbeddingsClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
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

    @Test
    @Transactional
    void whenSaveSomeNewsWithCallback_thenItIsUpdatedWithPreviousNewsData() throws ClientException {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        News previous = new News()
                .addSymbol(symbol)
                .setExternalId(1L)
                .setDate(LocalDate.now())
                .setHeadline("Headline1")
                .setUrl("url");

        Symbol symbol2 = symbolService.getOrCreateByName("MSFT");
        News current = new News()
                .addSymbol(symbol2)
                .setExternalId(2L)
                .setDate(LocalDate.now())
                .setHeadline("Headline2")
                .setUrl("url");

        // when
        jpaService.createIgnoringDuplicates(List.of(previous, current), true);

        // then
        verify(embeddingsClient, times(1)).embed(previous);
        verify(embeddingsClient, times(1)).embed(current);
        verify(embeddingsClient, times(2)).embed((News) any());
    }

    @Test
    @Transactional
    void whenSaveSomeNewsNoCallback_thenItIsNotUpdatedWithPreviousNewsData() throws ClientException {
        // given
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        News previous = new News()
                .addSymbol(symbol)
                .setExternalId(1L)
                .setDate(LocalDate.now())
                .setHeadline("Headline1")
                .setUrl("url");

        Symbol symbol2 = symbolService.getOrCreateByName("MSFT");
        News current = new News()
                .addSymbol(symbol2)
                .setExternalId(2L)
                .setDate(LocalDate.now())
                .setHeadline("Headline2")
                .setUrl("url");

        // when
        jpaService.createIgnoringDuplicates(List.of(previous, current), false);

        // then
        verify(embeddingsClient, times(0)).embed(previous);
        verify(embeddingsClient, times(0)).embed(current);
        verify(embeddingsClient, times(0)).embed((News) any());
    }
}
