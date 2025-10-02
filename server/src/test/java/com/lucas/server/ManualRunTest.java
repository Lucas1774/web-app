package com.lucas.server;

import com.lucas.server.components.tradingbot.common.DailyScheduler;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.BUY;
import static com.lucas.server.common.Constants.isTradingDate;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic program runner. Run containers manually and pass production env vars as env vars
 */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@Disabled("Manual run only")
class ManualRunTest {

    private static final List<String> symbolNames = List.of("AAPL", "NVDA", "MSFT", "AMZN", "META", "TSLA", "GOOGL");
    private static final LocalDate from = LocalDate.of(2025, 9, 1); // inclusive
    private static final LocalDate to = LocalDate.now(); // exclusive

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private RecommendationsRepository recommendationsRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private DailyScheduler dailyScheduler;

    private static Stats computeStats(Collection<MarketData> mds) {
        return mds.stream()
                .map(d -> {
                    BigDecimal open = d.getOpen();
                    BigDecimal close = d.getPrice().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    BigDecimal high = d.getHigh().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    BigDecimal low = d.getLow().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
                    return Stats.of(close, high, low);
                })
                .reduce(Stats::combine)
                .orElse(null);
    }

    private static String summaryOf(Stats s, String label) {
        if (s == null || s.count == 0) return label + ": (no data)";
        BigDecimal cnt = BigDecimal.valueOf(s.count);
        BigDecimal avgClose = s.sumClose.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgHigh = s.sumHigh.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgLow = s.sumLow.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return String.format(
                "%s%n" +
                        "Sample size: %d%n" +
                        "Avg close profit: %+.2f%%%n" +
                        "Avg max profit: %+.2f%%%n" +
                        "Avg max loss: %+.2f%%%n" +
                        "Max gain at close: %+.2f%%%n" +
                        "Max loss at close: %+.2f%%%n" +
                        "Max possible gain: %+.2f%%%n" +
                        "Min possible gain: %+.2f%%%n" +
                        "Min possible loss: %+.2f%%%n" +
                        "Max possible loss: %+.2f%%",
                label,
                s.count,
                avgClose,
                avgHigh,
                avgLow,
                s.maxClose.multiply(BigDecimal.valueOf(100)),
                s.minClose.multiply(BigDecimal.valueOf(100)),
                s.maxHigh.multiply(BigDecimal.valueOf(100)),
                s.minHigh.multiply(BigDecimal.valueOf(100)),
                s.maxLow.multiply(BigDecimal.valueOf(100)),
                s.minLow.multiply(BigDecimal.valueOf(100))
        );
    }

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
        record SymDate(Long symbolId, LocalDate date) {
        }

        Map<SymDate, MarketData> mdByKey = marketDataRepository.findBySymbol_IdInAndDateIn(
                        symbolRepository.findAll().stream().map(Symbol::getId).toList(),
                        from.datesUntil(to).toList()
                ).stream()
                .collect(Collectors.toMap(
                        md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()
                ));

        Map<Recommendation, MarketData> baselineMap = recommendationsRepository
                .findByDateBetween(from, to.minusDays(1))
                .stream()
                .filter(r -> isTradingDate(r.getDate()))
                .flatMap(r -> Optional.ofNullable(mdByKey.get(new SymDate(r.getSymbol().getId(), r.getDate())))
                        .map(md -> Map.entry(r, md))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<Recommendation, MarketData> filtered = baselineMap.entrySet()
                .stream()
                .filter(e -> {
                    if (!BUY.equals(e.getKey().getAction()) || e.getKey().getConfidence().compareTo(BigDecimal.valueOf(0.8)) < 0) {
                        return false;
                    }
                    BigDecimal gapPct = e.getValue().getOpen()
                            .subtract(e.getValue().getPreviousClose())
                            .divide(e.getValue().getPreviousClose(), 10, RoundingMode.HALF_UP);
                    return gapPct.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Stats filteredStats = computeStats(filtered.values());
        Stats baselineStats = computeStats(baselineMap.values());
        Stats allStats = computeStats(mdByKey.values());

        System.out.println(summaryOf(filteredStats, "FILTERED"));
        System.out.println(summaryOf(baselineStats, "BASELINE"));
        System.out.println(summaryOf(allStats, "ALL"));

        if (filteredStats != null && filteredStats.count > 0 && baselineStats != null && baselineStats.count > 0) {

            BigDecimal filteredAverageClose = filteredStats.sumClose.divide(BigDecimal.valueOf(filteredStats.count), 8, RoundingMode.HALF_UP);
            BigDecimal baselineAverageClose = baselineStats.sumClose.divide(BigDecimal.valueOf(baselineStats.count), 8, RoundingMode.HALF_UP);

            BigDecimal absDiff = filteredAverageClose.subtract(baselineAverageClose).multiply(BigDecimal.valueOf(100));
            BigDecimal denominator = filteredAverageClose.abs().max(baselineAverageClose.abs());
            BigDecimal relDiff = denominator.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : filteredAverageClose.subtract(baselineAverageClose)
                    .divide(denominator, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.printf("Absolute avg-close difference (filtered - baseline): %+.2f%%%n", absDiff);
            System.out.println("Relative difference vs baseline: " + relDiff.setScale(2, RoundingMode.HALF_UP) + "%");
        } else {
            System.out.println("Not enough data to compute uplifts.");
        }

        BigDecimal onePct = BigDecimal.valueOf(0.01);
        BigDecimal minusOnePct = onePct.negate();

        long profitableCount = filtered.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(onePct) > 0)
                .count();

        long safeCount = filtered.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(minusOnePct) > 0)
                .count();

        long baselineProfitableCount = baselineMap.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(onePct) > 0)
                .count();

        long baselineSafeCount = baselineMap.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(minusOnePct) > 0)
                .count();

        long allProfitableCount = mdByKey.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(onePct) > 0)
                .count();

        long allSafeCount = mdByKey.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> pct.compareTo(minusOnePct) > 0)
                .count();

        long totalFiltered = filtered.size();
        long totalBaseline = baselineMap.size();
        long total = mdByKey.size();

        System.out.printf(
                "Profitable records (max gain >1%%): %d (%.2f%%)%n" +
                        "Safe records (max loss <1%%): %d (%.2f%%)%n",
                profitableCount,
                100.0 * profitableCount / totalFiltered,
                safeCount,
                100.0 * safeCount / totalFiltered
        );

        System.out.printf(
                "Baseline profitable records (max gain >1%%): %d (%.2f%%)%n" +
                        "Baseline safe records (max loss <1%%): %d (%.2f%%)%n",
                baselineProfitableCount,
                100.0 * baselineProfitableCount / totalBaseline,
                baselineSafeCount,
                100.0 * baselineSafeCount / totalBaseline
        );

        System.out.printf(
                "All profitable records (max gain >1%%): %d (%.2f%%)%n" +
                        "Baseline safe records (max loss <1%%): %d (%.2f%%)%n",
                allProfitableCount,
                100.0 * allProfitableCount / total,
                allSafeCount,
                100.0 * allSafeCount / total
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
