package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestcontainersConfiguration;
import com.lucas.server.common.Constants;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.common.jpa.SymbolJpaService;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import org.junit.jupiter.api.AfterEach;
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
class FinnhubMarketResponseMapperTest {

    @Autowired
    SymbolJpaService symbolService;

    @Autowired
    FinnhubMarketResponseMapper mapper;

    @Autowired
    ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        symbolService.deleteAll();
    }

    @Test
    void whenMapValidJson_thenReturnMarketData() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "c" : 198.53,
                  "d" : 1.04,
                  "dp" : 0.5266,
                  "h" : 200.5399,
                  "l" : 197.535,
                  "o" : 199,
                  "pc" : 197.49,
                  "t" : 1746820800
                }
                """;
        Symbol symbol = new Symbol().setName("IBM");
        symbolService.save(symbol);

        // when
        MarketData result = mapper.map(objectMapper.readTree(json), symbol.getName());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo(symbol);
        assertThat(result.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(result.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(200.5399));
        assertThat(result.getLow()).isEqualByComparingTo(BigDecimal.valueOf(197.535));
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(198.53));
        assertThat(result.getVolume()).isNull();
        assertThat(result.getDate()).isEqualTo(LocalDate.parse("2025-05-09"));
        assertThat(result.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(197.49));
        assertThat(result.getChange()).isEqualByComparingTo(BigDecimal.valueOf(1.04));
        assertThat(result.getChangePercent()).isEqualTo("0.5266%");
    }

    @Test
    void whenMapValidJsonNonExistingSymbol_thenReturnMarketData() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "c" : 198.53,
                  "d" : 1.04,
                  "dp" : 0.5266,
                  "h" : 200.5399,
                  "l" : 197.535,
                  "o" : 199,
                  "pc" : 197.49,
                  "t" : 1746820800
                }
                """;
        Symbol symbol = new Symbol().setName("IBM"); // To be created by the mapper

        // when
        MarketData result = mapper.map(objectMapper.readTree(json), symbol.getName());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol().getId()).isNotNull();
        assertThat(result.getSymbol().getName()).isEqualTo(symbol.getName());
        assertThat(result.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(result.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(200.5399));
        assertThat(result.getLow()).isEqualByComparingTo(BigDecimal.valueOf(197.535));
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(198.53));
        assertThat(result.getVolume()).isNull();
        assertThat(result.getDate()).isEqualTo(LocalDate.parse("2025-05-09"));
        assertThat(result.getPreviousClose()).isEqualByComparingTo(BigDecimal.valueOf(197.49));
        assertThat(result.getChange()).isEqualByComparingTo(BigDecimal.valueOf(1.04));
        assertThat(result.getChangePercent()).isEqualTo("0.5266%");
    }

    @Test
    void whenMapInvalidJson_thenThrowException() {
        // given
        String invalidJson = "{}";

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(invalidJson), "AAPL")).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void whenMapMissingFields_thenThrowsException() {
        // given
        String json = """
                {
                  "c" : 198.53,
                  "d" : 1.04,
                  "dp" : 0.5266,
                  "h" : 200.5399,
                  "o" : 199,
                  "pc" : 197.49,
                  "t" : 1746820800
                }
                """;

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(json), "AAPL"))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(Constants.JSON_MAPPING_ERROR, "market"));
    }
}
