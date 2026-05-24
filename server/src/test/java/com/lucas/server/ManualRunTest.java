package com.lucas.server;

import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.server.components.tradingbot.common.DailyScheduler;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolRepository;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataRepository;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketSnapshotRepository;
import com.lucas.server.components.tradingbot.marketdata.mapper.MarketDataMapper;
import com.lucas.server.components.tradingbot.marketdata.mapper.MarketSnapshotMapper;
import com.lucas.server.components.tradingbot.recommendation.jpa.Recommendation;
import com.lucas.server.components.tradingbot.recommendation.jpa.RecommendationsRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.BUY;
import static com.lucas.server.common.Constants.NY_ZONE;
import static com.lucas.server.common.Constants.RecommendationMode;
import static com.lucas.server.common.Constants.Sector;
import static com.lucas.server.common.Constants.filterClients;
import static com.lucas.server.common.Constants.isTradingDate;
import static com.lucas.server.common.Constants.toPastOrFutureTradeDate;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic program runner. Run containers manually and pass production env vars as env vars
 *
 * <p>Model correlation tests are offset so if you run them before market open in the morning
 * they will all represent data available at previous date and before,
 * meaning nth day assertions will be done on nth day before yesterday recs.
 *
 * <p>Running the program at 9:00 UTC with <code>FROM</code> as yesterday will yield one day of data for each test
 *
 * <p>It is not the goal of the model correlation tests to measure sector strength.
 * Sector is controlled, removing companies from other sectors from the calculations.
 * The goal is instead to measure how good the model is at predicting growth
 * among the companies of a particular sector.
 * In short, which sectors is the model most right about.
 */
@TestPropertySource(properties = "spring.jpa.show-sql=false")
@Disabled("Manual run only")
@SuppressWarnings("java:S5976")
class ManualRunTest extends BaseTest {

    private static final Set<String> SYMBOL_NAMES =
            Set.of("AAPL", "NVDA", "MSFT", "AMZN", "META", "TSLA", "GOOGL", "GOOG", "IBM");
    private static final LocalDate FROM = LocalDate.of(2026, 4, 25); // inclusive
    private static final LocalDate TO = LocalDate.now().plusDays(1); // exclusive
    private static Set<Recommendation> allRecommendations;
    private static Set<MarketDataDomain> allMarketData;

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

    @Autowired
    private Map<String, AiClient> allClients;

    @Autowired
    private MarketDataMapper marketDataMapper;

    @Autowired
    private MarketSnapshotMapper marketSnapshotMapper;

    @Test
    void runMidnightTask() throws Exception {
        assertTrue(true);
        Method method = DailyScheduler.class.getDeclaredMethod("doMidnightTask", Set.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, SYMBOL_NAMES);
    }

    @Test
    void runMorningTask() throws Exception {
        assertTrue(true);
        Method method = DailyScheduler.class.getDeclaredMethod("doMorningTask", Set.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, SYMBOL_NAMES);
    }

    @Test
    void runEarlyMorningTask() throws Exception {
        assertTrue(true);
        Method method = DailyScheduler.class.getDeclaredMethod("doEarlyMorningTask", Set.class);
        method.setAccessible(true);
        method.invoke(dailyScheduler, SYMBOL_NAMES);
    }

