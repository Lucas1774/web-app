package com.lucas.server;

import com.lucas.server.components.tradingbot.common.DailyScheduler;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolRepository;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataRepository;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshot;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshotRepository;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic program runner. Run containers manually and pass production env vars as env vars
 * <p>
 * Model correlation tests are offset so if you run all three before market open in the morning
 * they will all represent data available at previous date and before,
 * meaning nth day assertions will be done on nth day before yesterday recs.
 * <p>
 * Running the program at 9:00 UTC with <code>FROM</code> as yesterday will yield one day of data for each test
 * <p>
 * It is not the goal of the model correlation tests to measure sector strength.
 * Sector is controlled, removing companies from other sectors from the calculations.
 * The goal is instead to measure how good the model is at predicting growth
 * among the companies of a particular sector.
 * In short, which sectors is the model most right about.
 */
@TestPropertySource(properties = "spring.jpa.show-sql=false")
@Disabled("Manual run only")
class ManualRunTest extends BaseTest {

    private static final Set<String> SYMBOL_NAMES = Set.of("AAPL", "NVDA", "MSFT", "AMZN", "META", "TSLA", "GOOGL");
    private static final LocalDate FROM = LocalDate.of(2025, 12, 1); // inclusive
    private static final LocalDate TO = LocalDate.now().plusDays(1); // exclusive

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private RecommendationsRepository recommendationsRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private MarketSnapshotRepository marketSnapshotRepository;

    @Autowired
    private DailyScheduler dailyScheduler;

    private static Stats computeStats(Set<MarketData> mds) {
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
        if (null == s || 0 == s.count) return label + ": (no data)";
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

    private static LocalDate toPastOrFutureTradeDate(LocalDate start, int tradeDaysElapsed, UnaryOperator<LocalDate> step) {
        LocalDate d = start;
        for (int i = 0; i < tradeDaysElapsed; i++) {
            do {
                d = step.apply(d);
            } while (!isTradingDate(d));
        }
        return d;
    }

    private static Stream<Sector> sectorsWithNullFirst() {
        return Stream.concat(Stream.of((Sector) null), Arrays.stream(Sector.values()));
    }

    private static Stream<Arguments> daysAndSectors() {
        return IntStream.of(0, 1, 2, 3)
                .boxed()
                .flatMap(day -> sectorsWithNullFirst().map(sector -> Arguments.of(day, sector)));
    }

    private static Stream<Arguments> timesAndSectors() {
        return Stream.of(LocalTime.of(9, 35), LocalTime.of(9, 45))
                .flatMap(time -> sectorsWithNullFirst().map(sector -> Arguments.of(time, sector)));
    }

    @Test
    @Transactional
    void runMidnightTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMidnightTask", Set.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, SYMBOL_NAMES);
    }

    @Test
    @Transactional
    void runMorningTask() throws Exception {
        assertTrue(true); // useless assertion so Sonar doesn't cry
        Method method = DailyScheduler.class.getDeclaredMethod("doMorningTask", Set.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, SYMBOL_NAMES);
    }

