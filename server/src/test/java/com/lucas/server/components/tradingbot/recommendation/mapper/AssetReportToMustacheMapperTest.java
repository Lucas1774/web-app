package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                  • N/A shares ($N/A)
                  • Avg Entry Price: $N/A
                  • Unrealized PnL: $0 (0%)
                • Price History (last 1 days):
                  • 2025-05-01: O100 H110 L90 C105 VN/A
                • Technical Indicators:
                  • 20-day EMA: N/A
                  • MACD(12,26,9): line=N/A,signal=N/A,hist=N/A
                  • 14-day RSI: N/A
                  • 14-day ATR%: N/A%
                  • 20-day OBV: N/A
                • News Summaries (last 2):
                  • 2025-04-30 20:00:00 EDT: "Headline One" (Sentiment: N/A. Confidence: 54.4412%) Summary: First summary
                  • 2025-05-01 20:00:00 EDT: "Headline Two" (Sentiment: N/A. Confidence: 54.4412%) Summary: Second summary
                
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
                  • 10.2412 shares ($101.4887)
                  • Avg Entry Price: $11.5874
                  • Unrealized PnL: $50 (80%)
                • Price History (last 1 days):
                  • 2025-05-01: O100 H110 L90 C105 V1234
                • Technical Indicators:
                  • 20-day EMA: 105
                  • MACD(12,26,9): line=42.42,signal=1.23,hist=41.19
                  • 14-day RSI: 15.67
                  • 14-day ATR%: 15.68%
                  • 20-day OBV: 15.69
                • News Summaries (last 2):
                  • 2025-04-30 20:00:00 EDT: "Headline One" (Sentiment: positive. Confidence: 54.4412%) Summary: First summary
                  • 2025-05-01 20:00:00 EDT: "Headline Two" (Sentiment: negative. Confidence: 54.4412%) Summary: Second summary
                
                """;

        assertThat(objectMapper.readTree(mapper.map(List.of(asset))).get("content").asText()).isEqualTo(expected);
    }

    private static AssetReportRaw getAssetReportNullValues() {
        List<PricePointRaw> pp = getPpNullValues();
        List<NewsItemRaw> news = getNewsNullValues();
        return new AssetReportRaw("FOO", null, null, null, null, null,
                pp.size(), pp, null, null, null, null, null, null, news.size(), news);
    }

    private static AssetReportRaw getAssetReportAllValues() {
        List<PricePointRaw> pp = getPpAllValues();
        List<NewsItemRaw> news = getNewsAllValues();
        return new AssetReportRaw("FOO", new BigDecimal("10.2412"), new BigDecimal("101.4887"),
                new BigDecimal("11.5874"), new BigDecimal("50"), new BigDecimal("80.00"), pp.size(), pp,
                new BigDecimal("105.00"), new BigDecimal("42.42"), new BigDecimal("1.23"),
                new BigDecimal("15.67"), new BigDecimal("15.68"), new BigDecimal("15.69"), news.size(), news);
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
                        "First summary", LocalDateTime.of(LocalDate.of(2025, 5, 1), LocalTime.MIDNIGHT)),
                new NewsItemRaw("Headline Two", null, BigDecimal.valueOf(54.4412),
                        "Second summary", LocalDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.MIDNIGHT))
        );
    }

    private static List<NewsItemRaw> getNewsAllValues() {
        return List.of(
                new NewsItemRaw("Headline One", "positive", BigDecimal.valueOf(54.4412),
                        "First summary", LocalDateTime.of(LocalDate.of(2025, 5, 1), LocalTime.MIDNIGHT)),
                new NewsItemRaw("Headline Two", "negative", BigDecimal.valueOf(54.4412),
                        "Second summary", LocalDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.MIDNIGHT))
        );
    }
}
