package com.lucas.server.components.tradingbot.marketdata.service;

import com.lucas.server.common.HttpRequestClient;
import com.lucas.server.common.exception.ClientException;
import com.lucas.server.common.exception.JsonProcessingException;
import com.lucas.server.components.tradingbot.common.FinnhubRateLimiter;
import com.lucas.server.components.tradingbot.common.jpa.Symbol;
import com.lucas.server.components.tradingbot.marketdata.jpa.MarketData;
import com.lucas.server.components.tradingbot.marketdata.mapper.FinnhubMarketResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import static com.lucas.server.common.Constants.*;

@Component
public class FinnhubMarketDataClient {

    private static final Logger logger = LoggerFactory.getLogger(FinnhubMarketDataClient.class);
    private final FinnhubMarketResponseMapper mapper;
    private final HttpRequestClient httpRequestClient;
    private final FinnhubRateLimiter finnhubRateLimiter;
    private final String endpoint;

    public FinnhubMarketDataClient(FinnhubMarketResponseMapper mapper, HttpRequestClient httpRequestClient,
                                   FinnhubRateLimiter finnhubRateLimiter, @Value("${finnhub.endpoint}") String endpoint) {
        this.mapper = mapper;
        this.httpRequestClient = httpRequestClient;
        this.finnhubRateLimiter = finnhubRateLimiter;
        this.endpoint = endpoint;
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Retryable(retryFor = {ClientException.class, JsonProcessingException.class}, maxAttempts = REQUEST_MAX_ATTEMPTS)
    public MarketData retrieveMarketData(Symbol symbol) throws ClientException, JsonProcessingException {
        String apiKey = finnhubRateLimiter.acquirePermission();
        logger.info(RETRIEVING_DATA_INFO, MARKET_DATA, symbol);
        String url = UriComponentsBuilder.fromUriString(endpoint + QUOTE)
                .queryParam(SYMBOL, symbol.getName())
                .queryParam("token", apiKey)
                .build()
                .toUriString();

        return mapper.map(httpRequestClient.fetch(url, false), symbol);
    }
}