    /**
     * High, low and volume (unused) lose correctness for values higher than 1
     *
     * @param daysAfter days after. 0 would be the day of the recommendation.
     */
    @ParameterizedTest
    @MethodSource("daysAndSectors")
    @Transactional
    void assertRecommendationsPrecisionAtCloseOfNthDay(int daysAfter, Sector sector) {
        assertTrue(true); // useless assertion so Sonar doesn't cry

        Set<Long> allSymbolIds = getSymbolIdsFor(sector);
        LocalDate from = toPastOrFutureTradeDate(FROM, daysAfter, d -> d.minusDays(1));
        LocalDate to = toPastOrFutureTradeDate(TO, daysAfter, d -> d.minusDays(1));
        Set<LocalDate> dates = from.datesUntil(to).collect(Collectors.toSet());

        Map<SymDate, MarketData> mdByKey = marketDataRepository.findBySymbol_IdInAndDateIn(allSymbolIds, dates).stream()
                .collect(Collectors.toMap(
                        md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()
                ));

        Set<Long> symbolsNeeded = mdByKey.values().stream()
                .map(snap -> snap.getSymbol().getId())
                .collect(Collectors.toSet());
        LocalDate firstDateForCurrentPriceNeeded = mdByKey.values().stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate(), daysAfter, d -> d.plusDays(1)))
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate lastDateForCurrentPriceNeeded = mdByKey.values().stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate(), daysAfter, d -> d.plusDays(1)).plusDays(1))
                .max(Comparator.naturalOrder())
                .orElseThrow();

        Set<LocalDate> nextDatesWindow = firstDateForCurrentPriceNeeded.datesUntil(lastDateForCurrentPriceNeeded)
                .collect(Collectors.toSet());

        Map<SymDate, MarketData> nextDayMd = marketDataRepository.findBySymbol_IdInAndDateIn(symbolsNeeded, nextDatesWindow).stream()
                .collect(Collectors.toMap(
                        md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()
                ));

        Map<SymDate, MarketData> mdsWithNextDayPrice = mdByKey.entrySet().stream()
                .flatMap(e -> Optional.ofNullable(nextDayMd.get(
                                new SymDate(e.getKey().symbolId(), toPastOrFutureTradeDate(e.getKey().date, daysAfter, d -> d.plusDays(1)))
                        ))
                        .map(next -> Map.entry(e.getKey(), new MarketData().setSymbol(e.getValue().getSymbol())
                                .setDate(e.getValue().getDate())
                                .setOpen(e.getValue().getOpen())
                                .setHigh(e.getValue().getHigh().max(next.getHigh()))
                                .setLow(e.getValue().getLow().min(next.getLow()))
                                .setPrice(next.getPrice())
                                .setVolume(e.getValue().getVolume() + next.getVolume())
                                .setPreviousClose(e.getValue().getPreviousClose())
                        ))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        processAndPrintResults(mdsWithNextDayPrice, from, to, sector);
    }

    @ParameterizedTest
    @MethodSource("timesAndSectors")
    @Transactional
    void assertRecommendationsPrecisionAtSnapshot(LocalTime time, Sector sector) {
        assertTrue(true); // useless assertion so Sonar doesn't cry

        Set<Long> allSymbolIds = getSymbolIdsFor(sector);
        Set<LocalDate> dates = FROM.datesUntil(TO).collect(Collectors.toSet());

        Map<SymDate, MarketSnapshot> msByKey = dates.stream()
                .flatMap(date -> marketSnapshotRepository.findAllBySymbol_IdInAndDateBetween(allSymbolIds,
                                date.atTime(time).minusMinutes(4).atZone(NY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                                date.atTime(time).plusMinutes(4).atZone(NY_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .stream())
                .collect(Collectors.toMap(
                        ms -> new SymDate(ms.getSymbol().getId(), ms.getDate().toLocalDate()),
                        Function.identity(), (a, b) -> a, HashMap::new
                ));

        Set<Long> symbolsNeeded = msByKey.values().stream()
                .map(snap -> snap.getSymbol().getId())
                .collect(Collectors.toSet());
        LocalDate firstDateForPreviousCloseNeeded = msByKey.values().stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate().toLocalDate(), 1, d -> d.minusDays(1)))
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate lastDateForPreviousCloseNeeded = msByKey.values().stream()
                .map(snap -> snap.getDate().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElseThrow();

        Set<LocalDate> previousDatesWindow = firstDateForPreviousCloseNeeded.datesUntil(lastDateForPreviousCloseNeeded)
                .collect(Collectors.toSet());

        Map<SymDate, MarketData> previousDayMd = marketDataRepository.findBySymbol_IdInAndDateIn(symbolsNeeded, previousDatesWindow).stream()
                .collect(Collectors.toMap(
                        md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()
                ));

        Map<SymDate, MarketData> msAsMdByKey = msByKey.entrySet().stream()
                .flatMap(e -> Optional.ofNullable(previousDayMd.get(
                                new SymDate(e.getKey().symbolId(), toPastOrFutureTradeDate(e.getKey().date(), 1, d -> d.minusDays(1)))
                        ))
                        .map(prev -> Map.entry(e.getKey(), MarketData.from(e.getValue())
                                .setPreviousClose(prev.getPrice())))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        processAndPrintResults(msAsMdByKey, FROM, TO, sector);
    }

    private Set<Long> getSymbolIdsFor(Sector sector) {
        return symbolRepository.findAll().stream()
                .filter(s -> null == sector || sector.equals(s.getSector()))
                .map(Symbol::getId)
                .collect(Collectors.toSet());
    }

    private void processAndPrintResults(Map<SymDate, MarketData> mdByKey, LocalDate from, LocalDate to, Sector sector) {
        System.out.println("=================================================");
        System.out.println("SECTOR: " + (null == sector ? "GLOBAL" : sector.name()));
        System.out.println("Range: " + from + " - " + to);
        System.out.println("-------------------------------------------------");

        Map<Recommendation, MarketData> baseline = getBaseline(mdByKey, from, to);
        Map<Recommendation, MarketData> filtered = getFiltered(baseline);

        Stats filteredStats = computeStats(new HashSet<>(filtered.values()));
        Stats allStats = computeStats(new HashSet<>(mdByKey.values()));
        Stats baselineStats = computeStats(new HashSet<>(baseline.values()));

        if (null != filteredStats && 0 < filteredStats.count && null != allStats && 0 < allStats.count) {
            BigDecimal filteredAverageClose = filteredStats.sumClose.divide(BigDecimal.valueOf(filteredStats.count), 8, RoundingMode.HALF_UP);
            BigDecimal allAverageClose = allStats.sumClose.divide(BigDecimal.valueOf(allStats.count), 8, RoundingMode.HALF_UP);

            BigDecimal absDiff = filteredAverageClose.subtract(allAverageClose).multiply(BigDecimal.valueOf(100));
            BigDecimal denominator = filteredAverageClose.abs().max(allAverageClose.abs());
            BigDecimal relDiff = 0 == denominator.compareTo(BigDecimal.ZERO)
                    ? BigDecimal.ZERO
                    : filteredAverageClose.subtract(allAverageClose)
                    .divide(denominator, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.printf("Absolute avg-close difference (filtered - all): %+.2f%%%n", absDiff);
            System.out.println("Relative difference vs all: " + relDiff.setScale(2, RoundingMode.HALF_UP) + "%");
        } else {
            System.out.println("Not enough data to compute uplifts.");
        }

        System.out.println(summaryOf(filteredStats, "FILTERED"));
        System.out.println(summaryOf(allStats, "ALL"));
        System.out.println(summaryOf(baselineStats, "BASELINE"));

        BigDecimal onePct = BigDecimal.valueOf(0.01);
        BigDecimal minusOnePct = onePct.negate();

        long profitableCount = filtered.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long safeCount = filtered.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long baselineProfitableCount = baseline.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long baselineSafeCount = baseline.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long allProfitableCount = mdByKey.values().stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long allSafeCount = mdByKey.values().stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long totalFiltered = filtered.size();
        long totalBaseline = baseline.size();
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


    private Map<Recommendation, MarketData> getBaseline(Map<SymDate, MarketData> mdByKey, LocalDate from, LocalDate to) {
        return recommendationsRepository
                .findByDateBetween(from, to)
                .stream()
                .filter(r -> isTradingDate(r.getDate()))
                .flatMap(r -> Optional.ofNullable(mdByKey.get(new SymDate(r.getSymbol().getId(), r.getDate())))
                        .map(md -> Map.entry(r, md))
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Recommendation, MarketData> getFiltered(Map<Recommendation, MarketData> baseline) {
        return baseline.entrySet()
                .stream()
                .filter(e -> {
                    if (!BUY.equals(e.getKey().getAction()) || 0 > e.getKey().getConfidence().compareTo(BigDecimal.valueOf(0.75))) {
                        return false;
                    }
                    BigDecimal gapPct = e.getValue().getOpen()
                            .subtract(e.getValue().getPreviousClose())
                            .divide(e.getValue().getPreviousClose(), 10, RoundingMode.HALF_UP);
                    return 0 < gapPct.compareTo(BigDecimal.ZERO) && 0 > gapPct.compareTo(BigDecimal.valueOf(0.02));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    record SymDate(Long symbolId, LocalDate date) {
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
