package com.lucas.server.common;

import com.lucas.server.components.tradingbot.common.AIClient;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.Clients.*;
import static com.lucas.server.common.Constants.Sector.*;

public class Constants {

    public static final int SUDOKU_SIZE = 9;
    public static final int SUDOKU_NUMBER_OF_CELLS = 81;
    public static final int MARKET_DATA_RELEVANT_DAYS_COUNT = 34;
    public static final int HISTORY_DAYS_COUNT = 10;
    public static final int NEWS_COUNT = 12;
    public static final int DATABASE_NEWS_PER_SYMBOL = 20;
    public static final int DATABASE_MARKET_DATA_PER_SYMBOL = 100;
    public static final int DATABASE_RECOMMENDATIONS_PER_SYMBOL = 30;
    public static final int MAX_RECOMMENDATIONS_COUNT = 36;
    public static final int REQUEST_MAX_ATTEMPTS = 2;
    public static final int RECOMMENDATION_MAX_ATTEMPTS = 5;
    public static final BigDecimal RECOMMENDATION_MEDIUM_GRAIN_THRESHOLD = BigDecimal.valueOf(0.75);
    public static final BigDecimal RECOMMENDATION_FINE_GRAIN_THRESHOLD = BigDecimal.valueOf(0.75);
    public static final String DEFAULT_USERNAME = "default";
    public static final String NA = "N/A";
    public static final String INVALID_EXPRESSION = "Invalid expression";
    public static final String COMPANY_NEWS = "/company-news";
    public static final String QUOTE = "/quote";
    public static final String TIME_SERIES = "/time_series";
    public static final String ANALYZE = "/analyze";
    public static final String SYMBOL = "symbol";
    public static final String CONTENT = "content";
    public static final String ROLE = "role";
    public static final String BUY = "BUY";
    public static final String MARKET_DATA = "market data";
    public static final String MARKET_SNAPSHOT = "market snapshot";
    public static final String NEWS = "news";
    public static final String SENTIMENT = "sentiment";
    public static final String RECOMMENDATION = "recommendation";
    public static final String VOLATILITY = "volatility";
    public static final String OBV = "OBV";
    public static final String AI_PER_SECOND_RATE_LIMITER = "perSecondRateLimiter";
    public static final String TWELVEDATA_RATE_LIMITER = "twelveDataRateLimiter";
    public static final String YAHOO_FINANCE_RATE_LIMITER = "yahooFinanceRateLimiter";
    public static final String SUDOKU_IGNORED_MALFORMED_JSON_WARN = "Couldn't deserialize sudoku from raw data {}";
    public static final String KPI_RETURNED_ZERO_WARN = "Value is zero for {}, {}";
    public static final String NON_COMPUTABLE_KPI_WARN = "Error attempting to compute {} for {}";
    public static final String CLIENT_FAILED_BACKUP_WARN = "{} failed when trying to process {}";
    public static final String MARKET_STILL_OPEN_WARN = "Market is still open!";
    public static final String RETRIEVAL_FAILED_WARN = "Error generating {} {}";
    public static final String SCHEDULED_TASK_SUCCESS_INFO = "Successfully {}: {}";
    public static final String RETRIEVING_DATA_INFO = "Retrieving {} for {}";
    public static final String PROMPTING_MODEL_INFO = "Prompting model {}";
    public static final String GENERATION_SUCCESSFUL_INFO = "Successfully generated {}";
    public static final String NO_YAHOO_NEWS_ERROR = "No news found in document {0}";
    public static final String MAPPING_ERROR = "Error mapping {0}";
    public static final String INSUFFICIENT_STOCK_ERROR = "{0}: Nothing to sell";
    public static final String SYMBOL_NOT_FOUND_ERROR = "{0}: Unknown symbol";
    public static final String AMERICA_NY = "America/New_York";
    public static final ZoneId NY_ZONE = ZoneId.of(AMERICA_NY);
    public static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    public static final LocalTime EARLY_CLOSE = LocalTime.of(13, 0);
    // TODO: remove 2025 after Jan first 🥳
    public static final Set<LocalDate> EARLY_CLOSE_DATES_2025 = Set.of(
            LocalDate.of(2025, 7, 3), // Pre‑Independence Day
            LocalDate.of(2025, 11, 28), // Day after Thanksgiving
            LocalDate.of(2025, 12, 24), // Christmas Eve
            LocalDate.of(2026, 11, 27), // Day after Thanksgiving
            LocalDate.of(2026, 12, 24)  // Christmas Eve
    );
    public static final Map<String, Sector> SYMBOL_TO_SECTOR = Map.<String, Sector>ofEntries(Map.entry("MMM", INDUSTRIALS), Map.entry("AOS", INDUSTRIALS), Map.entry("ABT", HEALTH_CARE), Map.entry("ABBV", HEALTH_CARE), Map.entry("ACN", INFORMATION_TECHNOLOGY), Map.entry("ADBE", INFORMATION_TECHNOLOGY), Map.entry("AMD", INFORMATION_TECHNOLOGY), Map.entry("AES", UTILITIES), Map.entry("AFL", FINANCIALS), Map.entry("A", HEALTH_CARE), Map.entry("APD", MATERIALS), Map.entry("ABNB", CONSUMER_DISCRETIONARY), Map.entry("AKAM", INFORMATION_TECHNOLOGY), Map.entry("ALB", MATERIALS), Map.entry("ARE", REAL_ESTATE), Map.entry("ALGN", HEALTH_CARE), Map.entry("ALLE", INDUSTRIALS), Map.entry("LNT", UTILITIES), Map.entry("ALL", FINANCIALS), Map.entry("GOOGL", COMMUNICATION_SERVICES), Map.entry("GOOG", COMMUNICATION_SERVICES), Map.entry("MO", CONSUMER_STAPLES), Map.entry("AMZN", CONSUMER_DISCRETIONARY), Map.entry("AMCR", MATERIALS), Map.entry("AEE", UTILITIES), Map.entry("AEP", UTILITIES), Map.entry("AXP", FINANCIALS), Map.entry("AIG", FINANCIALS), Map.entry("AMT", REAL_ESTATE), Map.entry("AWK", UTILITIES), Map.entry("AMP", FINANCIALS), Map.entry("AME", INDUSTRIALS), Map.entry("AMGN", HEALTH_CARE), Map.entry("APH", INFORMATION_TECHNOLOGY), Map.entry("ADI", INFORMATION_TECHNOLOGY), Map.entry("AON", FINANCIALS), Map.entry("APA", ENERGY), Map.entry("APO", FINANCIALS), Map.entry("AAPL", INFORMATION_TECHNOLOGY), Map.entry("AMAT", INFORMATION_TECHNOLOGY), Map.entry("APP", INFORMATION_TECHNOLOGY), Map.entry("APTV", CONSUMER_DISCRETIONARY), Map.entry("ACGL", FINANCIALS), Map.entry("ADM", CONSUMER_STAPLES), Map.entry("ARES", FINANCIALS), Map.entry("ANET", INFORMATION_TECHNOLOGY), Map.entry("AJG", FINANCIALS), Map.entry("AIZ", FINANCIALS), Map.entry("T", COMMUNICATION_SERVICES), Map.entry("ATO", UTILITIES), Map.entry("ADSK", INFORMATION_TECHNOLOGY), Map.entry("ADP", INDUSTRIALS), Map.entry("AZO", CONSUMER_DISCRETIONARY), Map.entry("AVB", REAL_ESTATE), Map.entry("AVY", MATERIALS), Map.entry("AXON", INDUSTRIALS), Map.entry("BKR", ENERGY), Map.entry("BALL", MATERIALS), Map.entry("BAC", FINANCIALS), Map.entry("BAX", HEALTH_CARE), Map.entry("BDX", HEALTH_CARE), Map.entry("BRK.B", FINANCIALS), Map.entry("BBY", CONSUMER_DISCRETIONARY), Map.entry("TECH", HEALTH_CARE), Map.entry("BIIB", HEALTH_CARE), Map.entry("BLK", FINANCIALS), Map.entry("BX", FINANCIALS), Map.entry("XYZ", FINANCIALS), Map.entry("BK", FINANCIALS), Map.entry("BA", INDUSTRIALS), Map.entry("BKNG", CONSUMER_DISCRETIONARY), Map.entry("BSX", HEALTH_CARE), Map.entry("BMY", HEALTH_CARE), Map.entry("AVGO", INFORMATION_TECHNOLOGY), Map.entry("BR", INDUSTRIALS), Map.entry("BRO", FINANCIALS), Map.entry("BF.B", CONSUMER_STAPLES), Map.entry("BLDR", INDUSTRIALS), Map.entry("BG", CONSUMER_STAPLES), Map.entry("BXP", REAL_ESTATE), Map.entry("CHRW", INDUSTRIALS), Map.entry("CDNS", INFORMATION_TECHNOLOGY), Map.entry("CPT", REAL_ESTATE), Map.entry("CPB", CONSUMER_STAPLES), Map.entry("COF", FINANCIALS), Map.entry("CAH", HEALTH_CARE), Map.entry("CCL", CONSUMER_DISCRETIONARY), Map.entry("CARR", INDUSTRIALS), Map.entry("CVNA", CONSUMER_DISCRETIONARY), Map.entry("CAT", INDUSTRIALS), Map.entry("CBOE", FINANCIALS), Map.entry("CBRE", REAL_ESTATE), Map.entry("CDW", INFORMATION_TECHNOLOGY), Map.entry("COR", HEALTH_CARE), Map.entry("CNC", HEALTH_CARE), Map.entry("CNP", UTILITIES), Map.entry("CF", MATERIALS), Map.entry("CRL", HEALTH_CARE), Map.entry("SCHW", FINANCIALS), Map.entry("CHTR", COMMUNICATION_SERVICES), Map.entry("CVX", ENERGY), Map.entry("CMG", CONSUMER_DISCRETIONARY), Map.entry("CB", FINANCIALS), Map.entry("CHD", CONSUMER_STAPLES), Map.entry("CI", HEALTH_CARE), Map.entry("CINF", FINANCIALS), Map.entry("CTAS", INDUSTRIALS), Map.entry("CSCO", INFORMATION_TECHNOLOGY), Map.entry("C", FINANCIALS), Map.entry("CFG", FINANCIALS), Map.entry("CLX", CONSUMER_STAPLES), Map.entry("CME", FINANCIALS), Map.entry("CMS", UTILITIES), Map.entry("KO", CONSUMER_STAPLES), Map.entry("CTSH", INFORMATION_TECHNOLOGY), Map.entry("COIN", FINANCIALS), Map.entry("CL", CONSUMER_STAPLES), Map.entry("CMCSA", COMMUNICATION_SERVICES), Map.entry("FIX", INDUSTRIALS), Map.entry("CAG", CONSUMER_STAPLES), Map.entry("COP", ENERGY), Map.entry("ED", UTILITIES), Map.entry("STZ", CONSUMER_STAPLES), Map.entry("CEG", UTILITIES), Map.entry("COO", HEALTH_CARE), Map.entry("CPRT", INDUSTRIALS), Map.entry("GLW", INFORMATION_TECHNOLOGY), Map.entry("CPAY", FINANCIALS), Map.entry("CTVA", MATERIALS), Map.entry("CSGP", REAL_ESTATE), Map.entry("COST", CONSUMER_STAPLES), Map.entry("CTRA", ENERGY), Map.entry("CRH", MATERIALS), Map.entry("CRWD", INFORMATION_TECHNOLOGY), Map.entry("CCI", REAL_ESTATE), Map.entry("CSX", INDUSTRIALS), Map.entry("CMI", INDUSTRIALS), Map.entry("CVS", HEALTH_CARE), Map.entry("DHR", HEALTH_CARE), Map.entry("DRI", CONSUMER_DISCRETIONARY), Map.entry("DDOG", INFORMATION_TECHNOLOGY), Map.entry("DVA", HEALTH_CARE), Map.entry("DAY", INDUSTRIALS), Map.entry("DECK", CONSUMER_DISCRETIONARY), Map.entry("DE", INDUSTRIALS), Map.entry("DELL", INFORMATION_TECHNOLOGY), Map.entry("DAL", INDUSTRIALS), Map.entry("DVN", ENERGY), Map.entry("DXCM", HEALTH_CARE), Map.entry("FANG", ENERGY), Map.entry("DLR", REAL_ESTATE), Map.entry("DG", CONSUMER_STAPLES), Map.entry("DLTR", CONSUMER_STAPLES), Map.entry("D", UTILITIES), Map.entry("DPZ", CONSUMER_DISCRETIONARY), Map.entry("DASH", CONSUMER_DISCRETIONARY), Map.entry("DOV", INDUSTRIALS), Map.entry("DOW", MATERIALS), Map.entry("DHI", CONSUMER_DISCRETIONARY), Map.entry("DTE", UTILITIES), Map.entry("DUK", UTILITIES), Map.entry("DD", MATERIALS), Map.entry("ETN", INDUSTRIALS), Map.entry("EBAY", CONSUMER_DISCRETIONARY), Map.entry("ECL", MATERIALS), Map.entry("EIX", UTILITIES), Map.entry("EW", HEALTH_CARE), Map.entry("EA", COMMUNICATION_SERVICES), Map.entry("ELV", HEALTH_CARE), Map.entry("EME", INDUSTRIALS), Map.entry("EMR", INDUSTRIALS), Map.entry("ETR", UTILITIES), Map.entry("EOG", ENERGY), Map.entry("EPAM", INFORMATION_TECHNOLOGY), Map.entry("EQT", ENERGY), Map.entry("EFX", INDUSTRIALS), Map.entry("EQIX", REAL_ESTATE), Map.entry("EQR", REAL_ESTATE), Map.entry("ERIE", FINANCIALS), Map.entry("ESS", REAL_ESTATE), Map.entry("EL", CONSUMER_STAPLES), Map.entry("EG", FINANCIALS), Map.entry("EVRG", UTILITIES), Map.entry("ES", UTILITIES), Map.entry("EXC", UTILITIES), Map.entry("EXE", ENERGY), Map.entry("EXPE", CONSUMER_DISCRETIONARY), Map.entry("EXPD", INDUSTRIALS), Map.entry("EXR", REAL_ESTATE), Map.entry("XOM", ENERGY), Map.entry("FFIV", INFORMATION_TECHNOLOGY), Map.entry("FDS", FINANCIALS), Map.entry("FICO", INFORMATION_TECHNOLOGY), Map.entry("FAST", INDUSTRIALS), Map.entry("FRT", REAL_ESTATE), Map.entry("FDX", INDUSTRIALS), Map.entry("FIS", FINANCIALS), Map.entry("FITB", FINANCIALS), Map.entry("FSLR", INFORMATION_TECHNOLOGY), Map.entry("FE", UTILITIES), Map.entry("FISV", FINANCIALS), Map.entry("F", CONSUMER_DISCRETIONARY), Map.entry("FTNT", INFORMATION_TECHNOLOGY), Map.entry("FTV", INDUSTRIALS), Map.entry("FOXA", COMMUNICATION_SERVICES), Map.entry("FOX", COMMUNICATION_SERVICES), Map.entry("BEN", FINANCIALS), Map.entry("FCX", MATERIALS), Map.entry("GRMN", CONSUMER_DISCRETIONARY), Map.entry("IT", INFORMATION_TECHNOLOGY), Map.entry("GE", INDUSTRIALS), Map.entry("GEHC", HEALTH_CARE), Map.entry("GEV", INDUSTRIALS), Map.entry("GEN", INFORMATION_TECHNOLOGY), Map.entry("GNRC", INDUSTRIALS), Map.entry("GD", INDUSTRIALS), Map.entry("GIS", CONSUMER_STAPLES), Map.entry("GM", CONSUMER_DISCRETIONARY), Map.entry("GPC", CONSUMER_DISCRETIONARY), Map.entry("GILD", HEALTH_CARE), Map.entry("GPN", FINANCIALS), Map.entry("GL", FINANCIALS), Map.entry("GDDY", INFORMATION_TECHNOLOGY), Map.entry("GS", FINANCIALS), Map.entry("HAL", ENERGY), Map.entry("HIG", FINANCIALS), Map.entry("HAS", CONSUMER_DISCRETIONARY), Map.entry("HCA", HEALTH_CARE), Map.entry("DOC", REAL_ESTATE), Map.entry("HSIC", HEALTH_CARE), Map.entry("HSY", CONSUMER_STAPLES), Map.entry("HPE", INFORMATION_TECHNOLOGY), Map.entry("HLT", CONSUMER_DISCRETIONARY), Map.entry("HOLX", HEALTH_CARE), Map.entry("HD", CONSUMER_DISCRETIONARY), Map.entry("HON", INDUSTRIALS), Map.entry("HRL", CONSUMER_STAPLES), Map.entry("HST", REAL_ESTATE), Map.entry("HWM", INDUSTRIALS), Map.entry("HPQ", INFORMATION_TECHNOLOGY), Map.entry("HUBB", INDUSTRIALS), Map.entry("HUM", HEALTH_CARE), Map.entry("HBAN", FINANCIALS), Map.entry("HII", INDUSTRIALS), Map.entry("IBM", INFORMATION_TECHNOLOGY), Map.entry("IEX", INDUSTRIALS), Map.entry("IDXX", HEALTH_CARE), Map.entry("ITW", INDUSTRIALS), Map.entry("INCY", HEALTH_CARE), Map.entry("IR", INDUSTRIALS), Map.entry("PODD", HEALTH_CARE), Map.entry("INTC", INFORMATION_TECHNOLOGY), Map.entry("IBKR", FINANCIALS), Map.entry("ICE", FINANCIALS), Map.entry("IFF", MATERIALS), Map.entry("IP", MATERIALS), Map.entry("INTU", INFORMATION_TECHNOLOGY), Map.entry("ISRG", HEALTH_CARE), Map.entry("IVZ", FINANCIALS), Map.entry("INVH", REAL_ESTATE), Map.entry("IQV", HEALTH_CARE), Map.entry("IRM", REAL_ESTATE), Map.entry("JBHT", INDUSTRIALS), Map.entry("JBL", INFORMATION_TECHNOLOGY), Map.entry("JKHY", FINANCIALS), Map.entry("J", INDUSTRIALS), Map.entry("JNJ", HEALTH_CARE), Map.entry("JCI", INDUSTRIALS), Map.entry("JPM", FINANCIALS), Map.entry("KVUE", CONSUMER_STAPLES), Map.entry("KDP", CONSUMER_STAPLES), Map.entry("KEY", FINANCIALS), Map.entry("KEYS", INFORMATION_TECHNOLOGY), Map.entry("KMB", CONSUMER_STAPLES), Map.entry("KIM", REAL_ESTATE), Map.entry("KMI", ENERGY), Map.entry("KKR", FINANCIALS), Map.entry("KLAC", INFORMATION_TECHNOLOGY), Map.entry("KHC", CONSUMER_STAPLES), Map.entry("KR", CONSUMER_STAPLES), Map.entry("LHX", INDUSTRIALS), Map.entry("LH", HEALTH_CARE), Map.entry("LRCX", INFORMATION_TECHNOLOGY), Map.entry("LW", CONSUMER_STAPLES), Map.entry("LVS", CONSUMER_DISCRETIONARY), Map.entry("LDOS", INDUSTRIALS), Map.entry("LEN", CONSUMER_DISCRETIONARY), Map.entry("LII", INDUSTRIALS), Map.entry("LLY", HEALTH_CARE), Map.entry("LIN", MATERIALS), Map.entry("LYV", COMMUNICATION_SERVICES), Map.entry("LMT", INDUSTRIALS), Map.entry("L", FINANCIALS), Map.entry("LOW", CONSUMER_DISCRETIONARY), Map.entry("LULU", CONSUMER_DISCRETIONARY), Map.entry("LYB", MATERIALS), Map.entry("MTB", FINANCIALS), Map.entry("MPC", ENERGY), Map.entry("MAR", CONSUMER_DISCRETIONARY), Map.entry("MMC", FINANCIALS), Map.entry("MLM", MATERIALS), Map.entry("MAS", INDUSTRIALS), Map.entry("MA", FINANCIALS), Map.entry("MTCH", COMMUNICATION_SERVICES), Map.entry("MKC", CONSUMER_STAPLES), Map.entry("MCD", CONSUMER_DISCRETIONARY), Map.entry("MCK", HEALTH_CARE), Map.entry("MDT", HEALTH_CARE), Map.entry("MRK", HEALTH_CARE), Map.entry("META", COMMUNICATION_SERVICES), Map.entry("MET", FINANCIALS), Map.entry("MTD", HEALTH_CARE), Map.entry("MGM", CONSUMER_DISCRETIONARY), Map.entry("MCHP", INFORMATION_TECHNOLOGY), Map.entry("MU", INFORMATION_TECHNOLOGY), Map.entry("MSFT", INFORMATION_TECHNOLOGY), Map.entry("MAA", REAL_ESTATE), Map.entry("MRNA", HEALTH_CARE), Map.entry("MOH", HEALTH_CARE), Map.entry("TAP", CONSUMER_STAPLES), Map.entry("MDLZ", CONSUMER_STAPLES), Map.entry("MPWR", INFORMATION_TECHNOLOGY), Map.entry("MNST", CONSUMER_STAPLES), Map.entry("MCO", FINANCIALS), Map.entry("MS", FINANCIALS), Map.entry("MOS", MATERIALS), Map.entry("MSI", INFORMATION_TECHNOLOGY), Map.entry("MSCI", FINANCIALS), Map.entry("NDAQ", FINANCIALS), Map.entry("NTAP", INFORMATION_TECHNOLOGY), Map.entry("NFLX", COMMUNICATION_SERVICES), Map.entry("NEM", MATERIALS), Map.entry("NWSA", COMMUNICATION_SERVICES), Map.entry("NWS", COMMUNICATION_SERVICES), Map.entry("NEE", UTILITIES), Map.entry("NKE", CONSUMER_DISCRETIONARY), Map.entry("NI", UTILITIES), Map.entry("NDSN", INDUSTRIALS), Map.entry("NSC", INDUSTRIALS), Map.entry("NTRS", FINANCIALS), Map.entry("NOC", INDUSTRIALS), Map.entry("NCLH", CONSUMER_DISCRETIONARY), Map.entry("NRG", UTILITIES), Map.entry("NUE", MATERIALS), Map.entry("NVDA", INFORMATION_TECHNOLOGY), Map.entry("NVR", CONSUMER_DISCRETIONARY), Map.entry("NXPI", INFORMATION_TECHNOLOGY), Map.entry("ORLY", CONSUMER_DISCRETIONARY), Map.entry("OXY", ENERGY), Map.entry("ODFL", INDUSTRIALS), Map.entry("OMC", COMMUNICATION_SERVICES), Map.entry("ON", INFORMATION_TECHNOLOGY), Map.entry("OKE", ENERGY), Map.entry("ORCL", INFORMATION_TECHNOLOGY), Map.entry("OTIS", INDUSTRIALS), Map.entry("PCAR", INDUSTRIALS), Map.entry("PKG", MATERIALS), Map.entry("PLTR", INFORMATION_TECHNOLOGY), Map.entry("PANW", INFORMATION_TECHNOLOGY), Map.entry("PSKY", COMMUNICATION_SERVICES), Map.entry("PH", INDUSTRIALS), Map.entry("PAYX", INDUSTRIALS), Map.entry("PAYC", INDUSTRIALS), Map.entry("PYPL", FINANCIALS), Map.entry("PNR", INDUSTRIALS), Map.entry("PEP", CONSUMER_STAPLES), Map.entry("PFE", HEALTH_CARE), Map.entry("PCG", UTILITIES), Map.entry("PM", CONSUMER_STAPLES), Map.entry("PSX", ENERGY), Map.entry("PNW", UTILITIES), Map.entry("PNC", FINANCIALS), Map.entry("POOL", CONSUMER_DISCRETIONARY), Map.entry("PPG", MATERIALS), Map.entry("PPL", UTILITIES), Map.entry("PFG", FINANCIALS), Map.entry("PG", CONSUMER_STAPLES), Map.entry("PGR", FINANCIALS), Map.entry("PLD", REAL_ESTATE), Map.entry("PRU", FINANCIALS), Map.entry("PEG", UTILITIES), Map.entry("PTC", INFORMATION_TECHNOLOGY), Map.entry("PSA", REAL_ESTATE), Map.entry("PHM", CONSUMER_DISCRETIONARY), Map.entry("PWR", INDUSTRIALS), Map.entry("QCOM", INFORMATION_TECHNOLOGY), Map.entry("DGX", HEALTH_CARE), Map.entry("Q", INFORMATION_TECHNOLOGY), Map.entry("RL", CONSUMER_DISCRETIONARY), Map.entry("RJF", FINANCIALS), Map.entry("RTX", INDUSTRIALS), Map.entry("O", REAL_ESTATE), Map.entry("REG", REAL_ESTATE), Map.entry("REGN", HEALTH_CARE), Map.entry("RF", FINANCIALS), Map.entry("RSG", INDUSTRIALS), Map.entry("RMD", HEALTH_CARE), Map.entry("RVTY", HEALTH_CARE), Map.entry("HOOD", FINANCIALS), Map.entry("ROK", INDUSTRIALS), Map.entry("ROL", INDUSTRIALS), Map.entry("ROP", INFORMATION_TECHNOLOGY), Map.entry("ROST", CONSUMER_DISCRETIONARY), Map.entry("RCL", CONSUMER_DISCRETIONARY), Map.entry("SPGI", FINANCIALS), Map.entry("CRM", INFORMATION_TECHNOLOGY), Map.entry("SNDK", INFORMATION_TECHNOLOGY), Map.entry("SBAC", REAL_ESTATE), Map.entry("SLB", ENERGY), Map.entry("STX", INFORMATION_TECHNOLOGY), Map.entry("SRE", UTILITIES), Map.entry("NOW", INFORMATION_TECHNOLOGY), Map.entry("SHW", MATERIALS), Map.entry("SPG", REAL_ESTATE), Map.entry("SWKS", INFORMATION_TECHNOLOGY), Map.entry("SJM", CONSUMER_STAPLES), Map.entry("SW", MATERIALS), Map.entry("SNA", INDUSTRIALS), Map.entry("SOLV", HEALTH_CARE), Map.entry("SO", UTILITIES), Map.entry("LUV", INDUSTRIALS), Map.entry("SWK", INDUSTRIALS), Map.entry("SBUX", CONSUMER_DISCRETIONARY), Map.entry("STT", FINANCIALS), Map.entry("STLD", MATERIALS), Map.entry("STE", HEALTH_CARE), Map.entry("SYK", HEALTH_CARE), Map.entry("SMCI", INFORMATION_TECHNOLOGY), Map.entry("SYF", FINANCIALS), Map.entry("SNPS", INFORMATION_TECHNOLOGY), Map.entry("SYY", CONSUMER_STAPLES), Map.entry("TMUS", COMMUNICATION_SERVICES), Map.entry("TROW", FINANCIALS), Map.entry("TTWO", COMMUNICATION_SERVICES), Map.entry("TPR", CONSUMER_DISCRETIONARY), Map.entry("TRGP", ENERGY), Map.entry("TGT", CONSUMER_STAPLES), Map.entry("TEL", INFORMATION_TECHNOLOGY), Map.entry("TDY", INFORMATION_TECHNOLOGY), Map.entry("TER", INFORMATION_TECHNOLOGY), Map.entry("TSLA", CONSUMER_DISCRETIONARY), Map.entry("TXN", INFORMATION_TECHNOLOGY), Map.entry("TPL", ENERGY), Map.entry("TXT", INDUSTRIALS), Map.entry("TMO", HEALTH_CARE), Map.entry("TJX", CONSUMER_DISCRETIONARY), Map.entry("TKO", COMMUNICATION_SERVICES), Map.entry("TTD", COMMUNICATION_SERVICES), Map.entry("TSCO", CONSUMER_DISCRETIONARY), Map.entry("TT", INDUSTRIALS), Map.entry("TDG", INDUSTRIALS), Map.entry("TRV", FINANCIALS), Map.entry("TRMB", INFORMATION_TECHNOLOGY), Map.entry("TFC", FINANCIALS), Map.entry("TYL", INFORMATION_TECHNOLOGY), Map.entry("TSN", CONSUMER_STAPLES), Map.entry("USB", FINANCIALS), Map.entry("UBER", INDUSTRIALS), Map.entry("UDR", REAL_ESTATE), Map.entry("ULTA", CONSUMER_DISCRETIONARY), Map.entry("UNP", INDUSTRIALS), Map.entry("UAL", INDUSTRIALS), Map.entry("UPS", INDUSTRIALS), Map.entry("URI", INDUSTRIALS), Map.entry("UNH", HEALTH_CARE), Map.entry("UHS", HEALTH_CARE), Map.entry("VLO", ENERGY), Map.entry("VTR", REAL_ESTATE), Map.entry("VLTO", INDUSTRIALS), Map.entry("VRSN", INFORMATION_TECHNOLOGY), Map.entry("VRSK", INDUSTRIALS), Map.entry("VZ", COMMUNICATION_SERVICES), Map.entry("VRTX", HEALTH_CARE), Map.entry("VTRS", HEALTH_CARE), Map.entry("VICI", REAL_ESTATE), Map.entry("V", FINANCIALS), Map.entry("VST", UTILITIES), Map.entry("VMC", MATERIALS), Map.entry("WRB", FINANCIALS), Map.entry("GWW", INDUSTRIALS), Map.entry("WAB", INDUSTRIALS), Map.entry("WMT", CONSUMER_STAPLES), Map.entry("DIS", COMMUNICATION_SERVICES), Map.entry("WBD", COMMUNICATION_SERVICES), Map.entry("WM", INDUSTRIALS), Map.entry("WAT", HEALTH_CARE), Map.entry("WEC", UTILITIES), Map.entry("WFC", FINANCIALS), Map.entry("WELL", REAL_ESTATE), Map.entry("WST", HEALTH_CARE), Map.entry("WDC", INFORMATION_TECHNOLOGY), Map.entry("WY", REAL_ESTATE), Map.entry("WSM", CONSUMER_DISCRETIONARY), Map.entry("WMB", ENERGY), Map.entry("WTW", FINANCIALS), Map.entry("WDAY", INFORMATION_TECHNOLOGY), Map.entry("WYNN", CONSUMER_DISCRETIONARY), Map.entry("XEL", UTILITIES), Map.entry("XYL", INDUSTRIALS), Map.entry("YUM", CONSUMER_DISCRETIONARY), Map.entry("ZBRA", INFORMATION_TECHNOLOGY), Map.entry("ZBH", HEALTH_CARE), Map.entry("ZTS", HEALTH_CARE));
    public static final Set<String> SP500_SYMBOLS = Set.copyOf(SYMBOL_TO_SECTOR.keySet());
    public static final int SCHEDULED_RECOMMENDATIONS_COUNT = SP500_SYMBOLS.size();
    private static final String FINNHUB_RATE_LIMITER = "finnhubRateLimiter";
    private static final String FINNHUB_RATE_LIMITER_2 = "finnhubRateLimiter2";
    private static final String FINNHUB_RATE_LIMITER_3 = "finnhubRateLimiter3";
    private static final OrderedIndexedSet<String> FINNHUB_RATE_LIMITERS = OrderedIndexedSet.of(FINNHUB_RATE_LIMITER, FINNHUB_RATE_LIMITER_2, FINNHUB_RATE_LIMITER_3);
    private static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final Set<LocalDate> MARKET_HOLIDAYS_2025 = Set.of(
            LocalDate.of(2025, 1, 1), // New Year's Day
            LocalDate.of(2025, 1, 20), // Martin Luther King Jr. Day
            LocalDate.of(2025, 2, 17), // Presidents’ Day
            LocalDate.of(2025, 4, 18), // Good Friday
            LocalDate.of(2025, 5, 26), // Memorial Day
            LocalDate.of(2025, 6, 19), // Juneteenth Day
            LocalDate.of(2025, 7, 4), // Independence Day
            LocalDate.of(2025, 9, 1), // Labor Day
            LocalDate.of(2025, 11, 27), // Thanksgiving
            LocalDate.of(2025, 12, 25), // Christmas Day
            LocalDate.of(2026, 1, 1), // New Year's Day
            LocalDate.of(2026, 1, 19), // Martin Luther King Jr. Day
            LocalDate.of(2026, 2, 16), // Presidents' Day
            LocalDate.of(2026, 4, 3), // Good Friday
            LocalDate.of(2026, 5, 25), // Memorial Day
            LocalDate.of(2026, 6, 19), // Juneteenth Day
            LocalDate.of(2026, 7, 3), // Independence Day
            LocalDate.of(2026, 9, 7), // Labor Day
            LocalDate.of(2026, 11, 26), // Thanksgiving
            LocalDate.of(2026, 12, 25)  // Christmas Day
    );
    private static final Set<Clients> FINE_GRAIN_CLIENTS = Set.of(GPT_4_1_SPECIALIST, GPT_4_1_2_SPECIALIST, GPT_4_1_3_SPECIALIST,
            GPT_4_1_4_SPECIALIST, GPT_4_1_5_SPECIALIST, GPT_4_1_6_SPECIALIST);
    private static final Set<Clients> RECOMMENDATION_CLIENTS = Set.of(GPT_4_1, GPT_4_1_2, GPT_4_1_3, GPT_4_1_4, GPT_4_1_5, GPT_4_1_6);
    private static final Set<Clients> RANDOM_RECOMMENDATION_CLIENTS = Set.of(GPT_4_1, GPT_4_1_2, GPT_4_1_3, GPT_4_1_4, GPT_4_1_5, GPT_4_1_6);
    private static final Map<RecommendationMode, Set<String>> modeToClientNames = new EnumMap<>(Map.of(
            RecommendationMode.FINE_GRAIN, FINE_GRAIN_CLIENTS.stream().map(Clients::toString).collect(Collectors.toSet()),
            RecommendationMode.RANDOM, RANDOM_RECOMMENDATION_CLIENTS.stream().map(Clients::toString).collect(Collectors.toSet()),
            RecommendationMode.NOT_RANDOM, RECOMMENDATION_CLIENTS.stream().map(Clients::toString).collect(Collectors.toSet())
    ));

