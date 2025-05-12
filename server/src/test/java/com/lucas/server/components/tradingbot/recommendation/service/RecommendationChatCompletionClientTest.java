package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.IllegalStateException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItem;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecommendationChatCompletionClientTest {

    @Autowired
    MarketDataJpaService marketDataService;

    @Autowired
    NewsJpaService newsService;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    MarketDataKpiGenerator kpiGenerator;

    @Autowired
    AssetReportDataProvider provider;

    @Autowired
    RecommendationChatCompletionClient chatCompletionClient;

    @MockitoBean
    @SuppressWarnings("unused")
    AzureOpenAiChatModel azureOpenAiChatModel;

    @BeforeEach
    void setup() {
        marketDataService.deleteAll();
        newsService.deleteAll();
        symbolService.deleteAll();
    }

    @Test
    void testProvide() throws IllegalStateException {
        // given: insert 15 days of market data. Needs to be greater than the history days
        Symbol symbol = symbolService.getOrCreateByName("AAPL");
        LocalDate today = LocalDate.now();
        List<MarketData> mds = new ArrayList<>();
        for (int i = 15; i >= 0; i--) {
            MarketData md = new MarketData()
                    .setSymbol(symbolService.getOrCreateByName(symbol.getName()))
                    .setDate(today.minusDays(i))
                    .setOpen(BigDecimal.valueOf(10 + i))
                    .setHigh(BigDecimal.valueOf(11 + i))
                    .setLow(BigDecimal.valueOf(9 + i))
                    .setPrice(BigDecimal.valueOf(10 + i))
                    .setVolume(1_000L * (i + 1));
            mds.add(md);
        }
        marketDataService.createIgnoringDuplicates(mds.stream().sorted(Comparator.comparing(MarketData::getDate)).toList());

        // given: insert 6 news items. Needs to be greater than the history days
        List<News> articles = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            News news = new News()
                    .setSymbol(symbol)
                    .setExternalId((long) i)
                    .setUrl("https://example.com/news/" + i)
                    .setHeadline("Headline " + i)
                    .setSummary("Summary " + i)
                    .setDate(today.minusDays(i).atStartOfDay());
            articles.add(news);
        }
        newsService.createIgnoringDuplicates(articles, false);

        // when
        List<MarketData> filteredMds = this.marketDataService.getTopForSymbolId(symbol.getId(), Constants.HISTORY_DAYS_COUNT);
        List<News> filteredNews = this.newsService.getTopForSymbolId(symbol.getId(), Constants.NEWS_COUNT);
        AssetReportRaw report = provider.provide(symbol, filteredMds, filteredNews);
        assertThatThrownBy(() -> chatCompletionClient.getRecommendations(Map.of(symbol, filteredMds), Map.of(symbol, filteredNews)))
                .isInstanceOf(ClientException.class);

        // then: symbol & history size & news count
        assertThat(report.symbol()).isEqualTo(symbol.getName());
        assertThat(report.historyDays()).isEqualTo(Constants.HISTORY_DAYS_COUNT);
        assertThat(report.priceHistory())
                .hasSize(Constants.HISTORY_DAYS_COUNT)
                .extracting(PricePointRaw::date)
                .containsExactly(today, today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(4),
                        today.minusDays(5), today.minusDays(6), today.minusDays(7), today.minusDays(8), today.minusDays(9),
                        today.minusDays(10), today.minusDays(11), today.minusDays(12), today.minusDays(13)
                );

        assertThat(report.newsCount()).isEqualTo(Constants.NEWS_COUNT);
        assertThat(report.news())
                .hasSize(Constants.NEWS_COUNT)
                .extracting(NewsItem::headline)
                .containsExactly("Headline 1", "Headline 2", "Headline 3", "Headline 4", "Headline 5");

        // then: KPIs match the kpiGenerator calculations
        List<MarketData> mdHistory = marketDataService.getTopForSymbolId(
                symbolService.findByName(symbol.getName()).orElseThrow().getId(), Constants.HISTORY_DAYS_COUNT);
        BigDecimal expectedMa5 = kpiGenerator.computeMovingAverage(mdHistory);
        BigDecimal expectedRsi14 = kpiGenerator.computeRsi(mdHistory);
        BigDecimal expectedAtr14 = kpiGenerator.computeAtr(mdHistory);
        BigDecimal expectedVolatility = kpiGenerator.computeVolatility(mdHistory);

        assertThat(report.ma5()).isEqualByComparingTo(expectedMa5);
        assertThat(report.rsi14()).isEqualByComparingTo(expectedRsi14);
        assertThat(report.atr14()).isEqualByComparingTo(expectedAtr14);
        assertThat(report.volatility()).isEqualByComparingTo(expectedVolatility);

        // then: unmapped fields are null as per implementation
        assertThat(report.position()).isNull();
        assertThat(report.entryPrice()).isNull();
        assertThat(report.positionValue()).isNull();
        assertThat(report.unrealizedPnL()).isNull();
    }
}
