package com.lucas.server.components.tradingbot.recommendation.service;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.tradingbot.common.AiClient;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.common.jpa.DataManager.SymbolPayload;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.config.AiProperties;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketSnapshotDomain;
import com.lucas.server.components.tradingbot.news.dto.NewsDomain;
import com.lucas.server.components.tradingbot.news.jpa.NewsPersistenceOrchestrator;
import com.lucas.server.components.tradingbot.portfolio.dto.PortfolioDomain;
import com.lucas.server.components.tradingbot.recommendation.dto.RecommendationDomain;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.AssetReportRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.NewsItemRaw;
import com.lucas.server.components.tradingbot.recommendation.mapper.AssetReportToMustacheMapper.PricePointRaw;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.orderedindexedset.OrderedIndexedSetImpl;
import com.lucas.utils.ratelimiter.DefaultSlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.lucas.server.common.Constants.FIXED_DATE;
import static com.lucas.server.common.Constants.MARKET_DATA_RELEVANT_DAYS_COUNT;
import static com.lucas.server.common.Constants.NY_ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SpringBootTest
class RecommendationChatCompletionClientTest extends ConfiguredTest {

    private static final String FAKE_COMPLETION =
            "[{\"symbol\":\"AAPL\",\"action\":\"HOLD\",\"confidence\":\"0.5\",\"rationale\":\"test\"}]";
    private static final Pattern CONTEXT_DATE_PATTERN = Pattern.compile("• Current date: (.+) \\(market opens soon\\)");
    private static final DateTimeFormatter CONTEXT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

    @Autowired
    private RecommendationChatCompletionClient client;

    @Autowired
    private NewsPersistenceOrchestrator newsPersistenceOrchestrator;

    @Autowired
    private SymbolJpaService symbolService;

    @Autowired
    private Map<String, AiClient> clients;

    @MockitoBean
    private AssetReportDataProvider assetReportDataProvider;

    private SymbolDomain symbol;

    static Stream<Arguments> promptCombinations() {
        return Stream.of(Arguments.of(false, false, false),
                Arguments.of(false, true, false),
                Arguments.of(true, false, false),
                Arguments.of(true, true, false),
                Arguments.of(false, false, true),
                Arguments.of(false, true, true),
                Arguments.of(true, false, true),
                Arguments.of(true, true, true));
    }

    @BeforeEach
    void setup() {
        symbol = seedMarketData();
    }

    @ParameterizedTest(name = "useOldNews={0}, hasPremarket={1}, fixMe={2}")
    @MethodSource("promptCombinations")
    void fullPrompt_isBuiltCorrectly(boolean useOldNews, boolean hasPremarket, boolean fixMe) throws Exception {
        when(assetReportDataProvider.provide(any())).thenReturn(assetReport(hasPremarket));

        String[] messages = getRecommendation(useOldNews, fixMe, hasPremarket).getInput().split("\n\n\n");

        assertThat(messages).hasSize(4);
        assertThat(messages[0]).isEqualTo(expectedSystem(useOldNews, hasPremarket));
        // Context message is time-dependent.
        ZonedDateTime now = ZonedDateTime.now(NY_ZONE);
        assertContextMessage(messages[1], now.minusSeconds(2), now.plusSeconds(2));
        assertThat(messages[2]).isEqualTo(expectedFewShot());
        assertThat(messages[3]).isEqualTo(fixMe ? expectedFixMeReport(hasPremarket) : expectedReport(hasPremarket));
    }

    @Test
    void fullPrompt_rendersNullsAsNot() throws Exception {
        String expected = """
                          --- Market Data & Features ---
                          [ASSET: FOO]
                          • Price History (last 1 days):
                            • 2025-05-01: O100 H110 L90 C105 VN/A
                          • Technical Indicators at last close:
                            • 20-day EMA: N/A
                            • MACD(12,26,9): line=N/A,signal=N/A,hist=N/A
                            • 14-day RSI: N/A
                            • 14-day ATR%: N/A
                            • 20-day OBV: N/A
                          • News Summaries (last 2):
                            • 2025-04-30 20:00:00 EST: Headline One: First summary
                            • 2025-05-01 20:00:00 EST: Headline Two: Second summary
                          
                          """;
        when(assetReportDataProvider.provide(any())).thenReturn(assetReportWithNulls());

        String[] messages = getRecommendation(false, false, false).getInput().split("\n\n\n");

        assertThat(messages).hasSize(4);
        assertThat(messages[3]).isEqualTo(expected);
    }

