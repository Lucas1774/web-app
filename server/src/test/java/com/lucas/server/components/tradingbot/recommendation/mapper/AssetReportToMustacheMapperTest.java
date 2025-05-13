package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.lucas.server.common.exception.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssetReportToMustacheMapperTest {

    private final AssetReportToMustacheMapper mapper = new AssetReportToMustacheMapper();

    @Test
    void map_injectsKnownFields_and_leavesNullsEmpty() throws JsonProcessingException {
        // given
        AssetReportToMustacheMapper.AssetReportRaw asset = getAssetReport();

        // when & then
        // It should contain the known, non-null scalars:
        assertThat(mapper.map(List.of(asset)))
                .contains("[ASSET: FOO]")
                .contains("5-day MA: 105")
                .contains("RSI14: 42.42")
                .contains("ATR14: 1.23")
                .contains("Volatility (14d): 15.67%")
                // It should render the single price point line:
                .contains("2025-05-01: O100 H110 L90 C105 V1234")
                // It should render the news headline, and leave sentiment blank:
                .contains("• 2025-05-01: \\\"Headline One\\\" (Sentiment: ) Summary: First summary")
                .contains("• 2025-05-02: \\\"Headline Two\\\" (Sentiment: ) Summary: Second summary")
                // And placeholders for nulls (position, PnL, etc.) appear as empty
                .contains("Current Position: N/A shares (N/A EUR)")
                .contains("Avg Entry Price: N/A EUR")
                .contains("Unrealized PnL including trading costs: N/A EUR");
    }

    private static AssetReportToMustacheMapper.AssetReportRaw getAssetReport() {
        AssetReportToMustacheMapper.PricePointRaw pp = new AssetReportToMustacheMapper.PricePointRaw(
                LocalDate.of(2025, 5, 1), new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("90"), new BigDecimal("105"), 1234L
        );
        List<AssetReportToMustacheMapper.NewsItemRaw> news = List.of(
                new AssetReportToMustacheMapper.NewsItemRaw("Headline One", null, "First summary",
                        LocalDate.of(2025, 5, 1).atStartOfDay()),
                new AssetReportToMustacheMapper.NewsItemRaw("Headline Two", null, "Second summary",
                        LocalDate.of(2025, 5, 2).atStartOfDay())
        );
        return new AssetReportToMustacheMapper.AssetReportRaw("FOO",
                null, null, null, 1,
                List.of(pp), new BigDecimal("105.00"), new BigDecimal("42.42"), new BigDecimal("1.23"),
                new BigDecimal("15.67"), null, 1, news
        );
    }
}
