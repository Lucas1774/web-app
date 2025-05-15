package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketDataJpaService;
import com.lucas.server.components.tradingbot.news.jpa.NewsJpaService;
import com.lucas.server.components.tradingbot.portfolio.jpa.PortfolioJpaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TwelveDataMarketResponseMapperTest {

    @Autowired
    MarketDataJpaService marketDataService;

    @Autowired
    NewsJpaService newsService;

    @Autowired
    PortfolioJpaService portfolioService;

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    TwelveDataMarketResponseMapper mapper;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        marketDataService.deleteAll();
        newsService.deleteAll();
        portfolioService.deleteAll();
        symbolService.deleteAll();
    }

    @Test
    void whenMapValidJson_thenReturnMarketData() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "symbol": "IBM",
                  "name": "Apple Inc",
                  "exchange": "NASDAQ",
                  "mic_code": "XNAS",
                  "currency": "USD",
                  "datetime": "2021-09-16",
                  "timestamp": 1631772000,
                  "open": "148.44000",
                  "high": "148.96840",
                  "low": "147.22099",
                  "close": "148.85001",
                  "volume": "67903927",
                  "previous_close": "149.09000",
                  "change": "-0.23999",
                  "percent_change": "-0.16097",
                  "average_volume": "83571571",
                  "rolling_1d_change": "123.123",
                  "rolling_7d_change": "123.123",
                  "rolling_period_change": "123.123",
                  "is_market_open": false,
                  "fifty_two_week": {
                    "low": "103.10000",
                    "high": "157.25999",
                    "low_change": "45.75001",
                    "high_change": "-8.40999",
                    "low_change_percent": "44.37440",
                    "high_change_percent": "-5.34782",
                    "range": "103.099998 - 157.259995"
                  },
                  "extended_change": "0.09",
                  "extended_percent_change": "0.05",
                  "extended_price": "125.22",
                  "extended_timestamp": 1649845281,
                  "last_quote_at": 1631772000
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("IBM");

        // when
        MarketData result = mapper.map(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo(symbol);
        assertThat(result.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(148.44));
        assertThat(result.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(148.9684));
        assertThat(result.getLow()).isEqualByComparingTo(BigDecimal.valueOf(147.22099)); // Postgres will later round these
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(148.85001));
        assertThat(result.getVolume()).isEqualByComparingTo(67903927L);
        assertThat(result.getDate()).isEqualTo(LocalDate.parse("2021-09-16"));
        assertThat(result.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(149.09000));
        assertThat(result.getChange()).isEqualByComparingTo(BigDecimal.valueOf(-0.23999));
        assertThat(result.getChangePercent()).isEqualByComparingTo(BigDecimal.valueOf(-0.16097));
    }

    @Test
    void whenMapInvalidJson_thenThrowException() {
        // given
        String invalidJson = "{}";

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(invalidJson), symbolService.getOrCreateByName("AAPL"))).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void whenMapMissingFields_thenThrowsException() {
        // given
        String json = """
                {
                  "symbol": "AAPL",
                  "name": "Apple Inc",
                  "exchange": "NASDAQ",
                  "mic_code": "XNAS",
                  "currency": "USD",
                  "datetime": "2021-09-16",
                  "timestamp": 1631772000,
                  "open": "148.44000",
                  "high": "148.96840",
                  "close": "148.85001",
                  "volume": "67903927",
                  "previous_close": "149.09000",
                  "change": "-0.23999",
                  "percent_change": "-0.16097",
                  "average_volume": "83571571",
                  "rolling_1d_change": "123.123",
                  "rolling_7d_change": "123.123",
                  "rolling_period_change": "123.123",
                  "is_market_open": false,
                  "fifty_two_week": {
                    "low": "103.10000",
                    "high": "157.25999",
                    "low_change": "45.75001",
                    "high_change": "-8.40999",
                    "low_change_percent": "44.37440",
                    "high_change_percent": "-5.34782",
                    "range": "103.099998 - 157.259995"
                  },
                  "extended_change": "0.09",
                  "extended_percent_change": "0.05",
                  "extended_price": "125.22",
                  "extended_timestamp": 1649845281,
                  "last_quote_at": 1631772000
                }
                """;

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(json), symbolService.getOrCreateByName("AAPL")))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"));
    }

    @Test
    void whenMapMismatchingSymbolField_thenThrowsException() {
        // given
        String json = """
                {
                  "symbol": "IBM",
                  "name": "Apple Inc",
                  "exchange": "NASDAQ",
                  "mic_code": "XNAS",
                  "currency": "USD",
                  "datetime": "2021-09-16",
                  "timestamp": 1631772000,
                  "open": "148.44000",
                  "high": "148.96840",
                  "low": "147.22099",
                  "close": "148.85001",
                  "volume": "67903927",
                  "previous_close": "149.09000",
                  "change": "-0.23999",
                  "percent_change": "-0.16097",
                  "average_volume": "83571571",
                  "rolling_1d_change": "123.123",
                  "rolling_7d_change": "123.123",
                  "rolling_period_change": "123.123",
                  "is_market_open": false,
                  "fifty_two_week": {
                    "low": "103.10000",
                    "high": "157.25999",
                    "low_change": "45.75001",
                    "high_change": "-8.40999",
                    "low_change_percent": "44.37440",
                    "high_change_percent": "-5.34782",
                    "range": "103.099998 - 157.259995"
                  },
                  "extended_change": "0.09",
                  "extended_percent_change": "0.05",
                  "extended_price": "125.22",
                  "extended_timestamp": 1649845281,
                  "last_quote_at": 1631772000
                }
                """;

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(json), symbolService.getOrCreateByName("AAPL")))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"));
    }
}