    private static String expectedSystem(boolean useOldNews, boolean hasPremarket) {
        String priceInstruction = hasPremarket
                ? "assess whether the sentiment is already priced in and adjust the entry accordingly."
                : "derive a fair entry from the provided data.";

        String newsLine = useOldNews ? "\n" : """
                                              
                                              - Prioritize last-few-hours news about unexpected events or analyst calls over the specified company or its sector; treat older or generic news as confirmation alongside technical indicators.
                                              """;

        return """
               You are TradeGPT, a trading oracle whose every recommendation directly impacts people's capital and survival.
               Your task: analyze provided market data, technical indicators, and news summaries with precision and discipline. For each asset, output a clear recommendation (BUY, SELL or HOLD), a confidence score between 0 and 1, and a concise but complete rationale, including for BUY or SELL actions a suggested entry price: %s State the basis used.
               %s- Do not hallucinate.
               - Output in raw JSON format with fields: symbol, action, confidence, rationale.\
               """.formatted(priceInstruction, newsLine);
    }

    private static String expectedFewShot() {
        return "[{\"symbol\":\"MSFT\",\"action\":\"BUY\",\"confidence\":0.xx,\"rationale\":\"...\"},"
               + "{\"symbol\":\"AAPL\",\"action\":\"SELL\",\"confidence\":0.xx,\"rationale\":\"...\"},"
               + "{\"symbol\":\"GOOG\",\"action\":\"HOLD\",\"confidence\":0.xx,\"rationale\":\"..."
               + " are not good / bad enough to justify a buy / sell\"}]";
    }

    private static String expectedReport(boolean hasPremarket) {
        String premarket = hasPremarket ? "• This morning's pre-market: O100 H110 L90 Last price105 Gap: 7.5%\n" : "";

        return """
               --- Market Data & Features ---
               [ASSET: FOO]
               %s• Price History (last 1 days):
                 • 2025-05-01: O100 H110 L90 C105 V1234
               • Technical Indicators at last close:
                 • 20-day EMA: 105
                 • MACD(12,26,9): line=42.42,signal=1.23,hist=41.19
                 • 14-day RSI: 15.67
                 • 14-day ATR%%: 15.68%%
                 • 20-day OBV: 15.69
               • News Summaries (last 2):
                 • 2025-04-30 20:00:00 EST: Sentiment: positive. Confidence: 54.4412%%. Headline One: First summary
                 • 2025-05-01 20:00:00 EST: Sentiment: negative. Confidence: 54.4412%%. Headline Two: Second summary
               
               """.formatted(premarket);
    }

    private static String expectedFixMeReport(boolean hasPremarket) {
        return "Here is the asset report. Please:\n\n"
               + " 1. Correct any typos and adjust the sentiment annotations to match each specific asset. \n"
               + "- If a news for \"AMD\" is marked \"positive\" but the text clearly implies a negative impact for AMD"
               + ", and a positive impact for other company, switch it to \"negative\", and vice-versa.\n"
               + "- Do not remove any news items or switch any sentiment to \"neutral.\"\n"
               + "- Only adjust sentiment if the annotation clearly contradicts the text for the named asset."
               + " Do not reinterpret or reclassify otherwise.\n"
               + "- Fix any misencoded character sequences and ensure all text is valid UTF-8.\n\n"
               + " 2. After you’ve cleaned the input, use that cleaned version to run your financial analysis.\n\n"
               + expectedReport(hasPremarket);
    }

    private static void assertContextMessage(String contextMessage, ZonedDateTime before, ZonedDateTime after) {
        assertThat(contextMessage).contains("• Low-volume (~$800/trade). Liquidity is not an issue")
                .contains("• Strategy: catalyst-led trades with medium-term support")
                .contains("• Morning entries (long or short) based on news and technical signals")
                .contains("• HOLD means: do not open a new position nor close existing ones");
        Matcher matcher = CONTEXT_DATE_PATTERN.matcher(contextMessage);
        assertThat(matcher.find()).as("context message should contain a formatted current date").isTrue();
        ZonedDateTime parsed = ZonedDateTime.parse(matcher.group(1), CONTEXT_DATE_FORMAT.withZone(NY_ZONE));
        assertThat(parsed).isBetween(before.minusSeconds(2), after.plusSeconds(2));
    }

    private static AssetReportRaw assetReport(boolean hasPremarket) {
        PricePointRaw historicalPricePoint = new PricePointRaw(LocalDate.of(2025, Month.MAY, 1),
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                1234L,
                null);

        PricePointRaw premarket = hasPremarket ? new PricePointRaw(null,
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                null,
                new BigDecimal("7.5")) : null;

        OrderedIndexedSet<NewsItemRaw> news = fullNews();

        return new AssetReportRaw("FOO",
                new BigDecimal("10.2412"),
                new BigDecimal("101.4887"),
                new BigDecimal("11.5874"),
                new BigDecimal("50"),
                new BigDecimal("80.00"),
                1,
                premarket,
                OrderedIndexedSet.of(historicalPricePoint),
                new BigDecimal("105.00"),
                new BigDecimal("42.42"),
                new BigDecimal("1.23"),
                new BigDecimal("15.67"),
                new BigDecimal("15.68"),
                new BigDecimal("15.69"),
                news.size(),
                news);
    }

