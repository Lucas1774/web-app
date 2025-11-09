package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.TestConfiguration;
import com.lucas.server.components.tradingbot.common.jpa.DataManager;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.lucas.server.common.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestConfiguration.class)
class RecommendationChatCompletionClientTest {

    @Autowired
    private MarketDataJpaService marketDataService;

    @Autowired
    private NewsJpaService newsService;

    @Autowired
    private PortfolioJpaService portfolioService;

    @Autowired
    private SymbolJpaService symbolService;

    @Autowired
    private MarketDataKpiGenerator kpiGenerator;

    @Autowired
    private AssetReportDataProvider provider;

    @Test
    void testProvide() {
        // given: insert 34 days of market data. Needs to be at least 34 and greater than the history days
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        LocalDate today = LocalDate.now();
        List<MarketData> mds = new ArrayList<>();
        for (int i = MARKET_DATA_RELEVANT_DAYS_COUNT; 0 <= i; i--) {
            MarketData md = new MarketData()
                    .setSymbol(symbolService.getOrCreateByName(Set.of(symbol.getName())).stream().findFirst().orElseThrow())
                    .setDate(today.minusDays(i))
                    .setOpen(BigDecimal.valueOf(10 + i))
                    .setHigh(BigDecimal.valueOf(11 + i))
                    .setLow(BigDecimal.valueOf(9 + i))
                    .setPrice(BigDecimal.valueOf(10 + i))
                    .setVolume(1_000L * (i + 1));
            mds.add(md);
        }
        marketDataService.createIgnoringDuplicates(mds.stream().sorted(Comparator.comparing(MarketData::getDate)).toList());
        BigDecimal lastPrice = mds.getLast().getPrice();
        MarketData premarket = new MarketData()
                .setSymbol(symbol)
                .setOpen(lastPrice)
                .setHigh(lastPrice.add(BigDecimal.valueOf(1)))
                .setLow(lastPrice.subtract(BigDecimal.valueOf(1)))
                .setPrice(lastPrice.add(new BigDecimal("0.5")));

        // given: insert 15 news items. Needs to be greater than the news per symbol and day
        List<News> articles = new ArrayList<>();
        LocalDateTime todayDateTime = LocalDateTime.now()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();
        for (int i = 1; 15 >= i; i++) {
            News news = new News()
                    .addSymbol(symbol)
                    .setExternalId((long) i)
                    .setUrl("https://example.com/news/" + i)
                    .setHeadline("Headline " + i)
                    .setSummary("Summary " + i)
                    .setDate(todayDateTime.minusDays(i - 1))
                    .setSentiment(0 == i % 3 ? "positive" : 0 == i % 2 ? "neutral" : "negative")
                    .setSentimentConfidence(BigDecimal.valueOf((i * 6) % 100));
            articles.add(news);
        }
        newsService.createOrUpdate(articles);

        // given: a portfolio for the symbol
        Portfolio portfolio = new Portfolio();
        portfolio.setSymbol(symbol);
        portfolio.setQuantity(BigDecimal.valueOf(1.1111));
        portfolio.setAverageCost(BigDecimal.valueOf(2.2222));
        portfolio.setAverageCommission(BigDecimal.ZERO);
        portfolio.setEffectiveTimestamp(today.atStartOfDay());
        portfolioService.save(portfolio);

        // when
        List<MarketData> filteredMds = marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT);
        List<News> filteredNews = newsService.getTopForSymbolId(symbol.getId(), NEWS_COUNT);
        Portfolio portfolioData = portfolioService.findBySymbol(symbol).orElseThrow();
        AssetReportRaw report = provider.provide(new DataManager.SymbolPayload(symbol, filteredMds, portfolioData)
                .setNews(filteredNews)
                .setPremarket(premarket));

        // then: symbol & premarket & history & news
        assertThat(report.symbol()).isEqualTo(symbol.getName());
        assertThat(report.premarket()).isNotNull()
                .satisfies(p -> {
                    assertThat(p.open()).isEqualByComparingTo(premarket.getOpen());
                    assertThat(p.high()).isEqualByComparingTo(premarket.getHigh());
                    assertThat(p.low()).isEqualByComparingTo(premarket.getLow());
                    assertThat(p.close()).isEqualByComparingTo(premarket.getPrice());
                    assertThat(p.gap()).isEqualByComparingTo(premarket.getPrice().subtract(lastPrice)
                            .divide(lastPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
                });
        assertThat(report.historyDays()).isEqualTo(HISTORY_DAYS_COUNT);
        assertThat(report.priceHistory())
                .hasSize(HISTORY_DAYS_COUNT)
                .extracting(PricePointRaw::date)
                .containsExactly(today, today.minusDays(1), today.minusDays(2), today.minusDays(3),
                        today.minusDays(4), today.minusDays(5), today.minusDays(6),
                        today.minusDays(7), today.minusDays(8), today.minusDays(9)
                );

        assertThat(report.newsCount()).isEqualTo(10); // 5 have neutral sentiment
        assertThat(report.news())
                .hasSize(10)
                .extracting(NewsItemRaw::headline)
                .containsExactly("Headline 1", "Headline 3", "Headline 5", "Headline 6", "Headline 7",
                        "Headline 9", "Headline 11", "Headline 12", "Headline 13", "Headline 15");

        // then: KPIs match the kpiGenerator calculations
        List<MarketData> mdHistory = marketDataService.getTopForSymbolId(
                symbolService.getOrCreateByName(Set.of(symbol.getName())).stream().findFirst().orElseThrow().getId(), 100);

        BigDecimal expectedEma20 = kpiGenerator.computeEma(mdHistory, 20).orElseThrow();
        BigDecimal macdLine1226 = kpiGenerator.computeMacdLine(mdHistory, 12, 26).orElseThrow();
        BigDecimal expectedMacdSignalLine9 = kpiGenerator.computeSignalLine(mdHistory, 9, 12, 26).orElseThrow();
        BigDecimal expectedRsi14 = kpiGenerator.computeRsi(mdHistory.getFirst());
        BigDecimal expectedAtr14 = kpiGenerator.computeRelativeAtr(mdHistory.getFirst());
        BigDecimal expectedObv20 = kpiGenerator.computeObv(mdHistory, 20).orElseThrow();

        assertThat(report.ema20()).isEqualByComparingTo(expectedEma20);
        assertThat(report.macdLine1226()).isEqualByComparingTo(macdLine1226);
        assertThat(report.macdSignalLine9()).isEqualByComparingTo(expectedMacdSignalLine9);
        assertThat(report.rsi14()).isEqualByComparingTo(expectedRsi14);
        assertThat(report.atr14()).isEqualByComparingTo(expectedAtr14);
        assertThat(report.obv20()).isEqualByComparingTo(expectedObv20);

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
        assertThat(report.unrealizedPercentPnL()).isEqualByComparingTo(BigDecimal.valueOf(350.0045)); // roughly (10 / 2.2) - 2.2
    }
}
