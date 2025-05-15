package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.marketdata.service.MarketDataKpiGenerator;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.portfolio.jpa.Portfolio;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioJpaService;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecommendationChatCompletionClientTest {

    @Autowired
    MarketDataJpaService marketDataService;

    @Autowired
    NewsJpaService newsService;

    @Autowired
    PortfolioJpaService portfolioService;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    MarketDataKpiGenerator kpiGenerator;

    @Autowired
    AssetReportDataProvider provider;

    @BeforeEach
    void setup() {
        marketDataService.deleteAll();
        newsService.deleteAll();
        symbolService.deleteAll();
    }

    @Test
    void testProvide() {
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

        // given: insert 15 news items. Needs to be greater than the news per symbol and day
        List<News> articles = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            News news = new News()
                    .addSymbol(symbol)
                    .setExternalId((long) i)
                    .setUrl("https://example.com/news/" + i)
                    .setHeadline("Headline " + i)
                    .setSummary("Summary " + i)
                    .setDate(today.minusDays(i).atStartOfDay())
                    .setSentiment(i % 3 == 0 ? "positive" : i % 2 == 0 ? "neutral" : "negative")
                    .setSentimentConfidence(BigDecimal.valueOf((i * 6) % 100));
            articles.add(news);
        }
        newsService.createIgnoringDuplicates(articles, false);

        // given: a portfolio for the symbol
        Portfolio portfolio = new Portfolio();
        portfolio.setSymbol(symbol);
        portfolio.setQuantity(BigDecimal.valueOf(1.1111));
        portfolio.setAverageCost(BigDecimal.valueOf(2.2222));
        portfolio.setEffectiveTimestamp(today.atStartOfDay());
        portfolioService.save(portfolio);

        // when
        List<MarketData> filteredMds = this.marketDataService.getTopForSymbolId(symbol.getId(), Constants.HISTORY_DAYS_COUNT);
        List<News> filteredNews = this.newsService.getTopForSymbolId(symbol.getId(), Constants.NEWS_COUNT);
        Portfolio portfolioData = this.portfolioService.findBySymbol(symbol).orElseThrow();
        AssetReportRaw report = provider.provide(symbol, filteredMds, filteredNews, portfolioData);

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
                .extracting(NewsItemRaw::headline)
                .containsExactly("Headline 1", "Headline 3", "Headline 5", "Headline 6", "Headline 7",
                        "Headline 9", "Headline 11", "Headline 12", "Headline 13", "Headline 15");

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

        // then: position fields match
        assertThat(report.position()).isEqualByComparingTo(BigDecimal.valueOf(1.1111));
        assertThat(report.entryPrice()).isEqualByComparingTo(BigDecimal.valueOf(2.2222));
        BigDecimal expectedPositionValue = BigDecimal.valueOf(1.1111)
                .multiply(BigDecimal.valueOf(2.2222))
                .setScale(4, RoundingMode.HALF_UP);
        assertThat(report.positionValue()).isEqualByComparingTo(expectedPositionValue);
        BigDecimal expectedUnrealizedPnL = mdHistory.getFirst()
                .getPrice()
                .subtract(report.entryPrice())
                .multiply(report.position())
                .setScale(4, RoundingMode.HALF_UP);
        assertThat(report.unrealizedPnL()).isEqualByComparingTo(expectedUnrealizedPnL);
    }
}
