package com.lucas.server.common;

import com.lucas.server.components.tradingbot.common.AIClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lucas.server.common.Constants.Clients.*;

public class Constants {

    private Constants() {
    }

    public enum MarketDataType {
        LAST, HISTORIC, REAL_TIME
    }

    public enum PortfolioType {
        REAL, MOCK
    }

    public enum RandomMode {
        RANDOM, NOT_RANDOM
    }

    public static List<AIClient> filterClients(Map<String, AIClient> allClients, RandomMode randomMode) {
        return allClients.entrySet().stream()
                .filter(clientNameToClient -> modeToClientNames.get(randomMode).contains(clientNameToClient.getKey()))
                .map(Map.Entry::getValue).toList();
    }

    public static void backOff(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static final int[] DIGITS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static final int SUDOKU_SIZE = 9;
    public static final int SUDOKU_NUMBER_OF_CELLS = 81;
    public static final int MARKET_DATA_RELEVANT_DAYS_COUNT = 34;
    public static final int HISTORY_DAYS_COUNT = 20;
    public static final int NEWS_COUNT = 10;
    public static final int DATABASE_NEWS_PER_SYMBOL = 20;
    public static final int DATABASE_MARKET_DATA_PER_SYMBOL = 100;
    public static final int DATABASE_RECOMMENDATIONS_PER_SYMBOL = 5;
    public static final int SCHEDULED_RECOMMENDATIONS_COUNT = 503;
    public static final int REQUEST_MAX_ATTEMPTS = 5;

    public static final int RECOMMENDATION_W_BAILOUT_MAX_RETRIES = 6;
    public static final int FINNHUB_BACKOFF_MILLIS = 1000;
    public static final int TWELVEDATA_BACKOFF_MILLIS = 7500;

    public static final String DEFAULT_USERNAME = "default";
    public static final String NA = "N/A";
    public static final String INVALID_EXPRESSION = "Invalid expression";
    public static final String COMPANY_NEWS = "/company-news";
    public static final String QUOTE = "/quote";
    public static final String TIME_SERIES = "/time_series";
    public static final String ANALYZE = "/analyze";
    public static final String CONTENT = "content";
    public static final String ROLE = "role";
    public static final String MARKET_DATA = "market data";
    public static final String NEWS = "news";
    public static final String SENTIMENT = "sentiment";
    public static final String RECOMMENDATION = "recommendation";
    public static final String PROMPT = "prompt";
    public static final String VOLATILITY = "volatility";
    public static final String OBV = "OBV";
    public static final String PER_MINUTE_RATE_LIMITER = "perMinuteRateLimiter";
    public static final String PER_SECOND_RATE_LIMITER = "perSecondRateLimiter";

    public static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    public static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    public static final LocalTime EARLY_CLOSE = LocalTime.of(13, 0);

    public static final String SUDOKU_IGNORED_MALFORMED_JSON_WARN = "Couldn't deserialize sudoku from raw data {}";
    public static final String KPI_RETURNED_ZERO_WARN = "Value is zero for {}, {}";
    public static final String NON_COMPUTABLE_KPI_WARN = "Error attempting to compute {} for {}";
    public static final String CLIENT_FAILED_BACKUP_WARN = "{} failed when trying to process {}: {}";
    public static final String MARKET_STILL_OPEN_WARN = "Market is still open!";
    public static final String RETRIEVAL_FAILED_WARN = "Error generating {} {}";
    public static final String SCHEDULED_TASK_SUCCESS_INFO = "Successfully {}: {}";
    public static final String RETRIEVING_DATA_INFO = "Retrieving {} for {}";
    public static final String PROMPTING_MODEL_INFO = "Prompting model {}";
    public static final String GENERATION_SUCCESSFUL_INFO = "Successfully generated {}";
    public static final String JSON_MAPPING_ERROR = "Error mapping {0} JSON";
    public static final String INSUFFICIENT_STOCK_ERROR = "{0}: Nothing to sell";
    public static final String SYMBOL_NOT_FOUND_ERROR = "{0}: Unknown symbol";

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
            LocalDate.of(2025, 12, 25) // Christmas Day
    );