    private static AssetReportRaw assetReportWithNulls() {
        PricePointRaw historicalPricePoint = new PricePointRaw(LocalDate.of(2025, Month.MAY, 1),
                new BigDecimal("100"),
                new BigDecimal("110"),
                new BigDecimal("90"),
                new BigDecimal("105"),
                null,
                null);

        OrderedIndexedSet<NewsItemRaw> news = nullNews();

        return new AssetReportRaw("FOO",
                null,
                null,
                null,
                null,
                null,
                1,
                null,
                OrderedIndexedSet.of(historicalPricePoint),
                null,
                null,
                null,
                null,
                null,
                null,
                news.size(),
                news);
    }

    private static MarketSnapshotDomain premarket() {
        return new MarketSnapshotDomain().setOpen(new BigDecimal("100"))
                .setHigh(new BigDecimal("110"))
                .setLow(new BigDecimal("90"))
                .setPrice(new BigDecimal("105"));
    }

    private static OrderedIndexedSet<NewsItemRaw> fullNews() {
        return OrderedIndexedSet.of(new NewsItemRaw("Headline One",
                        "positive",
                        BigDecimal.valueOf(54.4412),
                        "First summary",
                        LocalDateTime.of(LocalDate.of(2025, Month.MAY, 1), LocalTime.MIDNIGHT)),

                new NewsItemRaw("Headline Two",
                        "negative",
                        BigDecimal.valueOf(54.4412),
                        "Second summary",
                        LocalDateTime.of(LocalDate.of(2025, Month.MAY, 2), LocalTime.MIDNIGHT)));
    }

    private static OrderedIndexedSet<NewsItemRaw> nullNews() {
        return OrderedIndexedSet.of(new NewsItemRaw("Headline One",
                        null,
                        null,
                        "First summary",
                        LocalDateTime.of(LocalDate.of(2025, Month.MAY, 1), LocalTime.MIDNIGHT)),

                new NewsItemRaw("Headline Two",
                        null,
                        null,
                        "Second summary",
                        LocalDateTime.of(LocalDate.of(2025, Month.MAY, 2), LocalTime.MIDNIGHT)));
    }

    private RecommendationDomain getRecommendation(boolean useOldNews, boolean fixMe, boolean hasPremarket)
            throws Exception {
        AiClient stubbed = stubbedClient(fixMe);

        SymbolPayload payload = new SymbolPayload(symbol,
                marketDataService.getTopForSymbolId(symbol.getId(), MARKET_DATA_RELEVANT_DAYS_COUNT),
                new PortfolioDomain().setSymbol(symbol)).setNews(OrderedIndexedSet.of())
                .setPremarket(hasPremarket ? premarket() : null);

        RecommendationDomain recommendation =
                client.getRecommendations(Set.of(payload), OrderedIndexedSet.of(stubbed), useOldNews)
                        .stream()
                        .findFirst()
                        .orElseThrow();

        assertThat(recommendation.getInput()).isNotBlank();

        return recommendation;
    }

    private AiClient stubbedClient(boolean fixMe) throws Exception {
        AiClient real = clients.values().iterator().next();
        AiProperties.DeploymentProperties spiedConfig = spy(real.getConfig());
        doReturn(fixMe).when(spiedConfig).fixMe();

        AiClient spied = spy(real);
        doReturn(FAKE_COMPLETION).when(spied).complete(any());
        DefaultSlidingWindowRateLimiter rateLimiter = new DefaultSlidingWindowRateLimiter(100, Duration.ofSeconds(1));
        doReturn(rateLimiter).when(spied).getApiKeyRateLimiter();
        doReturn(rateLimiter).when(spied).getMoreRestrictiveRateLimiter();
        doReturn(rateLimiter).when(spied).getLessRestrictiveRateLimiter();
        doReturn(spiedConfig).when(spied).getConfig();

        return spied;
    }

    private SymbolDomain seedMarketData() {
        SymbolDomain res = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();
        LocalDate today = FIXED_DATE.toLocalDate();
        OrderedIndexedSet<MarketDataDomain> mds = new OrderedIndexedSetImpl<>();
        for (int i = MARKET_DATA_RELEVANT_DAYS_COUNT; 0 <= i; i--) {
            mds.add(new MarketDataDomain().setSymbol(res)
                    .setDate(today.minusDays(i))
                    .setOpen(BigDecimal.valueOf(100 + i))
                    .setHigh(BigDecimal.valueOf(110 + i))
                    .setLow(BigDecimal.valueOf(90 + i))
                    .setPrice(BigDecimal.valueOf(105 + i))
                    .setVolume(1_000L));
        }
        marketDataService.createIgnoringDuplicates(OrderedIndexedSet.copyOf(mds));

        Set<NewsDomain> news = new HashSet<>();
        for (int i = 1; 3 >= i; i++) {
            news.add(new NewsDomain().addSymbol(res)
                    .setExternalId((long) i)
                    .setUrl("https://example.com/" + i)
                    .setHeadline("Headline " + i)
                    .setSummary("Summary " + i)
                    .setDate(FIXED_DATE.minusDays(i)));
        }
        newsPersistenceOrchestrator.persistNews(news);

        return res;
    }
}
