package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.lucas.server.common.Mapper;
import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.lucas.server.common.Constants.*;

@Component
public class AssetReportToMustacheMapper implements Mapper<List<AssetReportRaw>, String> {

    private final Mustache mustache;

    public AssetReportToMustacheMapper() {
        MustacheFactory mf = new DefaultMustacheFactory();
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(AssetReportToMustacheMapper.class.getResourceAsStream("/prompt/asset-template.mustache")),
                StandardCharsets.UTF_8)) {
            mustache = mf.compile(reader, "asset-report");
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    public record AssetReportRaw(
            String symbol,
            BigDecimal position,
            BigDecimal positionValue,
            BigDecimal entryPrice,
            BigDecimal unrealizedPnL,
            BigDecimal unrealizedPercentPnL,
            int historyDays,
            List<PricePointRaw> priceHistory,
            BigDecimal ema20,
            BigDecimal macdLine1226,
            BigDecimal macdSignalLine9,
            BigDecimal rsi14,
            BigDecimal atr14,
            BigDecimal obv20,
            int newsCount,
            List<NewsItemRaw> news
    ) {
    }

    public record PricePointRaw(
            LocalDate date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume
    ) {
    }

    public record NewsItemRaw(
            String headline,
            String sentiment,
            BigDecimal sentimentConfidence,
            String summary,
            LocalDateTime date
    ) {
    }

    public record AssetReport(
            String symbol,
            String position,
            String positionValue,
            String entryPrice,
            String unrealizedPnL,
            String unrealizedPercentPnl,
            String historyDays,
            List<PricePoint> priceHistory,
            String ema20,
            String macdLine1226,
            String macdSignalLine9,
            String macdHist,
            String rsi14,
            String atr14,
            String obv20,
            String newsCount,
            List<NewsItem> news
    ) {
        private static AssetReport from(AssetReportRaw report) {
            return new AssetReport(
                    report.symbol,
                    report.position != null ? report.position.stripTrailingZeros().toPlainString() : NA,
                    report.positionValue != null ? report.positionValue.stripTrailingZeros().toPlainString() : NA,
                    report.entryPrice != null ? report.entryPrice.stripTrailingZeros().toPlainString() : NA,
                    report.unrealizedPnL != null ? report.unrealizedPnL.stripTrailingZeros().toPlainString() : "0",
                    report.unrealizedPercentPnL != null ? report.unrealizedPercentPnL.stripTrailingZeros().toPlainString().concat("%") : "0%",
                    String.valueOf(report.historyDays),
                    report.priceHistory.stream().map(PricePoint::from).toList(),
                    report.ema20 != null ? report.ema20.stripTrailingZeros().toPlainString() : NA,
                    report.macdLine1226 != null ? report.macdLine1226.stripTrailingZeros().toPlainString() : NA,
                    report.macdSignalLine9 != null ? report.macdSignalLine9.stripTrailingZeros().toPlainString() : NA,
                    report.macdLine1226 != null && report.macdSignalLine9 != null
                            ? report.macdLine1226.subtract(report.macdSignalLine9).stripTrailingZeros().toPlainString()
                            : NA,
                    report.rsi14 != null ? report.rsi14.stripTrailingZeros().toPlainString() : NA,
                    report.atr14 != null ? report.atr14.stripTrailingZeros().toPlainString().concat("%") : NA,
                    report.obv20 != null ? report.obv20.stripTrailingZeros().toPlainString() : NA,
                    String.valueOf(report.newsCount),
                    report.news.stream().map(NewsItem::from).toList()
            );
        }

        private record PricePoint(
                String date,
                String open,
                String high,
                String low,
                String close,
                String volume
        ) {
            private static PricePoint from(PricePointRaw point) {
                return new PricePoint(
                        point.date.toString(),
                        point.open.stripTrailingZeros().toPlainString(),
                        point.high.stripTrailingZeros().toPlainString(),
                        point.low.stripTrailingZeros().toPlainString(),
                        point.close.stripTrailingZeros().toPlainString(),
                        point.volume != null ? point.volume.toString() : NA
                );
            }
        }

        private record NewsItem(
                String headline,
                String sentiment,
                String sentimentConfidence,
                String summary,
                String date
        ) {
            private static NewsItem from(NewsItemRaw news) {
                return new NewsItem(
                        news.headline,
                        news.sentiment != null ? news.sentiment : NA,
                        news.sentimentConfidence != null ? news.sentimentConfidence.stripTrailingZeros().toPlainString().concat("%") : NA,
                        news.summary,
                        news.date.atOffset(ZoneOffset.UTC)
                                .toInstant()
                                .atZone(NY_ZONE)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
                );
            }
        }
    }

    @Override
    public String map(List<AssetReportRaw> assets) throws JsonProcessingException {
        StringWriter out = new StringWriter();
        try {
            mustache.execute(out, Collections.singletonMap("assets", assets.stream().map(AssetReport::from).toList())).flush();
            return out.toString();
        } catch (Exception e) {
            throw new JsonProcessingException(MessageFormat.format(MAPPING_ERROR, "asset report"), e);
        }
    }
}
