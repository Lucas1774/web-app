package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static com.lucas.server.common.Constants.CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

class AssetReportToMustacheMapperTest {

    private final AssetReportToMustacheMapper mapper = new AssetReportToMustacheMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void map_injectsMandatoryFields_and_leavesNullsEmpty() throws MappingException {
        // given
        AssetReportRaw asset = getAssetReportNullValues();

        // when & then
        String expected =
                """
                --- Market Data & Features ---
                [ASSET: FOO]
                • Price History (last 1 days):
                  • 2025-05-01: O100 H110 L90 C105 VN/A
                • Technical Indicators at last close:
                  • 20-day EMA: N/A
                  • MACD(12,26,9): line=N/A,signal=N/A,hist=N/A
                  • 14-day RSI: N/A
                  • 14-day ATR%: N/A
                  • 20-day OBV: N/A
                • News Summaries (last 2):
                  • 2025-04-30 20:00:00 EST: Headline One: First summary
                  • 2025-05-01 20:00:00 EST: Headline Two: Second summary
                
                """;
        assertThat(objectMapper.readTree(mapper.map(Set.of(asset))).get(CONTENT).asString()).isEqualTo(expected);
    }

    @Test
    void map_injectsAllField() throws MappingException {
        // given
        AssetReportRaw asset = getAssetReportAllValues();

        // when & then
        String expected =
                """
                --- Market Data & Features ---
                [ASSET: FOO]
                • This morning's pre-market: O100 H110 L90 Last price105 Gap: 7.5%
                • Price History (last 1 days):
                  • 2025-05-01: O100 H110 L90 C105 V1234
                • Technical Indicators at last close:
                  • 20-day EMA: 105
                  • MACD(12,26,9): line=42.42,signal=1.23,hist=41.19
                  • 14-day RSI: 15.67
                  • 14-day ATR%: 15.68%
                  • 20-day OBV: 15.69
                • News Summaries (last 2):
                  • 2025-04-30 20:00:00 EST: Sentiment: positive. Confidence: 54.4412%. Headline One: First summary
                  • 2025-05-01 20:00:00 EST: Sentiment: negative. Confidence: 54.4412%. Headline Two: Second summary
                
                """;

        assertThat(objectMapper.readTree(mapper.map(Set.of(asset))).get(CONTENT).asString()).isEqualTo(expected);
    }

    private static AssetReportRaw getAssetReportNullValues() {
        PricePointRaw pp = getPpNullValues();
        OrderedIndexedSet<NewsItemRaw> news = getNewsNullValues();
        return new AssetReportRaw("FOO",
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                OrderedIndexedSet.of(pp),
                null,
                null,
                null,
                null,
                null,
                null,
                news.size(),
                news);
    }

    private static PricePointRaw getPpNullValues() {
        return new PricePointRaw(LocalDate.of(2025, 5, 1),
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                null,
                null);
    }

    private static OrderedIndexedSet<NewsItemRaw> getNewsNullValues() {
        return OrderedIndexedSet.of(new NewsItemRaw("Headline One",
                        null,
                        null,
                        "First summary",
                        LocalDateTime.of(LocalDate.of(2025, 5, 1), LocalTime.MIDNIGHT)),
                new NewsItemRaw("Headline Two",
                        null,
                        null,
                        "Second summary",
                        LocalDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.MIDNIGHT)));
    }

    private static AssetReportRaw getAssetReportAllValues() {
        PricePointRaw pp = getPpAllValues();
        OrderedIndexedSet<NewsItemRaw> news = getNewsAllValues();
        return new AssetReportRaw("FOO",
                new BigDecimal("10.2412"),
                new BigDecimal("101.4887"),
                new BigDecimal("11.5874"),
                new BigDecimal("50"),
                new BigDecimal("80.00"),
                1,
                pp,
                OrderedIndexedSet.of(pp),
                new BigDecimal("105.00"),
                new BigDecimal("42.42"),
                new BigDecimal("1.23"),
                new BigDecimal("15.67"),
                new BigDecimal("15.68"),
                new BigDecimal("15.69"),
                news.size(),
                news);
    }

    private static PricePointRaw getPpAllValues() {
        return new PricePointRaw(LocalDate.of(2025, 5, 1),
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                1234L,
                new BigDecimal("7.5"));
    }

    private static OrderedIndexedSet<NewsItemRaw> getNewsAllValues() {
        return OrderedIndexedSet.of(new NewsItemRaw("Headline One",
                        "positive",
                        BigDecimal.valueOf(54.4412),
                        "First summary",
                        LocalDateTime.of(LocalDate.of(2025, 5, 1), LocalTime.MIDNIGHT)),
                new NewsItemRaw("Headline Two",
                        "negative",
                        BigDecimal.valueOf(54.4412),
                        "Second summary",
                        LocalDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.MIDNIGHT)));
    }
}
