package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import static com.lucas.server.common.Constants.JSON_MAPPING_ERROR;
import static com.lucas.server.common.Constants.MARKET_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TwelveDataMarketResponseMapperTest {

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    TwelveDataMarketResponseMapper mapper;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @Transactional
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
    @Transactional
    void whenMapInvalidJson_thenThrowException() {
        // given
        String invalidJson = "{}";

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(invalidJson), symbolService.getOrCreateByName("AAPL"))).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @Transactional
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
                .hasMessageContaining(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA));
    }

    @Test
    @Transactional
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
                .hasMessageContaining(MessageFormat.format(JSON_MAPPING_ERROR, MARKET_DATA));
    }

    @Test
    @Transactional
    void whenMapAllValidJson_thenReturnMarketDataList() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "meta": {
                    "symbol": "AAPL",
                    "interval": "1min",
                    "currency": "USD",
                    "exchange_timezone": "America/New_York",
                    "exchange": "NASDAQ",
                    "mic_code": "XNAS",
                    "type": "Common Stock"
                  },
                  "values": [
                    {
                      "datetime": "2021-09-16 15:59:00",
                      "open": "148.73500",
                      "high": "148.86000",
                      "low": "148.73000",
                      "close": "148.85001",
                      "volume": "624277"
                    },
                    {
                      "datetime": "2021-09-16 15:58:00",
                      "open": "148.72000",
                      "high": "148.78000",
                      "low": "148.70000",
                      "close": "148.74001",
                      "volume": "274622"
                    },
                    {
                      "datetime": "2021-09-16 15:57:00",
                      "open": "148.77499",
                      "high": "148.79500",
                      "low": "148.71001",
                      "close": "148.72501",
                      "volume": "254725"
                    },
                    {
                      "datetime": "2021-09-16 15:56:00",
                      "open": "148.76500",
                      "high": "148.78999",
                      "low": "148.72000",
                      "close": "148.78000",
                      "volume": "230758"
                    },
                    {
                      "datetime": "2021-09-16 15:55:00",
                      "open": "148.80000",
                      "high": "148.80000",
                      "low": "148.70000",
                      "close": "148.76230",
                      "volume": "348577"
                    }
                  ],
                  "status": "ok"
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("AAPL");

        // when
        List<MarketData> result = mapper.mapAll(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull().hasSize(5);

        MarketData firstEntry = result.getFirst();
        assertThat(firstEntry.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(148.735));
        assertThat(firstEntry.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(148.86));
        assertThat(firstEntry.getLow()).isEqualByComparingTo(BigDecimal.valueOf(148.73));
        assertThat(firstEntry.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(148.85001));
        assertThat(firstEntry.getVolume()).isEqualByComparingTo(624277L);
        assertThat(firstEntry.getDate()).isEqualTo(LocalDate.parse("2021-09-16"));

        MarketData secondEntry = result.get(1);
        assertThat(secondEntry.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(148.72));
        assertThat(secondEntry.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(148.78));
        assertThat(secondEntry.getLow()).isEqualByComparingTo(BigDecimal.valueOf(148.7));
        assertThat(secondEntry.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(148.74001));
        assertThat(secondEntry.getVolume()).isEqualByComparingTo(274622L);
        assertThat(secondEntry.getDate()).isEqualTo(LocalDate.parse("2021-09-16"));
    }

    @Test
    @Transactional
    void whenMapAllEmptyTimeSeries_thenReturnEmptyList() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "meta": {
                    "symbol": "AAPL",
                    "interval": "1min",
                    "currency": "USD",
                    "exchange_timezone": "America/New_York",
                    "exchange": "NASDAQ",
                    "mic_code": "XNAS",
                    "type": "Common Stock"
                  },
                  "values": [
                  ],
                  "status": "ok"
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("AAPL");

        // when
        List<MarketData> result = mapper.mapAll(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull().isEmpty();
    }
}
