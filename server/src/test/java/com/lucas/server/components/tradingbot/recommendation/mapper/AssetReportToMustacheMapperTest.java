package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssetReportToMustacheMapperTest {

    private final AssetReportToMustacheMapper mapper = new AssetReportToMustacheMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void map_injectsMandatoryFields_and_leavesNullsEmpty() throws IOException {
        // given
        AssetReportRaw asset = getAssetReportNullValues();

        // when & then
        String expected = """
                --- Market Data & Features ---
                [ASSET: FOO]
                • Current Position:
                  • N/A shares (N/A EUR)
                  • Avg Entry Price: N/A EUR
                • Price History (last 1 days):
                  • 2025-05-01: O 100 H 110 L 90 C 105 V N/A
                • Technical Indicators: 5-day MA: 105, RSI14: 42.42, ATR14: 1.23
                • Asset KPIs: Unrealized PnL including trading costs: N/A EUR, Volatility (14d): 15.67%
                --- News Summaries (Top 2) ---
                  • 2025-05-01: "Headline One" (Sentiment: N/A. Confidence: 54.4412%) Summary: First summary
                  • 2025-05-02: "Headline Two" (Sentiment: N/A. Confidence: 54.4412%) Summary: Second summary
                
                
                """;
        assertThat(objectMapper.readTree(mapper.map(List.of(asset))).get("content").asText()).isEqualTo(expected);
    }

    @Test
    void map_injectsAllField() throws IOException {
        // given
        AssetReportRaw asset = getAssetReportAllValues();

        // when & then
        String expected = """
                --- Market Data & Features ---
                [ASSET: FOO]
                • Current Position:
                  • 10.2412 shares (101.4887 EUR)
                  • Avg Entry Price: 11.5874 EUR
                • Price History (last 1 days):
                  • 2025-05-01: O 100 H 110 L 90 C 105 V 1234
                • Technical Indicators: 5-day MA: 105, RSI14: 42.42, ATR14: 1.23
                • Asset KPIs: Unrealized PnL including trading costs: 50 EUR, Volatility (14d): 15.67%
                --- News Summaries (Top 2) ---
                  • 2025-05-01: "Headline One" (Sentiment: positive. Confidence: 54.4412%) Summary: First summary
                  • 2025-05-02: "Headline Two" (Sentiment: negative. Confidence: 54.4412%) Summary: Second summary
                
                
                """;

        assertThat(objectMapper.readTree(mapper.map(List.of(asset))).get("content").asText()).isEqualTo(expected);
    }

    private static AssetReportRaw getAssetReportNullValues() {
        List<PricePointRaw> pp = getPpNullValues();
        List<NewsItemRaw> news = getNewsNullValues();
        return new AssetReportRaw("FOO", null, null, null, pp.size(),
                pp, new BigDecimal("105.00"), new BigDecimal("42.42"), new BigDecimal("1.23"),
                new BigDecimal("15.67"), null, news.size(), news
        );
    }

    private static AssetReportRaw getAssetReportAllValues() {
        List<PricePointRaw> pp = getPpAllValues();
        List<NewsItemRaw> news = getNewsAllValues();
        return new AssetReportRaw("FOO", new BigDecimal("10.2412"), new BigDecimal("101.4887"),
                new BigDecimal("11.5874"), pp.size(), pp, new BigDecimal("105.00"), new BigDecimal("42.42"),
                new BigDecimal("1.23"), new BigDecimal("15.67"), new BigDecimal("50"), news.size(), news);
    }

    private static List<PricePointRaw> getPpNullValues() {
        return List.of(
                new PricePointRaw(LocalDate.of(2025, 5, 1), new BigDecimal("100"),
                        new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"), null)
        );
    }

    private static List<PricePointRaw> getPpAllValues() {
        return List.of(
                new PricePointRaw(LocalDate.of(2025, 5, 1), new BigDecimal("100"),
                        new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("105"), 1234L)
        );
    }

    private static List<NewsItemRaw> getNewsNullValues() {
        return List.of(
                new NewsItemRaw("Headline One", null, BigDecimal.valueOf(54.4412),
                        "First summary", LocalDate.of(2025, 5, 1).atStartOfDay()),
                new NewsItemRaw("Headline Two", null, BigDecimal.valueOf(54.4412),
                        "Second summary", LocalDate.of(2025, 5, 2).atStartOfDay())
        );
    }

    private static List<NewsItemRaw> getNewsAllValues() {
        return List.of(
                new NewsItemRaw("Headline One", "positive", BigDecimal.valueOf(54.4412),
                        "First summary", LocalDate.of(2025, 5, 1).atStartOfDay()),
                new NewsItemRaw("Headline Two", "negative", BigDecimal.valueOf(54.4412),
                        "Second summary", LocalDate.of(2025, 5, 2).atStartOfDay())
        );
    }
}