    private static final Map<String, String> ENTITY_MAP = Map.ofEntries(
            Map.entry("&quot;", "\""),
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
            Map.entry("&rdquo;", "”")
    );

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

    public static Set<AIClient> filterClients(Map<String, AIClient> allClients, RecommendationMode recommendationMode) {
        return allClients.entrySet().stream()
                .filter(clientNameToClient -> modeToClientNames.get(recommendationMode).contains(clientNameToClient.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    public static OrderedIndexedSet<String> getFinnhubRateLimiterNames() {
        return FINNHUB_RATE_LIMITERS;
    }

    public static int[] getDigits() {
        return DIGITS;
    }

    public static boolean isTradingDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return DayOfWeek.SATURDAY != dayOfWeek
                && DayOfWeek.SUNDAY != dayOfWeek
                && !MARKET_HOLIDAYS_2025.contains(date);
    }

    public enum MarketDataType {
        LAST, HISTORIC, REAL_TIME
    }

    public enum PortfolioType {
        REAL, MOCK
    }

    public enum RecommendationMode {
        FINE_GRAIN, RANDOM, NOT_RANDOM
    }

    protected enum Clients {
        GPT_4_1("gpt-4.1"),
        GPT_4_1_2("gpt-4.1-2"),
        GPT_4_1_3("gpt-4.1-3"),
        GPT_4_1_4("gpt-4.1-4"),
        GPT_4_1_5("gpt-4.1-5"),
        GPT_4_1_6("gpt-4.1-6"),
        GPT_4_1_SPECIALIST("gpt-4.1-specialist"),
        GPT_4_1_2_SPECIALIST("gpt-4.1-2-specialist"),
        GPT_4_1_3_SPECIALIST("gpt-4.1-3-specialist"),
        GPT_4_1_4_SPECIALIST("gpt-4.1-4-specialist"),
        GPT_4_1_5_SPECIALIST("gpt-4.1-5-specialist"),
        GPT_4_1_6_SPECIALIST("gpt-4.1-6-specialist"),
        GPT_4_1_MINI("gpt-4.1-mini"),
        GPT_4_1_NANO("gpt-4.1-nano"),
        GPT_4O("gpt-4o"),
        GPT_4O_MINI("gpt-4o-mini"),
        DEEPSEEK_V3("deepseek-v3"),
        GROK_3("grok-3"),
        GROK_3_2("grok-3-2"),
        GROK_3_3("grok-3-3"),
        GROK_3_4("grok-3-4"),
        GROK_3_5("grok-3-5"),
        GROK_3_6("grok-3-6"),
        LLAMA_3_3_70B("llama-3.3-70b"),
        LLAMA_3_1_405B("llama-3.1-405b"),
        LLAMA_3_1_70B("llama-3.1-70b"),
        LLAMA_3_1_8B("llama-3.1-8b"),
        MISTRAL_LARGE("mistral-large"),
        MISTRAL_NEMO("mistral-nemo"),
        MINISTRAL_3B("ministral-3b"),
        COMMAND_R_PLUS_2024("command-r-plus-2024"),
        COMMAND_A("command-a"),
        A21_JAMBA("a21-jamba"),
        PHI_4("phi-4"),
        DEEPSEEK_R1_0528("deepseek-r1-0528"), // not working
        DEEPSEEK_R1("deepseek-r1"), // not working
        COMMAND_R_PLUS("command-r-plus"), // not working
        O4_MINI("o4-mini"), // not working
        O3("o3"), // not working
        O3_MINI("o3-mini"), // not working
        O1("o1"), // not working
        O1_PREVIEW("o1-preview"), // not working
        O1_MINI("o1-mini"); // not working

        private final String label;

        Clients(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Sector {
        COMMUNICATION_SERVICES("Communication Services"),
        UTILITIES("Utilities"),
        HEALTH_CARE("Health Care"),
        CONSUMER_STAPLES("Consumer Staples"),
        ENERGY("Energy"),
        REAL_ESTATE("Real Estate"),
        FINANCIALS("Financials"),
        MATERIALS("Materials"),
        INDUSTRIALS("Industrials"),
        CONSUMER_DISCRETIONARY("Consumer Discretionary"),
        INFORMATION_TECHNOLOGY("Information Technology");

        private final String label;

        Sector(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
