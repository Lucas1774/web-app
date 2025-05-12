package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AlphavantageMarketResponseMapperTest {

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    AlphavantageMarketResponseMapper mapper;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        symbolService.deleteAll();
    }

    @Test
    void whenMapValidJson_thenReturnMarketData() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                    "Global Quote": {
                        "01. symbol": "IBM",
                        "02. open": "140.5000",
                        "03. high": "142.0000",
                        "04. low": "139.5000",
                        "05. price": "141.2500",
                        "06. volume": "4832356",
                        "07. latest trading day": "2023-12-15",
                        "08. previous close": "140.0000",
                        "09. change": "1.2500",
                        "10. change percent": "0.8928%"
                    }
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("IBM");

        // when
        MarketData result = mapper.map(objectMapper.readTree(json)).setSymbol(symbol);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol().getName()).isEqualTo(symbol.getName());
        assertThat(result.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(140.5000));
        assertThat(result.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(142.0000));
        assertThat(result.getLow()).isEqualByComparingTo(BigDecimal.valueOf(139.5000));
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(141.2500));
        assertThat(result.getVolume()).isEqualByComparingTo(4832356L);
        assertThat(result.getDate()).isEqualTo(LocalDate.parse("2023-12-15"));
        assertThat(result.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(140.0000));
        assertThat(result.getChange()).isEqualByComparingTo(BigDecimal.valueOf(1.2500));
        assertThat(result.getChangePercent()).isEqualByComparingTo(BigDecimal.valueOf(0.8928));
    }

    @Test
    void whenMapInvalidJson_thenThrowException() {
        // given
        String invalidJson = "{}";

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(invalidJson))).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void whenMapAllValidJson_thenReturnMarketDataList() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                    "Weekly Time Series": {
                        "2023-12-15": {
                            "1. open": "140.5000",
                            "2. high": "142.0000",
                            "3. low": "139.5000",
                            "4. close": "141.2500",
                            "5. volume": "4832356"
                        },
                        "2023-12-08": {
                            "1. open": "139.0000",
                            "2. high": "141.0000",
                            "3. low": "138.5000",
                            "4. close": "140.0000",
                            "5. volume": "4532356"
                        }
                    }
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("IBM");

        // when
        List<MarketData> result = mapper.mapAll(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull().hasSize(2);

        MarketData firstEntry = result.getFirst();
        assertThat(firstEntry.getSymbol().getName()).isEqualTo(symbol.getName());
        assertThat(firstEntry.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(140.5000));
        assertThat(firstEntry.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(142.0000));
        assertThat(firstEntry.getLow()).isEqualByComparingTo(BigDecimal.valueOf(139.5000));
        assertThat(firstEntry.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(141.2500));
        assertThat(firstEntry.getVolume()).isEqualByComparingTo(4832356L);
        assertThat(firstEntry.getDate()).isEqualTo(LocalDate.parse("2023-12-15"));

        MarketData secondEntry = result.get(1);
        assertThat(firstEntry.getSymbol().getName()).isEqualTo(symbol.getName());
        assertThat(secondEntry.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(139.0000));
        assertThat(secondEntry.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(141.0000));
        assertThat(secondEntry.getLow()).isEqualByComparingTo(BigDecimal.valueOf(138.5000));
        assertThat(secondEntry.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(140.0000));
        assertThat(secondEntry.getVolume()).isEqualByComparingTo(4532356L);
        assertThat(secondEntry.getDate()).isEqualTo(LocalDate.parse("2023-12-08"));
    }

    @Test
    void whenMapAllEmptyTimeSeries_thenReturnEmptyList() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                    "Weekly Time Series": {
                    }
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName("IBM");

        // when
        List<MarketData> result = mapper.mapAll(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void whenMapAllMissingFields_thenThrowsException() {
        // given
        String json = """
                {
                    "Global Quote": {
                        "01. symbol": "IBM",
                        "02. open": "140.5000",
                        "03. high": "142.0000",
                        "05. price": "141.2500",
                        "06. volume": "4832356",
                        "07. latest trading day": "2023-12-15",
                        "08. previous close": "140.0000",
                        "09. change": "1.2500",
                        "10. change percent": "0.8928%"
                    }
                }
                """;

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(json)))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"));
    }
}
