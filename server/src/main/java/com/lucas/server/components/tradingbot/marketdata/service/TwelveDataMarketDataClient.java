package com.lucas.server.components.tradingbot.marketdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lucas.server.common.Constants;
import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.TwelveDataMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.function.UnaryOperator;

@Component
public class TwelveDataMarketDataClient {

    private final HttpRequestClient httpRequestClient;
    private final String endpoint;
    private final String apiKey;
    private static final Logger logger = LoggerFactory.getLogger(TwelveDataMarketDataClient.class);
    private final Map<Constants.TwelveDataType, TwelveDataMarketDataClient.JsonToMarketDataFunction> typeToMapper;
    private static final EnumMap<Constants.TwelveDataType, String> typeToEndpoint = new EnumMap<>(Map.of(
            Constants.TwelveDataType.LAST, Constants.QUOTE,
            Constants.TwelveDataType.HISTORIC, Constants.TIME_SERIES
    ));
    private static final Map<Constants.TwelveDataType, UnaryOperator<UriComponentsBuilder>> typeToBuilderCustomizer = new EnumMap<>(Map.of(
            Constants.TwelveDataType.LAST, builder -> builder,
            Constants.TwelveDataType.HISTORIC, builder -> builder.queryParam("interval", "1day")
    ));

    @FunctionalInterface
    private interface JsonToMarketDataFunction {
        List<MarketData> apply(Symbol symbol, JsonNode jsonNode) throws JsonProcessingException;
    }

    public TwelveDataMarketDataClient(HttpRequestClient httpRequestClient, TwelveDataMarketResponseMapper mapper,
                                      @Value("${twelve-data.endpoint}") String endpoint,
                                      @Value("${twelve-data.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        typeToMapper = new EnumMap<>(Map.of(
                Constants.TwelveDataType.LAST, (s, j) -> List.of(mapper.map(j, s)),
                Constants.TwelveDataType.HISTORIC, (s, j) -> mapper.mapAll(j, s).stream()
                        .sorted(Comparator.comparing(MarketData::getDate))
                        .toList()
        ));
    }

    private List<MarketData> retrieveMarketData(Symbol symbol, Constants.TwelveDataType type) throws ClientException, JsonProcessingException {
        logger.info(Constants.RETRIEVING_MARKET_DATA_INFO, symbol);
        String url = typeToBuilderCustomizer.get(type).apply(UriComponentsBuilder.fromUriString(endpoint + typeToEndpoint.get(type)))
                .queryParam("symbol", symbol.getName())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        return typeToMapper.get(type).apply(symbol, httpRequestClient.fetch(url));
    }

    public List<MarketData> retrieveMarketData(List<Symbol> symbols, Constants.TwelveDataType type) throws ClientException, JsonProcessingException {
        List<MarketData> res = new ArrayList<>();
        for (Symbol symbol : symbols) {
            res.addAll(retrieveMarketData(symbol, type));
            Constants.backOff(7500);
        }
        return res;
    }
}