    @Test
    void runBeforeMorningTask() throws Exception {
        assertTrue(true);
        Method method = DailyScheduler.class.getDeclaredMethod("doBeforeMorningTask", Set.class);
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
    @Transactional(readOnly = true)
    void assertRecommendationsPrecisionAtCloseOfNthDay(int daysAfter, Sector sector) {
        assertTrue(true);

        initializeMarketData();
        Set<Long> allSymbolIds = getSymbolIdsFor(sector);
        LocalDate from = toPastOrFutureTradeDate(FROM, daysAfter, d -> d.minusDays(1));
        LocalDate to = toPastOrFutureTradeDate(TO, daysAfter, d -> d.minusDays(1));
        Set<LocalDate> dates = from.datesUntil(to).collect(Collectors.toUnmodifiableSet());

        Map<SymDate, MarketDataDomain> mdByKey = allMarketData.stream()
                .filter(md -> allSymbolIds.contains(md.getSymbol().getId()) && dates.contains(md.getDate()))
                .collect(Collectors.toUnmodifiableMap(md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()));

        Set<Long> symbolsNeeded =
                mdByKey.values().stream().map(snap -> snap.getSymbol().getId()).collect(Collectors.toUnmodifiableSet());
        LocalDate firstDateForCurrentPriceNeeded = mdByKey.values()
                .stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate(), daysAfter, d -> d.plusDays(1)))
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate lastDateForCurrentPriceNeeded = mdByKey.values()
                .stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate(), daysAfter, d -> d.plusDays(1)).plusDays(1))
                .max(Comparator.naturalOrder())
                .orElseThrow();

        Set<LocalDate> nextDatesWindow = firstDateForCurrentPriceNeeded.datesUntil(lastDateForCurrentPriceNeeded)
                .collect(Collectors.toUnmodifiableSet());

        Map<SymDate, MarketDataDomain> nextDayMd = allMarketData.stream()
                .filter(md -> symbolsNeeded.contains(md.getSymbol().getId()) && nextDatesWindow.contains(md.getDate()))
                .collect(Collectors.toUnmodifiableMap(md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()));

        Map<SymDate, MarketDataDomain> mdsWithNextDayPrice = mdByKey.entrySet()
                .stream()
                .flatMap(e -> Optional.ofNullable(nextDayMd.get(new SymDate(e.getKey().symbolId(),
                                toPastOrFutureTradeDate(e.getKey().date, daysAfter, d -> d.plusDays(1)))))
                        .map(next -> Map.entry(e.getKey(),
                                new MarketDataDomain().setSymbol(e.getValue().getSymbol())
                                        .setDate(e.getValue().getDate())
                                        .setOpen(e.getValue().getOpen())
                                        .setHigh(e.getValue().getHigh().max(next.getHigh()))
                                        .setLow(e.getValue().getLow().min(next.getLow()))
                                        .setPrice(next.getPrice())
                                        .setVolume(e.getValue().getVolume() + next.getVolume())
                                        .setPreviousClose(e.getValue().getPreviousClose())))
                        .stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        processAndPrintResults(mdsWithNextDayPrice, from, to, sector, "close");
    }

    @ParameterizedTest
    @MethodSource("timesAndSectors")
    @Transactional(readOnly = true)
    void assertRecommendationsPrecisionAtSnapshot(LocalTime time, Sector sector) {
        assertTrue(true);

        initializeMarketData();
        Set<Long> allSymbolIds = getSymbolIdsFor(sector);
        Set<LocalDate> dates = FROM.datesUntil(TO).collect(Collectors.toUnmodifiableSet());

        Map<SymDate, MarketSnapshotDomain> msByKey = dates.stream()
                .flatMap(date -> marketSnapshotRepository.findAllBySymbol_IdInAndDateBetween(allSymbolIds,
                        date.atTime(time)
                                .minusMinutes(4)
                                .atZone(NY_ZONE)
                                .withZoneSameInstant(ZoneOffset.UTC)
                                .toLocalDateTime(),
                        date.atTime(time)
                                .plusMinutes(4)
                                .atZone(NY_ZONE)
                                .withZoneSameInstant(ZoneOffset.UTC)
                                .toLocalDateTime()).stream().map(marketSnapshotMapper::toDto))
                .collect(Collectors.toUnmodifiableMap(ms -> new SymDate(ms.getSymbol().getId(),
                        ms.getDate().toLocalDate()), Function.identity(), (a, b) -> a));

        Set<Long> symbolsNeeded =
                msByKey.values().stream().map(snap -> snap.getSymbol().getId()).collect(Collectors.toUnmodifiableSet());
        LocalDate firstDateForPreviousCloseNeeded = msByKey.values()
                .stream()
                .map(snap -> toPastOrFutureTradeDate(snap.getDate().toLocalDate(), 1, d -> d.minusDays(1)))
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate lastDateForPreviousCloseNeeded = msByKey.values()
                .stream()
                .map(snap -> snap.getDate().toLocalDate())
                .max(Comparator.naturalOrder())
                .orElseThrow();

        Set<LocalDate> previousDatesWindow = firstDateForPreviousCloseNeeded.datesUntil(lastDateForPreviousCloseNeeded)
                .collect(Collectors.toUnmodifiableSet());

        Map<SymDate, MarketDataDomain> previousDayMd = allMarketData.stream()
                .filter(md -> symbolsNeeded.contains(md.getSymbol().getId())
                              && previousDatesWindow.contains(md.getDate()))
                .collect(Collectors.toUnmodifiableMap(md -> new SymDate(md.getSymbol().getId(), md.getDate()),
                        Function.identity()));

        Map<SymDate, MarketDataDomain> msAsMdByKey = msByKey.entrySet()
                .stream()
                .flatMap(e -> Optional.ofNullable(previousDayMd.get(new SymDate(e.getKey().symbolId(),
                                toPastOrFutureTradeDate(e.getKey().date(), 1, d -> d.minusDays(1)))))
                        .map(prev -> Map.entry(e.getKey(),
                                MarketDataDomain.from(e.getValue()).setPreviousClose(prev.getPrice())))
                        .stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        processAndPrintResults(msAsMdByKey, FROM, TO, sector, time.toString());
    }

    private static Stream<Arguments> daysAndSectors() {
        return IntStream.of(0, 1, 2, 3, 4, 5)
                .boxed()
                .flatMap(day -> sectorsWithNullFirst().map(sector -> Arguments.of(day, sector)));
    }

    private static Stream<Sector> sectorsWithNullFirst() {
        return Stream.concat(Stream.of((Sector) null), Arrays.stream(Sector.values()));
    }

    private static Stream<Arguments> timesAndSectors() {
        return Stream.of(LocalTime.of(9, 35), LocalTime.of(9, 45))
                .flatMap(time -> sectorsWithNullFirst().map(sector -> Arguments.of(time, sector)));
    }

    private static Stats computeStats(Set<MarketDataDomain> mds) {
        return mds.stream().map(d -> {
            BigDecimal open = d.getOpen();
            BigDecimal close = d.getPrice().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
            BigDecimal high = d.getHigh().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
            BigDecimal low = d.getLow().subtract(open).divide(open, 8, RoundingMode.HALF_UP);
            return Stats.of(close, high, low);
        }).reduce(Stats::combine).orElse(null);
    }

    private static String summaryOf(Stats s, String label, String exitLabel) {
        if (null == s || 0 == s.count) {
            return label + ": (no data)";
        }
        BigDecimal cnt = BigDecimal.valueOf(s.count);
        BigDecimal avgClose = s.sumClose.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgHigh = s.sumHigh.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal avgLow = s.sumLow.divide(cnt, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return String.format("%s%n" + "Sample size: %d%n" + "Avg %s profit: %+.2f%%%n" + "Avg max profit: %+.2f%%%n"
                             + "Avg max loss: %+.2f%%%n" + "Max gain at %s: %+.2f%%%n" + "Max loss at %s: %+.2f%%%n"
                             + "Max possible gain: %+.2f%%%n" + "Min possible gain: %+.2f%%%n"
                             + "Min possible loss: %+.2f%%%n" + "Max possible loss: %+.2f%%",
                label,
                s.count,
                exitLabel,
                avgClose,
                avgHigh,
                avgLow,
                exitLabel,
                s.maxClose.multiply(BigDecimal.valueOf(100)),
                exitLabel,
                s.minClose.multiply(BigDecimal.valueOf(100)),
                s.maxHigh.multiply(BigDecimal.valueOf(100)),
                s.minHigh.multiply(BigDecimal.valueOf(100)),
                s.maxLow.multiply(BigDecimal.valueOf(100)),
                s.minLow.multiply(BigDecimal.valueOf(100)));
    }

    /**
     * Pre-loads all market data needed across all possible test parameter combinations.
     * Covers all symbols and dates from daysAfter=0 to daysAfter=5 (6 variations)
     * and times 9:35 and 9:45 with all sectors.
     * Note: If test input parameters change (e.g., daysAndSectors returns different values),
     * update the tradeDaysElapsed value (currently 5) to match the maximum daysAfter parameter.
     */
    private void initializeMarketData() {
        if (null == allMarketData) {
            Set<Long> allSymbolIds =
                    symbolRepository.findAll().stream().map(Symbol::getId).collect(Collectors.toUnmodifiableSet());

            LocalDate earliestFrom = toPastOrFutureTradeDate(FROM, 5, d -> d.minusDays(1));
            LocalDate latestToWithLookahead = toPastOrFutureTradeDate(TO, 5, d -> d.plusDays(1));

            Set<LocalDate> allDates =
                    earliestFrom.datesUntil(latestToWithLookahead).collect(Collectors.toUnmodifiableSet());
            allMarketData = Set.copyOf(marketDataRepository.findBySymbol_IdInAndDateIn(allSymbolIds, allDates))
                    .stream()
                    .map(marketDataMapper::toDto)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * Pre-loads all recommendations needed across all possible test parameter combinations.
     * Covers date range from daysAfter=0 to daysAfter=5 (6 variations) with all sectors.
     * Note: If test input parameters change (e.g., daysAndSectors returns different values),
     * update the tradeDaysElapsed value (currently 5) to match the maximum daysAfter parameter.
     */
    private void initializeRecommendations() {
        if (null == allRecommendations) {
            LocalDate earliestFrom = toPastOrFutureTradeDate(FROM, 5, d -> d.minusDays(1));
            LocalDate latestTo = toPastOrFutureTradeDate(TO, 0, d -> d.minusDays(1));

            allRecommendations = Set.copyOf(recommendationsRepository.findByDateBetween(earliestFrom, latestTo));
        }
    }

    private Set<Long> getSymbolIdsFor(Sector sector) {
        return symbolRepository.findAll()
                .stream()
                .filter(s -> null == sector || sector.equals(s.getSector()))
                .map(Symbol::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void processAndPrintResults(Map<SymDate, MarketDataDomain> mdByKey,
                                        LocalDate from,
                                        LocalDate to,
                                        Sector sector,
                                        String exitLabel) {
        System.out.println("=================================================");
        System.out.println("SECTOR: " + (null == sector ? "GLOBAL" : sector.name()));
        System.out.println("Range: " + from + " - " + to);
        System.out.println("-------------------------------------------------");

        Map<Recommendation, MarketDataDomain> baseline = getBaseline(mdByKey, from, to);
        Map<Recommendation, MarketDataDomain> filtered = getFiltered(baseline);

        Stats filteredStats = computeStats(Set.copyOf(filtered.values()));
        Stats allStats = computeStats(Set.copyOf(mdByKey.values()));

        if (null != filteredStats && 0 < filteredStats.count && null != allStats && 0 < allStats.count) {
            BigDecimal filteredAverageClose =
                    filteredStats.sumClose.divide(BigDecimal.valueOf(filteredStats.count), 8, RoundingMode.HALF_UP);
            BigDecimal allAverageClose =
                    allStats.sumClose.divide(BigDecimal.valueOf(allStats.count), 8, RoundingMode.HALF_UP);

            BigDecimal absDiff = filteredAverageClose.subtract(allAverageClose).multiply(BigDecimal.valueOf(100));
            BigDecimal denominator = filteredAverageClose.abs().max(allAverageClose.abs());
            BigDecimal relDiff = 0 == denominator.compareTo(BigDecimal.ZERO)
                    ? BigDecimal.ZERO
                    : filteredAverageClose.subtract(allAverageClose)
                            .divide(denominator, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

            System.out.printf("Absolute avg-%s difference (filtered - all): %+.2f%%%n", exitLabel, absDiff);
            System.out.println("Relative difference vs all: " + relDiff.setScale(2, RoundingMode.HALF_UP) + "%");
        } else {
            System.out.println("Not enough data to compute uplifts.");
        }

        System.out.println(summaryOf(filteredStats, "FILTERED", exitLabel));
        System.out.println(summaryOf(allStats, "ALL", exitLabel));
        System.out.println(summaryOf(computeStats(Set.copyOf(baseline.values())), "BASELINE", exitLabel));

        BigDecimal onePct = BigDecimal.valueOf(0.01);
        BigDecimal minusOnePct = onePct.negate();

        long profitableCount = filtered.values()
                .stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long safeCount = filtered.values()
                .stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long baselineProfitableCount = baseline.values()
                .stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long baselineSafeCount = baseline.values()
                .stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long allProfitableCount = mdByKey.values()
                .stream()
                .map(md -> md.getHigh().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(onePct))
                .count();

        long allSafeCount = mdByKey.values()
                .stream()
                .map(md -> md.getLow().subtract(md.getOpen()).divide(md.getOpen(), 8, RoundingMode.HALF_UP))
                .filter(pct -> 0 < pct.compareTo(minusOnePct))
                .count();

        long totalFiltered = filtered.size();
        long totalBaseline = baseline.size();
        long total = mdByKey.size();

        System.out.printf(
                "Profitable records (max gain >1%%): %d (%.2f%%)%n" + "Safe records (max loss <1%%): %d (%.2f%%)%n",
                profitableCount,
                100.0 * profitableCount / totalFiltered,
                safeCount,
                100.0 * safeCount / totalFiltered);

        System.out.printf("Baseline profitable records (max gain >1%%): %d (%.2f%%)%n"
                          + "Baseline safe records (max loss <1%%): %d (%.2f%%)%n",
                baselineProfitableCount,
                100.0 * baselineProfitableCount / totalBaseline,
                baselineSafeCount,
                100.0 * baselineSafeCount / totalBaseline);

        System.out.printf("All profitable records (max gain >1%%): %d (%.2f%%)%n"
                          + "Baseline safe records (max loss <1%%): %d (%.2f%%)%n",
                allProfitableCount,
                100.0 * allProfitableCount / total,
                allSafeCount,
                100.0 * allSafeCount / total);
    }

    private Map<Recommendation, MarketDataDomain> getBaseline(Map<SymDate, MarketDataDomain> mdByKey,
                                                              LocalDate from,
                                                              LocalDate to) {
        initializeRecommendations();

        return allRecommendations.stream()
                .filter(r -> !r.getDate().isBefore(from) && r.getDate().isBefore(to))
                .filter(r -> isTradingDate(r.getDate()))
                .flatMap(r -> Optional.ofNullable(mdByKey.get(new SymDate(r.getSymbol().getId(), r.getDate())))
                        .map(md -> Map.entry(r, md))
                        .stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Recommendation, MarketDataDomain> getFiltered(Map<Recommendation, MarketDataDomain> baseline) {
        return baseline.entrySet()
                .stream()
                // It is also possible to filter by gap, for instance (open - previousClose, as percentage)
                .filter(e -> BUY.equals(e.getKey().getAction()) && 0 <= e.getKey()
                        .getConfidence()
                        .compareTo(BigDecimal.valueOf(0.8)) && filterClients(allClients,
                        RecommendationMode.FINE_GRAIN).stream()
                                     .map(c -> c.getConfig().name())
                                     .collect(Collectors.toUnmodifiableSet())
                                     .contains(e.getKey().getModel()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    record SymDate(Long symbolId, LocalDate date) {
    }

    record Stats(BigDecimal sumClose,
                 BigDecimal sumHigh,
                 BigDecimal sumLow,
                 BigDecimal maxClose,
                 BigDecimal minClose,
                 BigDecimal maxHigh,
                 BigDecimal minHigh,
                 BigDecimal maxLow,
                 BigDecimal minLow,
                 long count) {
        static Stats of(BigDecimal close, BigDecimal high, BigDecimal low) {
            return new Stats(close, high, low, close, close, high, high, low, low, 1);
        }

        Stats combine(Stats o) {
            return new Stats(sumClose.add(o.sumClose),
                    sumHigh.add(o.sumHigh),
                    sumLow.add(o.sumLow),
                    maxClose.max(o.maxClose),
                    minClose.min(o.minClose),
                    maxHigh.max(o.maxHigh),
                    minHigh.min(o.minHigh),
                    maxLow.max(o.maxLow),
                    minLow.min(o.minLow),
                    count + o.count);
        }
    }
}
