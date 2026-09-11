package com.lucas.server.common;

import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Constants {

    public static final int SUDOKU_SIZE = 9;
    public static final int SUDOKU_NUMBER_OF_CELLS = 81;
    public static final int MARKET_DATA_RELEVANT_DAYS_COUNT = 34;
    public static final int HISTORY_DAYS_COUNT = 10;
    public static final int NEWS_COUNT = 12;
    public static final int REQUEST_MAX_ATTEMPTS = 2;
    public static final String SPECIALIST = "-specialist";
    public static final String DEFAULT_USERNAME = "default";
    public static final String INVALID_EXPRESSION = "Invalid expression";
    public static final String QUOTE = "/quote";
    public static final String SYMBOL = "symbol";
    public static final String CONTENT = "content";
    public static final String BUY = "BUY";
    public static final String MARKET_DATA = "market data";
    public static final String MARKET_SNAPSHOT = "market snapshot";
    public static final String NEWS = "news";
    public static final String SENTIMENT = "sentiment";
    public static final String RECOMMENDATION = "recommendation";
    public static final String VOLATILITY = "volatility";
    public static final String TWELVEDATA_RATE_LIMITER = "twelveDataRateLimiter";
    public static final String YAHOO_FINANCE_RATE_LIMITER = "yahooFinanceRateLimiter";
    public static final String KPI_RETURNED_ZERO_WARN = "Value is zero for {}, {}";
    public static final String NON_COMPUTABLE_KPI_WARN = "Error attempting to compute {} for {}";
    public static final String RETRIEVAL_FAILED_WARN = "Error generating {} {}";
    public static final String RETRIEVING_DATA_INFO = "Retrieving {} for {}";
    public static final String NO_YAHOO_NEWS_ERROR = "No news found in document {0}";
    public static final String MAPPING_ERROR = "Error mapping {0}";
    public static final String AMERICA_NY = "America/New_York";
    public static final ZoneId NY_ZONE = ZoneId.of(AMERICA_NY);
    public static final String UTC = "UTC";
    public static final ZoneId UTC_ZONE = ZoneId.of(UTC);
    // for testing purposes
    public static final LocalDateTime FIXED_DATE = LocalDate.of(2025, Month.JANUARY, 1).atStartOfDay();
    public static final Set<String> GEMINI_3_8_FLASH_CLIENTS = Set.of("gemini-3.8-flash",
            "gemini-3.8-flash_2",
            "gemini-3.8-flash_3",
            "gemini-3.8-flash_4",
            "gemini-3.8-flash_5",
            "gemini-3.8-flash_6");
    public static final Set<String> GEMINI_3_7_FLASH_CLIENTS = Set.of("gemini-3.7-flash",
            "gemini-3.7-flash_2",
            "gemini-3.7-flash_3",
            "gemini-3.7-flash_4",
            "gemini-3.7-flash_5",
            "gemini-3.7-flash_6");
    public static final Set<String> GEMINI_3_6_FLASH_CLIENTS = Set.of("gemini-3.6-flash",
            "gemini-3.6-flash_2",
            "gemini-3.6-flash_3",
            "gemini-3.6-flash_4",
            "gemini-3.6-flash_5",
            "gemini-3.6-flash_6");
    public static final Set<String> GEMINI_3_5_FLASH_CLIENTS = Set.of("gemini-3.5-flash",
            "gemini-3.5-flash_2",
            "gemini-3.5-flash_3",
            "gemini-3.5-flash_4",
            "gemini-3.5-flash_5",
            "gemini-3.5-flash_6");
    public static final Set<String> GEMINI_LITE_CLIENTS = Set.of("gemini-3.5-flash-lite",
            "gemini-3.5-flash-lite_2",
            "gemini-3.5-flash-lite_3",
            "gemini-3.5-flash-lite_4",
            "gemini-3.5-flash-lite_5",
            "gemini-3.5-flash-lite_6");
    public static final Set<String> OPENROUTER_CLIENTS =
            Set.of("openrouter", "openrouter_2", "openrouter_3", "openrouter_4", "openrouter_5");
    public static final Set<String> FINE_GRAIN_CLIENT_NAMES = Stream.of(GEMINI_3_8_FLASH_CLIENTS,
            GEMINI_3_7_FLASH_CLIENTS,
            GEMINI_3_6_FLASH_CLIENTS,
            GEMINI_3_5_FLASH_CLIENTS).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    // If a model here has a specialist version, that one will be assumed to output a thinking block too
    private static final Set<String> CLIENTS_WITH_THINKING_BLOCK = Set.of();
    private static final String FINNHUB_RATE_LIMITER = "finnhubRateLimiter";
    private static final String FINNHUB_RATE_LIMITER_2 = "finnhubRateLimiter2";
    private static final String FINNHUB_RATE_LIMITER_3 = "finnhubRateLimiter3";
    private static final String FINNHUB_RATE_LIMITER_4 = "finnhubRateLimiter4";
    private static final OrderedIndexedSet<String> FINNHUB_RATE_LIMITERS = OrderedIndexedSet.of(FINNHUB_RATE_LIMITER,
            FINNHUB_RATE_LIMITER_2,
            FINNHUB_RATE_LIMITER_3,
            FINNHUB_RATE_LIMITER_4);
    private static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final Set<LocalDate> MARKET_HOLIDAYS_2026 =
            Set.of(LocalDate.of(2026, Month.JANUARY, 1), // New Year's Day
                    LocalDate.of(2026, Month.JANUARY, 19), // Martin Luther King Jr. Day
                    LocalDate.of(2026, Month.FEBRUARY, 16), // Presidents' Day
                    LocalDate.of(2026, Month.APRIL, 3), // Good Friday
                    LocalDate.of(2026, Month.MAY, 25), // Memorial Day
                    LocalDate.of(2026, Month.JUNE, 19), // Juneteenth Day
                    LocalDate.of(2026, Month.JULY, 3), // Independence Day
                    LocalDate.of(2026, Month.SEPTEMBER, 7), // Labor Day
                    LocalDate.of(2026, Month.NOVEMBER, 26), // Thanksgiving
                    LocalDate.of(2026, Month.DECEMBER, 25)  // Christmas Day
            );
    private static final Map<String, String> ENTITY_MAP = Map.ofEntries(Map.entry("&quot;", "\""),
            Map.entry("&#39;", "'"),
            Map.entry("&amp;", "&"),
            Map.entry("&lt;", "<"),
            Map.entry("&gt;", ">"),
            Map.entry("&nbsp;", " "),
            Map.entry("&cent;", "¢"),
            Map.entry("&pound;", "£"),
            Map.entry("&yen;", "¥"),
            Map.entry("&euro;", "€"),
            Map.entry("&copy;", "©"),
            Map.entry("&reg;", "®"),
            Map.entry("&trade;", "™"),
            Map.entry("&bull;", "•"),
            Map.entry("&mdash;", "—"),
            Map.entry("&ndash;", "–"),
            Map.entry("&hellip;", "…"),
            Map.entry("&lsquo;", "‘"),
            Map.entry("&rsquo;", "’"),
            Map.entry("&ldquo;", "“"),
            Map.entry("&rdquo;", "”"));

    @SuppressWarnings("unused")
    private Constants() {
    }

    public static String sanitizeHtml(String input) {
        if (null == input) {
            return null;
        }
        String result = input;
        for (Map.Entry<String, String> e : ENTITY_MAP.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    public static Set<String> getModelsWithThinkingBlock() {
        return CLIENTS_WITH_THINKING_BLOCK.stream()
                .flatMap(client -> Stream.of(client, client + SPECIALIST))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<AiClient> filterClients(Map<String, AiClient> allClients, Set<String> clientNames) {
        return clientNames.stream()
                .map(name -> Optional.ofNullable(allClients.get(name)).orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static OrderedIndexedSet<String> getFinnhubRateLimiterNames() {
        return FINNHUB_RATE_LIMITERS;
    }

    public static LocalDate toPastOrFutureTradeDate(LocalDate start,
                                                    int tradeDaysElapsed,
                                                    UnaryOperator<LocalDate> step) {
        LocalDate d = start;
        for (int i = 0; i < tradeDaysElapsed; i++) {
            do {
                d = step.apply(d);
            } while (!isTradingDate(d));
        }
        return d;
    }

    public static boolean isTradingDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return !DayOfWeek.SATURDAY.equals(dayOfWeek) && !DayOfWeek.SUNDAY.equals(dayOfWeek)
               && !MARKET_HOLIDAYS_2026.contains(date);
    }

    public static int[] getDigits() {
        return DIGITS;
    }

    public enum AlgorithmKind {
        EDGE,
        CORNER,
        PARITY
    }

    public enum MarketDataType {
        LAST,
        HISTORIC,
        REAL_TIME
    }

    public enum PortfolioType {
        REAL,
        MOCK
    }

    public enum AiProvider {
        OPENROUTER,
        GOOGLE
    }
}