    public static boolean isHoliday(LocalDate date) {
        return MARKET_HOLIDAYS_2025.contains(date);
    }

    public static final Set<LocalDate> EARLY_CLOSE_DATES_2025 = Set.of(
            LocalDate.of(2025, 7, 3), // Pre‑Independence Day
            LocalDate.of(2025, 11, 28), // Day after Thanksgiving
            LocalDate.of(2025, 12, 24) // Christmas Eve
    );

    protected enum Clients {
        GPT_4_1("gpt-4.1"),
        GPT_4_1_2("gpt-4.1-2"),
        GPT_4_1_3("gpt-4.1-3"),
        GPT_4_1_4("gpt-4.1-4"),
        GPT_4_1_5("gpt-4.1-5"),
        GPT_4_1_6("gpt-4.1-6"),
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

    private static final List<Clients> RECOMMENDATION_CLIENTS = List.of(GPT_4_1, GPT_4_1_2, GPT_4_1_3, GPT_4_1_4, GPT_4_1_5, GPT_4_1_6);
    private static final List<Clients> RANDOM_RECOMMENDATION_CLIENTS = List.of(GPT_4_1, GPT_4_1_2, GPT_4_1_3, GPT_4_1_4, GPT_4_1_5, GPT_4_1_6);
    private static final Map<RandomMode, Set<String>> modeToClientNames = new EnumMap<>(Map.of(
            RandomMode.RANDOM, RANDOM_RECOMMENDATION_CLIENTS.stream().map(Clients::toString).collect(Collectors.toSet()),
            RandomMode.NOT_RANDOM, RECOMMENDATION_CLIENTS.stream().map(Clients::toString).collect(Collectors.toSet())
    ));

