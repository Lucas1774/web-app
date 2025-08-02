package com.lucas.server.components.tradingbot.marketdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.config.SlidingWindowRateLimiter;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.TwelveDataMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.function.UnaryOperator;

import static com.lucas.server.common.Constants.*;

@Component
public class TwelveDataMarketDataClient {

    private static final Logger logger = LoggerFactory.getLogger(TwelveDataMarketDataClient.class);
    private static final EnumMap<MarketDataType, String> typeToEndpoint = new EnumMap<>(Map.of(
            MarketDataType.LAST, QUOTE,
            MarketDataType.HISTORIC, TIME_SERIES
    ));
    private static final Map<MarketDataType, UnaryOperator<UriComponentsBuilder>> typeToBuilderCustomizer = new EnumMap<>(Map.of(
            MarketDataType.LAST, builder -> builder,
            MarketDataType.HISTORIC, builder -> builder.queryParam("interval", "1day")
    ));
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;
    private final String apiKey;
    private final Map<MarketDataType, TwelveDataMarketDataClient.JsonToMarketDataFunction> typeToMapper;

    public TwelveDataMarketDataClient(HttpRequestClient httpRequestClient, Map<String, SlidingWindowRateLimiter> rateLimiters,
                                      TwelveDataMarketResponseMapper mapper,
                                      @Value("${twelve-data.endpoint}") String endpoint,
                                      @Value("${twelve-data.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.rateLimiter = rateLimiters.get(TWELVEDATA_RATE_LIMITER);
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        typeToMapper = new EnumMap<>(Map.of(
                MarketDataType.LAST, (s, j) -> List.of(mapper.map(j, s)),
                MarketDataType.HISTORIC, (s, j) -> mapper.mapAll(j, s).stream()
                        .sorted(Comparator.comparing(MarketData::getDate))
                        .toList()
        ));
    }

    private List<MarketData> retrieveMarketData(Symbol symbol, MarketDataType type) throws ClientException, JsonProcessingException {
        rateLimiter.acquirePermission();
        logger.info(RETRIEVING_DATA_INFO, MARKET_DATA, symbol);
        String url = typeToBuilderCustomizer.get(type).apply(UriComponentsBuilder.fromUriString(endpoint + typeToEndpoint.get(type)))
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        return typeToMapper.get(type).apply(symbol, httpRequestClient.fetch(url, false));
    }

    public List<MarketData> retrieveMarketData(List<Symbol> symbols, MarketDataType type) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            res.addAll(retrieveMarketData(symbol, type));
        }
        return res;
    }

    @FunctionalInterface
    private interface JsonToMarketDataFunction {
        List<MarketData> apply(Symbol symbol, JsonNode jsonNode) throws JsonProcessingException;
    }
}
