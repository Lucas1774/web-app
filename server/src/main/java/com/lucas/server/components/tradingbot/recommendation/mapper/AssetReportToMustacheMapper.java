package com.lucas.server.components.tradingbot.recommendation.mapper;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.lucas.server.common.exception.ConfigurationException;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.utils.Mapper;
import com.lucas.utils.exception.MappingException;
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
import static com.lucas.utils.Utils.EMPTY_STRING;

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

    @Override
    public String map(List<AssetReportRaw> assets) throws MappingException {
        StringWriter out = new StringWriter();
        try {
            mustache.execute(out, Collections.singletonMap("assets", assets.stream().map(AssetReport::from).toList())).flush();
            return out.toString();
        } catch (Exception e) {
            throw new MappingException(MessageFormat.format(MAPPING_ERROR, "asset report"), e);
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
            PricePointRaw premarket,
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
            Long volume,
            BigDecimal gap
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
            PricePoint premarket,
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
                    null != report.position ? report.position.stripTrailingZeros().toPlainString() : NA,
                    null != report.positionValue ? report.positionValue.stripTrailingZeros().toPlainString() : NA,
                    null != report.entryPrice ? "$".concat(report.entryPrice.stripTrailingZeros().toPlainString()) : NA,
                    null != report.unrealizedPnL ? report.unrealizedPnL.stripTrailingZeros().toPlainString() : "0",
                    null != report.unrealizedPercentPnL ? report.unrealizedPercentPnL.stripTrailingZeros().toPlainString().concat("%") : "0%",
                    String.valueOf(report.historyDays),
                    null != report.premarket ? PricePoint.from(report.premarket) : null,
                    report.priceHistory.stream().map(PricePoint::from).toList(),
                    null != report.ema20 ? report.ema20.stripTrailingZeros().toPlainString() : NA,
                    null != report.macdLine1226 ? report.macdLine1226.stripTrailingZeros().toPlainString() : NA,
                    null != report.macdSignalLine9 ? report.macdSignalLine9.stripTrailingZeros().toPlainString() : NA,
                    null != report.macdLine1226 && null != report.macdSignalLine9
                            ? report.macdLine1226.subtract(report.macdSignalLine9).stripTrailingZeros().toPlainString()
                            : NA,
                    null != report.rsi14 ? report.rsi14.stripTrailingZeros().toPlainString() : NA,
                    null != report.atr14 ? report.atr14.stripTrailingZeros().toPlainString().concat("%") : NA,
                    null != report.obv20 ? report.obv20.stripTrailingZeros().toPlainString() : NA,
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
                String volume,
                String gap
        ) {
            private static PricePoint from(PricePointRaw point) {
                return new PricePoint(
                        null != point.date ? point.date.toString() : NA,
                        point.open.stripTrailingZeros().toPlainString(),
                        point.high.stripTrailingZeros().toPlainString(),
                        point.low.stripTrailingZeros().toPlainString(),
                        point.close.stripTrailingZeros().toPlainString(),
                        null != point.volume ? point.volume.toString() : NA,
                        null != point.gap ? point.gap.stripTrailingZeros().toPlainString() : NA
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
                        null != news.sentiment ? "Sentiment: ".concat(news.sentiment).concat(". ") : EMPTY_STRING,
                        null != news.sentimentConfidence ? "Confidence: ".concat(news.sentimentConfidence.stripTrailingZeros().toPlainString()).concat("%. ") : EMPTY_STRING,
                        news.summary,
                        news.date.atOffset(ZoneOffset.UTC)
                                .toInstant()
                                .atZone(NY_ZONE)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
                );
            }
        }
    }
}
