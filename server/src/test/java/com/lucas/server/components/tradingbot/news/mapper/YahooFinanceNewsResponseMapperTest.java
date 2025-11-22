package com.lucas.server.components.tradingbot.news.mapper;

import com.lucas.server.ConfiguredTest;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.news.jpa.News;
import com.lucas.utils.exception.MappingException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Set;

import static com.lucas.server.common.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YahooFinanceNewsResponseMapperTest extends ConfiguredTest {

    @Autowired
    private SymbolJpaService symbolService;

    @Autowired
    private YahooFinanceNewsResponseMapper mapper;

    @Autowired
    private DocumentBuilderFactory factory;

    @Test
    @Transactional
    void whenMapValidXml_thenReturnNewsEntity() throws Exception {
        // given
        String itemXml = """
                <item>
                    <description>The Dow Jones index rose after surprise economic data. Tesla stock rallied on new plans to launch its robotaxi service.</description>
                    <guid isPermaLink="false">d7c02a66-936a-32e1-b631-65fbc838c25d</guid>
                    <link>https://finance.yahoo.com/m/d7c02a66-936a-32e1-b631-65fbc838c25d/stock-market-today%3A-dow%2C-s%26p.html?.tsrc=rss</link>
                    <pubDate>Fri, 25 Jul 2025 20:36:04 +0000</pubDate>
                    <title>Stock Market Today: Dow, S&amp;P Climb On Trump-China Deal Hopes; Cathie Wood Loads Up On Tesla Stock (Live Coverage)</title>
                </item>
                """;

        Element item = (Element) parseXml("<?xml version=\"1.0\"?><rss><channel>" + itemXml + "</channel></rss>")
                .getElementsByTagName("item").item(0);

        // when
        News news = mapper.map(item);

        // then
        assertThat(news)
                .isNotNull()
                .satisfies(n -> {
                    assertThat(news.getExternalId()).isEqualTo("d7c02a66-936a-32e1-b631-65fbc838c25d".hashCode());
                    assertThat(news.getDate()).isEqualTo(LocalDateTime.of(2025, 7, 25, 20, 36, 4));
                    assertThat(news.getHeadline()).isEqualTo("Stock Market Today: Dow, S&P Climb On Trump-China Deal Hopes; Cathie Wood Loads Up On Tesla Stock (Live Coverage)");
                    assertThat(news.getSummary()).isEqualTo("The Dow Jones index rose after surprise economic data. Tesla stock rallied on new plans to launch its robotaxi service.");
                    assertThat(news.getUrl()).isEqualTo("https://finance.yahoo.com/m/d7c02a66-936a-32e1-b631-65fbc838c25d/stock-market-today%3A-dow%2C-s%26p.html?.tsrc=rss");
                    assertThat(news.getSource()).isEqualTo("Yahoo Finance RSS");
                    assertThat(news.getCategory()).isNull();
                    assertThat(news.getImage()).isNull();
                    assertThat(news.getSymbols()).isEmpty();
                });
    }

    @Test
    @Transactional
    void whenMapAllValidArray_thenReturnNewsList() throws Exception {
        // given
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <rss version="2.0">
                    <channel>
                        <copyright>Copyright (c) 2025 Yahoo Inc. All rights reserved.</copyright>
                        <description>Latest Financial News for TSLA</description>
                        <image>
                            <height>45</height>
                            <link>http://finance.yahoo.com/q/h?s=TSLA</link>
                            <title>Yahoo! Finance: TSLA News</title>
                            <url>https://s.yimg.com/rz/stage/p/yahoo_finance_en-US_h_p_finance_2.png</url>
                            <width>144</width>
                        </image>
                        <item>
                            <description>Archer Aviation might have a brighter future than the struggling luxury EV maker.</description>
                            <guid isPermaLink="false">4b2588dd-dc6f-3d71-84e0-de1ef9da2e48</guid>
                            <link>https://www.fool.com/investing/2025/07/26/ev-stock-that-will-be-worth-more-than-lucid/?.tsrc=rss</link>
                            <pubDate>Sat, 26 Jul 2025 11:30:00 +0000</pubDate>
                            <title>Prediction: 1 EV Stock That Will Be Worth More Than Lucid 1 Year From Now</title>
                        </item>
                        <item>
                            <description>Tesla is facing another lawsuit regarding its self-driving claims.  The California DMV is trying to suspend Tesla's dealer license for 30 days.  The EV maker has also launched a new round of buyer incentives.</description>
                            <guid isPermaLink="false">493b7cb4-2573-3103-92c3-c2cd35c4a7d8</guid>
                            <link>https://www.fool.com/investing/2025/07/26/why-tesla-deliveries-could-hit-yet-another-speed/?.tsrc=rss</link>
                            <pubDate>Sat, 26 Jul 2025 11:15:00 +0000</pubDate>
                            <title>Why Tesla Deliveries Could Hit Yet Another Speed Bump</title>
                        </item>
                        <item>
                            <description>Electric vehicles promise lower running costs, but how much cheaper are they really? Here’s how charging a Tesla stacks up against fueling a BMW 3 Series.</description>
                            <guid isPermaLink="false">7a1eca4e-e2cc-3b36-b796-2227608665fd</guid>
                            <link>https://finance.yahoo.com/news/does-cost-charge-tesla-monthly-091023135.html?.tsrc=rss</link>
                            <pubDate>Sat, 26 Jul 2025 09:10:23 +0000</pubDate>
                            <title>What Does It Cost To Charge a Tesla Monthly Compared To Gas for a BMW 3 Series?</title>
                        </item>
                        <item>
                            <description>Autonomous vehicles are set to transform the ride-hailing industry.</description>
                            <guid isPermaLink="false">07aac843-f9c1-3e67-b060-5afc5d11da7a</guid>
                            <link>https://www.fool.com/investing/2025/07/26/should-buy-autonomous-driving-stock-before-aug-6/?.tsrc=rss</link>
                            <pubDate>Sat, 26 Jul 2025 08:29:00 +0000</pubDate>
                            <title>Should You Buy This Magnificent Autonomous Driving Stock Before Aug. 6?</title>
                        </item>
                        <item>
                            <description>Elon Musk's Tesla told the California Public Utilities Commission it plans a car service around San Francisco similar to robotaxis but with human drivers.</description>
                            <guid isPermaLink="false">7ed9a7b7-0070-3af8-8112-7f75f56a15ca</guid>
                            <link>https://www.nbcnews.com/tech/elon-musk/tesla-plans-friends-family-car-service-california-regulator-says-rcna221209?.tsrc=rss</link>
                            <pubDate>Sat, 26 Jul 2025 01:44:24 +0000</pubDate>
                            <title>Tesla plans 'friends and family' car service in California, regulator says</title>
                        </item>
                        <item>
                            <description>An earnings miss and Elon Musk warning send the shares of the electric-car maker down. But the company still has all of the components of success in artificial intelligence.</description>
                            <guid isPermaLink="false">7034ecb5-0554-3919-953d-a8010b82cff0</guid>
                            <link>https://finance.yahoo.com/m/7034ecb5-0554-3919-953d-a8010b82cff0/tesla-clings-to-the.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 23:42:00 +0000</pubDate>
                            <title>Tesla Clings to the Trillion-Dollar Club. How to Stay In? AI.</title>
                        </item>
                        <item>
                            <description>There will be an employee in the driver's seat -- a big change from how things have been working in Austin.</description>
                            <guid isPermaLink="false">58813c8e-33ea-3262-837c-25c1a8d5d41b</guid>
                            <link>https://finance.yahoo.com/news/tesla-reportedly-bringing-limited-version-145505078.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 23:41:43 +0000</pubDate>
                            <title>Tesla wants to bring robotaxis to San Francisco. Here’s what’s standing in the way.</title>
                        </item>
                        <item>
                            <description>The company's announcement of a new car model drove investors into the stock.</description>
                            <guid isPermaLink="false">a22e3eca-acc8-335a-ad57-2fac3e0e7a91</guid>
                            <link>https://www.fool.com/investing/2025/07/25/why-nio-stock-accelerated-12-higher-this-week/?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 22:51:37 +0000</pubDate>
                            <title>Why Nio Stock Accelerated 12% Higher This Week</title>
                        </item>
                        <item>
                            <description>The week in stocks: Tesla hit the skids, what's driving meme stocks and analysts rethink targets on major Canadian firms</description>
                            <guid isPermaLink="false">0a989911-6630-31ee-bfee-ce6adb2eded5</guid>
                            <link>https://ca.finance.yahoo.com/news/hurricane-season-heating-canadian-oil-220236709.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 22:02:36 +0000</pubDate>
                            <title>Hurricane season is heating up and these Canadian oil stocks are poised to benefit</title>
                        </item>
                        <item>
                            <description>Meta, Amazon and Microsoft lead an earnings wave, along with a Fed meeting and Trump tariff deadline. Tesla robotaxi may expand to San Francisco, with a caveat.</description>
                            <guid isPermaLink="false">9a954028-d676-3679-8768-2eedc7f9763d</guid>
                            <link>https://finance.yahoo.com/m/9a954028-d676-3679-8768-2eedc7f9763d/dow-jones-futures%3A-meta%2C.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 21:40:03 +0000</pubDate>
                            <title>Dow Jones Futures: Meta, Amazon, Fed, Trump Tariffs Ahead; Tesla Robotaxi Or Just Taxi?</title>
                        </item>
                        <item>
                            <description>Tesla's latest earnings report is playing a role in Solana's sell-off today.</description>
                            <guid isPermaLink="false">bf488631-4c68-305b-9629-fc4f6e64d8e5</guid>
                            <link>https://www.fool.com/investing/2025/07/25/why-solana-is-sinking-today/?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 21:36:00 +0000</pubDate>
                            <title>Why Solana Is Sinking Today</title>
                        </item>
                        <item>
                            <description>Musk ruled out a merger between Tesla and xAI earlier in July, but said he planned to hold a shareholder vote on investment in the startup by the automaker.  The startup completed a $5 billion debt raise alongside a separate $5 billion strategic equity investment, Morgan Stanley said last month.</description>
                            <guid isPermaLink="false">1a0ccf72-39d2-30ab-aa8d-60d07570b17c</guid>
                            <link>https://finance.yahoo.com/news/tesla-gets-multiple-shareholder-proposals-212333957.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 21:23:33 +0000</pubDate>
                            <title>Tesla gets multiple shareholder proposals related to investment in xAI</title>
                        </item>
                        <item>
                            <description>A record-breaking deficit, fading revenues, and looming US trade talks could reshape global market momentum.</description>
                            <guid isPermaLink="false">d432aa92-7168-3878-b623-49048f741f8e</guid>
                            <link>https://finance.yahoo.com/news/chinas-733-billion-warning-why-211856511.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 21:18:56 +0000</pubDate>
                            <title>China's $733 Billion Warning: Why Investors Can't Ignore This Red Flag</title>
                        </item>
                        <item>
                            <description>US equity indexes rose this week, with the S&amp;P 500 and the Nasdaq Composite claiming new records aft</description>
                            <guid isPermaLink="false">91351577-00b6-39fe-a4bb-b6bcc944ec7f</guid>
                            <link>https://finance.yahoo.com/news/us-equity-indexes-rise-week-205736276.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:57:36 +0000</pubDate>
                            <title>US Equity Indexes Rise This Week as Japan Trade Deal, Corporate Earnings Boost Investors' Sentiment</title>
                        </item>
                        <item>
                            <description>A new, lower-priced Tesla EV is slated to hit roads in the fourth quarter. It might not be what investors expect.</description>
                            <guid isPermaLink="false">84571d3c-1e03-3df5-b65b-59d05258b6e7</guid>
                            <link>https://finance.yahoo.com/m/84571d3c-1e03-3df5-b65b-59d05258b6e7/tesla-stock-is-rising..html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:40:00 +0000</pubDate>
                            <title>Tesla Stock Is Rising. Robo-taxi Trumps a Lack of New Models.</title>
                        </item>
                        <item>
                            <description>Friday, Booz Allen reported fiscal first-quarter earnings per share of $1.48 from sales of $2.9 billion.</description>
                            <guid isPermaLink="false">e8e4c11b-b79c-33da-9bc2-7ea11128823d</guid>
                            <link>https://finance.yahoo.com/m/e8e4c11b-b79c-33da-9bc2-7ea11128823d/booz-allen-earnings%2C-outlook.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:38:00 +0000</pubDate>
                            <title>Booz Allen Earnings, Outlook a Relief in Post-DOGE World. The Stock Is Dropping.</title>
                        </item>
                        <item>
                            <description>The Dow Jones index rose after surprise economic data. Tesla stock rallied on new plans to launch its robotaxi service.</description>
                            <guid isPermaLink="false">d7c02a66-936a-32e1-b631-65fbc838c25d</guid>
                            <link>https://finance.yahoo.com/m/d7c02a66-936a-32e1-b631-65fbc838c25d/stock-market-today%3A-dow%2C-s%26p.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:36:04 +0000</pubDate>
                            <title>Stock Market Today: Dow, S&amp;P Climb On Trump-China Deal Hopes; Cathie Wood Loads Up On Tesla Stock (Live Coverage)</title>
                        </item>
                        <item>
                            <description>SAN FRANCISCO (Reuters) -Tesla has told California it would expand operations of a chartered transportation service in the Bay Area, a state regulator said on Friday.  The permit does not allow the company to run vehicles autonomously, the California Public Utilities Commission said.  The update follows a report that Tesla was preparing to roll out robotaxis in the Bay Area with a safety driver as soon as this weekend.</description>
                            <guid isPermaLink="false">77c597f1-6d1c-3525-9e64-dbe11ab38154</guid>
                            <link>https://ca.finance.yahoo.com/news/tesla-plans-expand-chartered-transport-202419246.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:24:19 +0000</pubDate>
                            <title>Tesla plans to expand chartered transport service, California regulator says</title>
                        </item>
                        <item>
                            <description>Intel stock falls after the chip maker reports a wider loss in the second quarter, while Centene rises even as it posts a surprise quarterly loss.</description>
                            <guid isPermaLink="false">0d725364-875d-34d3-857c-d193237f2d2a</guid>
                            <link>https://finance.yahoo.com/m/0d725364-875d-34d3-857c-d193237f2d2a/these-stocks-moved-the-most.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:10:00 +0000</pubDate>
                            <title>These Stocks Moved the Most Today: Intel, Centene, Charter, Newmont, Deckers, Boston Beer, Tesla, and More</title>
                        </item>
                        <item>
                            <description>Tesla fell more than 8% Thursday as Elon Musk warned of "rough" quarters. But Wood's ARK loaded up.</description>
                            <guid isPermaLink="false">734f6db2-9f02-3278-8a5a-dec6c708db1d</guid>
                            <link>https://finance.yahoo.com/m/734f6db2-9f02-3278-8a5a-dec6c708db1d/as-tesla-stock-sank-on.html?.tsrc=rss</link>
                            <pubDate>Fri, 25 Jul 2025 20:08:36 +0000</pubDate>
                            <title>As Tesla Stock Sank On Earnings, Cathie Wood Loaded Up</title>
                        </item>
                        <language>en-US</language>
                        <lastBuildDate>Sat, 26 Jul 2025 11:56:18 +0000</lastBuildDate>
                        <link>http://finance.yahoo.com/q/h?s=TSLA</link>
                        <title>Yahoo! Finance: TSLA News</title>
                    </channel>
                </rss>
                """;

        // when
        Set<News> list = mapper.mapAll(parseXml(xml), symbol);

        // then
        assertThat(list)
                .isNotNull()
                .hasSize(20)
                .allSatisfy(n -> assertThat(n.getSymbols().stream().map(Symbol::getName))
                        .hasSize(1)
                        .containsExactly(symbol.getName())
                )
                .extracting(News::getHeadline)
                .containsExactlyInAnyOrder(
                        "Prediction: 1 EV Stock That Will Be Worth More Than Lucid 1 Year From Now",
                        "Why Tesla Deliveries Could Hit Yet Another Speed Bump",
                        "What Does It Cost To Charge a Tesla Monthly Compared To Gas for a BMW 3 Series?",
                        "Should You Buy This Magnificent Autonomous Driving Stock Before Aug. 6?",
                        "Tesla plans 'friends and family' car service in California, regulator says",
                        "Tesla Clings to the Trillion-Dollar Club. How to Stay In? AI.",
                        "Tesla wants to bring robotaxis to San Francisco. Here’s what’s standing in the way.",
                        "Why Nio Stock Accelerated 12% Higher This Week",
                        "Hurricane season is heating up and these Canadian oil stocks are poised to benefit",
                        "Dow Jones Futures: Meta, Amazon, Fed, Trump Tariffs Ahead; Tesla Robotaxi Or Just Taxi?",
                        "Why Solana Is Sinking Today",
                        "Tesla gets multiple shareholder proposals related to investment in xAI",
                        "China's $733 Billion Warning: Why Investors Can't Ignore This Red Flag",
                        "US Equity Indexes Rise This Week as Japan Trade Deal, Corporate Earnings Boost Investors' Sentiment",
                        "Tesla Stock Is Rising. Robo-taxi Trumps a Lack of New Models.",
                        "Booz Allen Earnings, Outlook a Relief in Post-DOGE World. The Stock Is Dropping.",
                        "Stock Market Today: Dow, S&P Climb On Trump-China Deal Hopes; Cathie Wood Loads Up On Tesla Stock (Live Coverage)",
                        "Tesla plans to expand chartered transport service, California regulator says",
                        "These Stocks Moved the Most Today: Intel, Centene, Charter, Newmont, Deckers, Boston Beer, Tesla, and More",
                        "As Tesla Stock Sank On Earnings, Cathie Wood Loaded Up"
                );
    }

    @Test
    @Transactional
    void whenMapAllEmptyOrNonArray_thenThrowsException() throws Exception {
        // given: valid XML structure but no <item> elements
        String xmlWithoutItems = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>Yahoo! Finance: TSLA News</title>
                        <link>http://finance.yahoo.com/q/h?s=TSLA</link>
                        <description>Latest Financial News for TSLA</description>
                        <!-- no <item> entries here -->
                    </channel>
                </rss>
                """;
        Document doc = parseXml(xmlWithoutItems);

        // when & then
        assertThatThrownBy(() -> mapper.mapAll(doc, symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow()))
                .isInstanceOf(MappingException.class)
                .cause()
                .hasMessageContaining(MessageFormat.format(NO_YAHOO_NEWS_ERROR, doc));
    }

    @Test
    @Transactional
    void whenMapAllMissingFields_thenThrowsException() throws Exception {
        // given
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <item>
                            <title>News with missing fields</title>
                            <description>Some description</description>
                            <!-- Missing guid and pubDate -->
                        </item>
                    </channel>
                </rss>
                """;
        Document doc = parseXml(invalidXml);

        // when & then
        assertThatThrownBy(() -> mapper.mapAll(doc, symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow()))
                .isInstanceOf(MappingException.class)
                .hasMessageContaining(MessageFormat.format(MAPPING_ERROR, NEWS));
    }

    private Document parseXml(String xml) throws Exception {
        return factory
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