    public static final List<String> SP500_SYMBOLS = List.of("MMM", "AOS", "ABT", "ABBV", "ACN", "ADBE", "AMD", "AES", "AFL", "A", "APD", "ABNB", "AKAM", "ALB", "ARE", "ALGN", "ALLE", "LNT", "ALL", "GOOGL", "GOOG", "MO", "AMZN", "AMCR", "AEE", "AEP", "AXP", "AIG", "AMT", "AWK", "AMP", "AME", "AMGN", "APH", "ADI", "ANSS", "AON", "APA", "APO", "AAPL", "AMAT", "APTV", "ACGL", "ADM", "ANET", "AJG", "AIZ", "T", "ATO", "ADSK", "ADP", "AZO", "AVB", "AVY", "AXON", "BKR", "BALL", "BAC", "BAX", "BDX", "BRK.B", "BBY", "TECH", "BIIB", "BLK", "BX", "BK", "BA", "BKNG", "BSX", "BMY", "AVGO", "BR", "BRO", "BF.B", "BLDR", "BG", "BXP", "CHRW", "CDNS", "CZR", "CPT", "CPB", "COF", "CAH", "KMX", "CCL", "CARR", "CAT", "CBOE", "CBRE", "CDW", "COR", "CNC", "CNP", "CF", "CRL", "SCHW", "CHTR", "CVX", "CMG", "CB", "CHD", "CI", "CINF", "CTAS", "CSCO", "C", "CFG", "CLX", "CME", "CMS", "KO", "CTSH", "COIN", "CL", "CMCSA", "CAG", "COP", "ED", "STZ", "CEG", "COO", "CPRT", "GLW", "CPAY", "CTVA", "CSGP", "COST", "CTRA", "CRWD", "CCI", "CSX", "CMI", "CVS", "DHR", "DRI", "DVA", "DAY", "DECK", "DE", "DELL", "DAL", "DVN", "DXCM", "FANG", "DLR", "DG", "DLTR", "D", "DPZ", "DASH", "DOV", "DOW", "DHI", "DTE", "DUK", "DD", "EMN", "ETN", "EBAY", "ECL", "EIX", "EW", "EA", "ELV", "EMR", "ENPH", "ETR", "EOG", "EPAM", "EQT", "EFX", "EQIX", "EQR", "ERIE", "ESS", "EL", "EG", "EVRG", "ES", "EXC", "EXE", "EXPE", "EXPD", "EXR", "XOM", "FFIV", "FDS", "FICO", "FAST", "FRT", "FDX", "FIS", "FITB", "FSLR", "FE", "FI", "F", "FTNT", "FTV", "FOXA", "FOX", "BEN", "FCX", "GRMN", "IT", "GE", "GEHC", "GEV", "GEN", "GNRC", "GD", "GIS", "GM", "GPC", "GILD", "GPN", "GL", "GDDY", "GS", "HAL", "HIG", "HAS", "HCA", "DOC", "HSIC", "HSY", "HES", "HPE", "HLT", "HOLX", "HD", "HON", "HRL", "HST", "HWM", "HPQ", "HUBB", "HUM", "HBAN", "HII", "IBM", "IEX", "IDXX", "ITW", "INCY", "IR", "PODD", "INTC", "ICE", "IFF", "IP", "IPG", "INTU", "ISRG", "IVZ", "INVH", "IQV", "IRM", "JBHT", "JBL", "JKHY", "J", "JNJ", "JCI", "JPM", "JNPR", "K", "KVUE", "KDP", "KEY", "KEYS", "KMB", "KIM", "KMI", "KKR", "KLAC", "KHC", "KR", "LHX", "LH", "LRCX", "LW", "LVS", "LDOS", "LEN", "LII", "LLY", "LIN", "LYV", "LKQ", "LMT", "L", "LOW", "LULU", "LYB", "MTB", "MPC", "MKTX", "MAR", "MMC", "MLM", "MAS", "MA", "MTCH", "MKC", "MCD", "MCK", "MDT", "MRK", "META", "MET", "MTD", "MGM", "MCHP", "MU", "MSFT", "MAA", "MRNA", "MHK", "MOH", "TAP", "MDLZ", "MPWR", "MNST", "MCO", "MS", "MOS", "MSI", "MSCI", "NDAQ", "NTAP", "NFLX", "NEM", "NWSA", "NWS", "NEE", "NKE", "NI", "NDSN", "NSC", "NTRS", "NOC", "NCLH", "NRG", "NUE", "NVDA", "NVR", "NXPI", "ORLY", "OXY", "ODFL", "OMC", "ON", "OKE", "ORCL", "OTIS", "PCAR", "PKG", "PLTR", "PANW", "PARA", "PH", "PAYX", "PAYC", "PYPL", "PNR", "PEP", "PFE", "PCG", "PM", "PSX", "PNW", "PNC", "POOL", "PPG", "PPL", "PFG", "PG", "PGR", "PLD", "PRU", "PEG", "PTC", "PSA", "PHM", "PWR", "QCOM", "DGX", "RL", "RJF", "RTX", "O", "REG", "REGN", "RF", "RSG", "RMD", "RVTY", "ROK", "ROL", "ROP", "ROST", "RCL", "SPGI", "CRM", "SBAC", "SLB", "STX", "SRE", "NOW", "SHW", "SPG", "SWKS", "SJM", "SW", "SNA", "SOLV", "SO", "LUV", "SWK", "SBUX", "STT", "STLD", "STE", "SYK", "SMCI", "SYF", "SNPS", "SYY", "TMUS", "TROW", "TTWO", "TPR", "TRGP", "TGT", "TEL", "TDY", "TER", "TSLA", "TXN", "TPL", "TXT", "TMO", "TJX", "TKO", "TSCO", "TT", "TDG", "TRV", "TRMB", "TFC", "TYL", "TSN", "USB", "UBER", "UDR", "ULTA", "UNP", "UAL", "UPS", "URI", "UNH", "UHS", "VLO", "VTR", "VLTO", "VRSN", "VRSK", "VZ", "VRTX", "VTRS", "VICI", "V", "VST", "VMC", "WRB", "GWW", "WAB", "WBA", "WMT", "DIS", "WBD", "WM", "WAT", "WEC", "WFC", "WELL", "WST", "WDC", "WY", "WSM", "WMB", "WTW", "WDAY", "WYNN", "XEL", "XYL", "YUM", "ZBRA", "ZBH", "ZTS");
}
