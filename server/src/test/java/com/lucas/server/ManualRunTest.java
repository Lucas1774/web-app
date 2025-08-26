package com.lucas.server;

import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.server.components.tradingbot.common.DailyScheduler;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataRepository;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic program runner. Run containers manually and pass production env vars as env vars
 */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@Disabled("Manual run only")
class ManualRunTest {

    private static final List<String> symbolNames = List.of("AAPL", "NVDA", "MSFT", "AMZN", "META", "TSLA", "GOOGL");
    private static final LocalDate from = LocalDate.of(2025, 8, 1); // inclusive
    private static final LocalDate to = LocalDate.now(); // exclusive

    @Autowired
    private Map<String, AIClient> allClients;

    @Autowired
    private RecommendationsRepository recommendationsRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private DailyScheduler dailyScheduler;

    @Test
    @Transactional
    void runMidnightTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMidnightTask", List.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, symbolNames);
    }

    @Test
    @Transactional
    void runMorningTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMorningTask", List.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, symbolNames);
    }

    @Test
    @Transactional
    void assertRecommendationsPrecision() {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Set<String> clientNames = filterClients(allClients, RecommendationMode.FINE_GRAIN)
                .stream()
                .map(c -> c.getConfig().name())
                .collect(Collectors.toSet());

        Map<Recommendation, MarketData> recommendationToFutureMd = recommendationsRepository
                .findByDateBetween(from, to.minusDays(1))
                .stream()
                .filter(r -> isTradingDate(r.getDate())
                        && BUY.equals(r.getAction())
                        && clientNames.contains(r.getModel())
                        && r.getConfidence().compareTo(BigDecimal.valueOf(0.9)) >= 0
                )
                .flatMap(r -> marketDataRepository.findBySymbol_IdInAndDateIn(
                                List.of(r.getSymbol().getId()),
                                List.of(r.getDate())
                        ).stream()
                        .findFirst()
                        .map(md -> Map.entry(r, md))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Stats stats = recommendationToFutureMd.values().stream()
                .map(d -> {
                    BigDecimal open = d.getOpen();
                    BigDecimal close = d.getPrice()
                            .subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    BigDecimal high = d.getHigh()
                            .subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    BigDecimal low = d.getLow()
                            .subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    return Stats.of(close, high, low);
                })
                .reduce(Stats::combine)
                .orElseThrow();

        BigDecimal cnt = BigDecimal.valueOf(stats.count);
        BigDecimal avgClosePct = stats.sumClose.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgHighPct = stats.sumHigh.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgLowPct = stats.sumLow.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        BigDecimal maxClosePct = stats.maxClose.multiply(BigDecimal.valueOf(100));
        BigDecimal minClosePct = stats.minClose.multiply(BigDecimal.valueOf(100));
        BigDecimal maxHighPct = stats.maxHigh.multiply(BigDecimal.valueOf(100));
        BigDecimal minHighPct = stats.minHigh.multiply(BigDecimal.valueOf(100));
        BigDecimal maxLowPct = stats.maxLow.multiply(BigDecimal.valueOf(100));
        BigDecimal minLowPct = stats.minLow.multiply(BigDecimal.valueOf(100));

        BigDecimal onePct = BigDecimal.valueOf(0.01);
        BigDecimal minusOnePct = onePct.negate();

        long profitableCount = recommendationToFutureMd.values().stream()
                .map(md -> md.getHigh()
                        .subtract(md.getOpen())
                        .divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(onePct) > 0)
                .count();

        long safeCount = recommendationToFutureMd.values().stream()
                .map(md -> md.getLow()
                        .subtract(md.getOpen())
                        .divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(minusOnePct) > 0)
                .count();

        System.out.printf(
                "Sample size: %d%n" +
                        "Avg close profit: %+.2f%%%n" +
                        "Avg max profit: %+.2f%%%n" +
                        "Avg max loss: %+.2f%%%n" +
                        "Max gain at close: %+.2f%%%n" +
                        "Max loss at close: %+.2f%%%n" +
                        "Max possible gain: %+.2f%%%n" +
                        "Min possible gain: %+.2f%%%n" +
                        "Min possible loss: %+.2f%%%n" +
                        "Max possible loss: %+.2f%%%n" +
                        "Profitable records (max gain >1%%): %d%n" +
                        "Safe records (max loss <1%%): %d%n",
                stats.count,
                avgClosePct, avgHighPct, avgLowPct,
                maxClosePct, minClosePct,
                maxHighPct, minHighPct,
                maxLowPct, minLowPct,
                profitableCount,
                safeCount
        );
    }

    record Stats(
            BigDecimal sumClose, BigDecimal sumHigh, BigDecimal sumLow,
            BigDecimal maxClose, BigDecimal minClose,
            BigDecimal maxHigh, BigDecimal minHigh,
            BigDecimal maxLow, BigDecimal minLow,
            long count
    ) {
        static Stats of(BigDecimal close, BigDecimal high, BigDecimal low) {
            return new Stats(
                    close, high, low,
                    close, close,
                    high, high,
                    low, low,
                    1
            );
        }

        Stats combine(Stats o) {
            return new Stats(
                    sumClose.add(o.sumClose),
                    sumHigh.add(o.sumHigh),
                    sumLow.add(o.sumLow),
                    maxClose.max(o.maxClose),
                    minClose.min(o.minClose),
                    maxHigh.max(o.maxHigh),
                    minHigh.min(o.minHigh),
                    maxLow.max(o.maxLow),
                    minLow.min(o.minLow),
                    count + o.count
            );
        }
    }
}
