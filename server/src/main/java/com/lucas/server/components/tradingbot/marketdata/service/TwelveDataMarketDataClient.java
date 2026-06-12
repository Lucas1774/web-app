package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.components.tradingbot.common.dto.SymbolDomain;
import com.lucas.server.components.tradingbot.marketdata.dto.MarketDataDomain;
import com.lucas.server.components.tradingbot.marketdata.mapper.TwelveDataMarketResponseMapper;
import com.lucas.utils.exception.MappingException;
import com.lucas.utils.orderedindexedset.OrderedIndexedSet;
import com.lucas.utils.ratelimiter.SlidingWindowRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.lucas.server.common.Constants.MARKET_DATA;
import static com.lucas.server.common.Constants.MarketDataType;
import static com.lucas.server.common.Constants.QUOTE;
import static com.lucas.server.common.Constants.REQUEST_MAX_ATTEMPTS;
import static com.lucas.server.common.Constants.RETRIEVING_DATA_INFO;
import static com.lucas.server.common.Constants.SYMBOL;
import static com.lucas.server.common.Constants.TIME_SERIES;
import static com.lucas.server.common.Constants.TWELVEDATA_RATE_LIMITER;

@Component
@Slf4j
public class TwelveDataMarketDataClient {

    private static final EnumMap<MarketDataType, String> typeToEndpoint =
            new EnumMap<>(Map.of(MarketDataType.LAST, QUOTE, MarketDataType.HISTORIC, TIME_SERIES));
    private static final Map<MarketDataType, UnaryOperator<UriComponentsBuilder>> typeToBuilderCustomizer =
            new EnumMap<>(Map.of(MarketDataType.LAST,
                    builder -> builder,
                    MarketDataType.HISTORIC,
                    builder -> builder.queryParam("interval", "1day")));
    private final HttpRequestClient httpRequestClient;
    private final SlidingWindowRateLimiter rateLimiter;
    private final String endpoint;
    private final String apiKey;
    private final Map<MarketDataType, TwelveDataMarketDataClient.JsonToMarketDataFunction> typeToMapper;

    public TwelveDataMarketDataClient(HttpRequestClient httpRequestClient,
                                      Map<String, SlidingWindowRateLimiter> rateLimiters,
                                      TwelveDataMarketResponseMapper mapper,
                                      @Value("${twelve-data.endpoint}") String endpoint,
                                      @Value("${twelve-data.api-key}") String apiKey) {
        this.httpRequestClient = httpRequestClient;
        rateLimiter = rateLimiters.get(TWELVEDATA_RATE_LIMITER);
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        typeToMapper = new EnumMap<>(Map.of(MarketDataType.LAST,
                (s, j) -> OrderedIndexedSet.of(mapper.map(j, s)),
                MarketDataType.HISTORIC,
                (s, j) -> mapper.mapAll(j, s)));
    }

    @Retryable(retryFor = {ClientException.class, MappingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public OrderedIndexedSet<MarketDataDomain> retrieveMarketData(SymbolDomain symbol, MarketDataType type)
            throws ClientException, MappingException {
        rateLimiter.acquirePermission();
        log.info(RETRIEVING_DATA_INFO, MARKET_DATA, symbol);
        String url = typeToBuilderCustomizer.get(type)
                .apply(UriComponentsBuilder.fromUriString(endpoint + typeToEndpoint.get(type)))
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("apikey", apiKey)
                .build()
                .toUriString();

        return typeToMapper.get(type).apply(symbol, httpRequestClient.fetch(url, false));
    }

    @FunctionalInterface
    private interface JsonToMarketDataFunction {
        OrderedIndexedSet<MarketDataDomain> apply(SymbolDomain symbol, JsonNode jsonNode) throws MappingException;
    }
}
