package com.lucas.server.components.tradingbot.marketdata.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucas.server.TestConfiguration;
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
import java.util.Set;

import static com.lucas.server.common.Constants.MAPPING_ERROR;
import static com.lucas.server.common.Constants.PREMARKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestConfiguration.class)
class YahooFinanceMarketResponseMapperTest {

    @Autowired
    private SymbolJpaService symbolService;

    @Autowired
    private YahooFinanceMarketResponseMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void whenMapValidJson_thenReturnMarketData() throws JsonProcessingException, com.fasterxml.jackson.core.JsonProcessingException {
        // given
        String json = """
                {
                  "chart": {
                    "result": [
                      {
                        "meta": {
                          "currency": "USD",
                          "symbol": "AAPL",
                          "exchangeName": "NMS",
                          "fullExchangeName": "NasdaqGS",
                          "instrumentType": "EQUITY",
                          "firstTradeDate": 345479400,
                          "regularMarketTime": 1753819201,
                          "hasPrePostMarketData": true,
                          "gmtoffset": -14400,
                          "timezone": "EDT",
                          "exchangeTimezoneName": "America/New_York",
                          "regularMarketPrice": 211.27,
                          "fiftyTwoWeekHigh": 260.1,
                          "fiftyTwoWeekLow": 169.21,
                          "regularMarketDayHigh": 214.81,
                          "regularMarketDayLow": 210.825,
                          "regularMarketVolume": 48540908,
                          "longName": "Apple Inc.",
                          "shortName": "Apple Inc.",
                          "chartPreviousClose": 214.05,
                          "previousClose": 214.05,
                          "scale": 3,
                          "priceHint": 2,
                          "currentTradingPeriod": {
                            "pre": {
                              "timezone": "EDT",
                              "end": 1753882200,
                              "start": 1753862400,
                              "gmtoffset": -14400
                            },
                            "regular": {
                              "timezone": "EDT",
                              "end": 1753905600,
                              "start": 1753882200,
                              "gmtoffset": -14400
                            },
                            "post": {
                              "timezone": "EDT",
                              "end": 1753920000,
                              "start": 1753905600,
                              "gmtoffset": -14400
                            }
                          },
                          "tradingPeriods": {
                            "pre": [
                              [
                                {
                                  "timezone": "EDT",
                                  "end": 1753882200,
                                  "start": 1753862400,
                                  "gmtoffset": -14400
                                }
                              ]
                            ],
                            "post": [
                              [
                                {
                                  "timezone": "EDT",
                                  "end": 1753920000,
                                  "start": 1753905600,
                                  "gmtoffset": -14400
                                }
                              ]
                            ],
                            "regular": [
                              [
                                {
                                  "timezone": "EDT",
                                  "end": 1753905600,
                                  "start": 1753882200,
                                  "gmtoffset": -14400
                                }
                              ]
                            ]
                          },
                          "dataGranularity": "1h",
                          "range": "1d",
                          "validRanges": [
                            "1d",
                            "5d",
                            "1mo",
                            "3mo",
                            "6mo",
                            "1y",
                            "2y",
                            "5y",
                            "10y",
                            "ytd",
                            "max"
                          ]
                        },
                        "indicators": {
                          "quote": [
                            {
                              "close": [
                                211.4,
                                211.44
                              ],
                              "low": [
                                211.27,
                                211.44
                              ],
                              "open": [
                                211.38,
                                211.44
                              ],
                              "high": [
                                211.63,
                                211.44
                              ],
                              "volume": [
                                0,
                                0
                              ]
                            }
                          ]
                        },
                        "timestamp": [
                          1753862400,
                          1753864520
                        ]
                      }
                    ],
                    "error": null
                  }
                }
                """;
        Symbol symbol = symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow();

        // when
        MarketData result = mapper.map(objectMapper.readTree(json), symbol);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo(symbol);
        assertThat(result.getOpen()).isEqualByComparingTo(BigDecimal.valueOf(211.38));
        assertThat(result.getHigh()).isEqualByComparingTo(BigDecimal.valueOf(211.63));
        assertThat(result.getLow()).isEqualByComparingTo(BigDecimal.valueOf(211.27));
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(211.44));
        assertThat(result.getVolume()).isNull();
        assertThat(result.getDate()).isNull();
        assertThat(result.getPreviousClose()).isNull();
        assertThat(result.getChange()).isNull();
        assertThat(result.getChangePercent()).isNull();
    }

    @Test
    @Transactional
    void whenMapInvalidJson_thenThrowException() {
        // given
        String invalidJson = "{}";

        // when & then
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(invalidJson), symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow())).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @Transactional
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
        assertThatThrownBy(() -> mapper.map(objectMapper.readTree(json), symbolService.getOrCreateByName(Set.of("AAPL")).stream().findFirst().orElseThrow()))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining(MessageFormat.format(MAPPING_ERROR, PREMARKET));
    }
}
